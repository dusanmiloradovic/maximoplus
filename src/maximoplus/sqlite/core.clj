(ns maximoplus.sqlite.core
     (:require [next.jdbc :as jdbc]
               [next.jdbc.sql :as sql]
               [maximoplus.logger :as log]
               [clojure.java.io :as io]
               [clojure.set :as s :refer [rename-keys difference]]
               [maximoplus.maxinterop.core :as m]
               [clojure.data.json :as json]
               [clojure.string :as str :refer [join]]))

(defn create-meta-tables
  [connection structure]
  (doseq [s structure]
;;    (log/error "! "s)
    (jdbc/execute! connection [s])))



(defn create-common-tables
  [connection]
  (jdbc/execute! connection ["create table objectMeta(objectName text,JSON_STORE text)"])
  (jdbc/execute! connection ["create table objectQbe(objectName text,JSON_STORE text)"])
  )

(defn create-table-from-maximo-data
  [table-name maximo-data connection]
  (when (empty?
         (jdbc/execute! connection ["select name from sqlite_master where type='table' and name=?" table-name ]))
    (let [[_ first-row] maximo-data 
          _ (println maximo-data)
          columns (keys
                   (dissoc
                    first-row
                    "_uniqueid"
                    "_selected"
                    :lastRow
                    :new
                    :deleted
                    :readonly
                    ))
          full-cols (concat
                     [["uniqueid" "integer primary key"]
                      ["_SELECTED" "text"]
                      ["parentid" "integer"]
                      ["readonly" "integer"]
                      ["changed" "integer"]
                      ["changedValue" "text"]
                      ["rownum" "integer"]
                      ["tabsess" "text"]]
                     (map (fn [c] [(.toUpperCase c) "text"]) columns))
          _ (println full-cols)
          columns-string (str "("
                              (join ","
                                    (map (fn [[col datatype]]
                                           (str col " " datatype ))
                                         full-cols))
                              ")")
          ]
      (jdbc/execute! connection [(str "create table " table-name columns-string)])
      (jdbc/execute! connection [(str "create table " table-name "_flags" columns-string)])
      )))

(defn get-data-and-flags
  [m]
  [(reduce-kv
    (fn [m k v]
      (assoc m (keyword k)
             (if (sequential? v)
               (first v)
               v) ))
    {} m)
   (reduce-kv
    (fn [m k v]
      (assoc m (keyword k)
             (if (sequential? v)
               (let [flag-v (second v)]
                 (if (= flag-v 7)
                   "[\"true\",\"false\"]"
                   (if (= flag-v 8)
                     "[\"false\",\"true\"]"
                     "[\"false\",\"false\"]")))
               v)))
    {} m)]
  )

(defn get-full-mboset-data
  [ui mbo-set columns parent-id]
  (let [mbo-set-length (.count mbo-set)]
    (loop [rez [] counter 0]
      (if (= counter mbo-set-length)
        (let [full-data
              (map-indexed
               (fn [i d]
                 (get-data-and-flags
                  (assoc d "rownum" i "parentid" parent-id)))
               (map (fn [[rownum d]]
                      (assoc
                       (rename-keys (dissoc d
                                            :new :deleted :lastRow)
                                    {"_uniqueid" "uniqueid" "_selected" "_SELECTED"})
                       "rownum" rownum))
                    rez))]
          [(map first full-data)
           (map second full-data)])
        (recur (conj
                rez
                (m/fetch-mbovalues-for-mbo ui mbo-set columns counter))
               (inc counter))))))

(defn get-app-mboset
  [ui mboname application qbe]
  (let [mbo-set  (.getMboSet ^psdi.server.MXServer @m/server mboname ui)]
    (when application
      (.setApp mbo-set application)
      (.setQueryBySiteQbe mbo-set))
    (when qbe
      (dotimes [k (keys qbe)]
        (.setQbe mbo-set k (get qbe k))))
    
    mbo-set))

(defn get-rel-mboset
  [ui parent-mbo relation]
  (.getMboSet parent-mbo relation))

(defn insert-into-object-meta!
  [connection object-name meta]
  (sql/insert! connection "objectMeta" {:objectName object-name
                                        :JSON_STORE (json/write-str meta)} ))

(defn get-list-mboset
  [ui ^psdi.mbo.MboRemote parent-mbo column-name]
  (.getList (.getZombie (.getThisMboSet parent-mbo)) column-name)
  )

(defn create-table-from-object
  [ui connection mbo-info is-list? ^psdi.mbo.MboRemote parent-mbo parent-table-name create-table?]
  (let [{mboname :mboname
         columns :columns
         qbe :qbe
         objectMeta :object-meta
         children :children
         application :application
         relation :relation
         lists :lists
         list-column :list-column} mbo-info
        ^psdi.mbo.MboSetRemote mbo-set
        (if-not parent-mbo
          (get-app-mboset ui mboname application qbe)
          (if is-list?
            (get-list-mboset ui parent-mbo list-column)
            (get-rel-mboset ui parent-mbo relation)))
        _ (.reset mbo-set)
        mbo-set-length (.count mbo-set)
        values (m/fetch-mbovalues-for-mbo ui mbo-set columns 0)
        [full-data full-flags] (get-full-mboset-data ui mbo-set columns (if-not parent-mbo -1 (.getUniqueIDValue parent-mbo)))
        key-cols (keys (get full-data 0))
        table-name (if parent-table-name
                     (if is-list?
                       (str "list_" parent-table-name "_" list-column)
                       (str parent-table-name "_" relation))
                       mboname)]
    (when create-table?
      (create-table-from-maximo-data table-name values connection)
      (insert-into-object-meta! connection table-name (cons
                                                       {:attributeName "_SELECTED" :maxType "YORN"}
                                                       (m/get-metadata-no-control columns mbo-set ui))))
    (sql/insert-multi! connection table-name (keys (first full-data)) (map vals full-data))
    (sql/insert-multi! connection (str table-name "_flags") (keys (first full-flags)) (map vals full-flags))
    (doseq [child children]
      (dotimes [i mbo-set-length]
        (let [mbo (.getMbo mbo-set i)]
          (create-table-from-object ui connection child false mbo table-name (= 0 i)))))
    (doseq [list lists]
          (create-table-from-object ui connection list true (.getMbo mbo-set 0) table-name true))))

(declare translate-client-offline-creation-request)

;;TODO generate file in TMP folder
(defn generate-server-file
  [ui mbo-info file-name ];;list is structure of {:column [:view-columns}} - :column is the column on mbo
  (let [connection (jdbc/get-connection {:dbtype "sqlite" :dbname file-name :auto-commit false})
        translated-mbo-info (translate-client-offline-creation-request mbo-info)]
    (.setAutoCommit connection false)
    (create-common-tables connection)
    (create-table-from-object ui connection translated-mbo-info  false nil nil true)
    (.commit connection)))

(defn get-generated-server-file
  [ui input-data file-name]
  (generate-server-file ui input-data file-name)
  (io/input-stream file-name)
 )

(def test-mbo-info-1
  {:mboname "po"
   :columns ["ponum" "description" "status" "shipvia"]
   :application "po"
   })

(def test-mbo-info-2
    {:mboname "po"
   :columns ["ponum" "description" "status" "shipvia"]
     :application "po"
     :children [{:relation "poline"
                 :columns ["itemnum"  "storeloc" "orderqty" "orderunit" "polinenum" "description" "linecost" "loadedcost"]}
                ]
     :lists   [{:list-column "shipvia"
                :columns ["value" "description"]}
               {:list-column "status"
                :columns ["value" "description"]}]
     })

(defn translate-one-line-client-request
  [vline]
  (rename-keys vline
               {:object-name :mboname
                :app-name :application
                :rel-name :relation
                :list-col-name :list-column}))

(defn translate-initial-client-request
  [val]
  (into {}
        (map (fn [[k v]]
               [k (translate-one-line-client-request v)])
             val)))

(defn translate-client-offline-creation-request
  [vl]
  (loop [vl (translate-initial-client-request vl)]
    (let [vvals (vals vl)
          parents (set (filter some? (map :parent-name vvals)))
          all-keys (set (keys vl))
          non-parents (difference all-keys parents)]
      (if (or
           (empty? non-parents)
           (empty? parents))
        (-> vl vals first)
        (recur
         (loop [new-val vl non-parents non-parents]
;           (println "????")
 ;          (println new-val)
           (if (empty? non-parents)
             new-val
             (let [np (first non-parents)
                   npval (get new-val np)
                   is-list? (not= nil (:list-column npval))
                   parent-npval (:parent-name npval)
                   parent-npval-val (update-in (get new-val parent-npval)
                                               [(if is-list? :lists :children)]
                                               conj
                                               npval)]
               (recur
                (dissoc
                 (assoc new-val parent-npval parent-npval-val)
                 np)
                (rest non-parents))))))))))


;;select * from sqlite_master
;;where name not in ('objectMeta','objectQbe','workflowPrefetch','offlineChanges')
;;and type='table'
;;and name not like 'list_%';
;;
;;select * from objectMeta
;;where objectName='po';
;;
;;
;;select * from sqlite_master s -- the query to get the main object
;;where name not in ('objectMeta','objectQbe','workflowPrefetch','offlineChanges')
;;and type='table'
;;and name not like 'list_%'
;;and not exists (select name from sqlite_master ss where s.name!=ss.name
;;and instr(s.name,ss.name)>0);
;;
;;
;;select * from sqlite_master s --the query to get the relation objects
;;where name not in ('objectMeta','objectQbe','workflowPrefetch','offlineChanges')
;;and type='table'
;;and name not like 'list_%'
;;and name not like '%_flags'
;;and exists (select name from sqlite_master ss where s.name!=ss.name
;;and instr(s.name,ss.name)>0);
;;
;;select JSON_STORE from objectMeta where objectName='po'; 
;;
;;
;;select JSON_STORE from objectMeta where objectName='po_line'; 


