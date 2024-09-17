(ns maximoplus.maxinterop.core
  (:use [clojure.repl]
        [clojure.string :only [upper-case]]
        )
  (:require [clojure.set]
            [clojure.data.json :as json]
                                        ;            [clojure.tools.logging :as log]
            [maximoplus.logger :as log]
            [cognitect.transit :as transit]            
            [clojure.core.async :refer [go go-loop <! >! >!! chan buffer timeout alts!]]
            [immutant.web :as web]
            [immutant.web.async :as async]
            [immutant.web.sse :as sse]
            [maximoplus.lic :refer :all])
  
  (:import [psdi.util MXSession]
           [psdi.server MXServer]
                                        ;[psdi.mbo MboValueListener Mbo MboSet]
           [java.io ByteArrayInputStream ByteArrayOutputStream]
           )
  )

(set! *warn-on-reflection* true)

(defn env[]
  (System/getenv))

(declare get-mbo-set-info)
(declare register-the-mboset)
(declare get-ms-info)

(def server (atom nil))

(def security-service (atom nil))

(def properties (atom nil))

(defn get-security-service
  []
  (if @security-service
    @security-service
    (let [^psdi.security.SecurityService sec-service  (.lookup ^psdi.server.MXServer @server "SECURITY")]
      (reset! security-service sec-service))))

(defn host-ip
  []
  (first 
   (apply concat
          (map #(->> % .getInetAddresses enumeration-seq (map (fn[x] (.getHostAddress x))))
               (filter #(not (.isLoopback %)) (enumeration-seq (java.net.NetworkInterface/getNetworkInterfaces)))))))

(defn update-maxsession [^psdi.security.UserInfo ui]
  (let [^psdi.server.MXServer ser @server
        ^psdi.mbo.MboSet session-set (.getMboSet ser "maxsession" ui)
        session-id (.getMaxSessionID ui)
        ^psdi.mbo.Mbo max-sess (.getMboForUniqueId session-set session-id)]
    (.setValue max-sess "clienthost" (host-ip) 11)
    (.setValue max-sess "application" "maximoplus" 11)
    (.save session-set)))

(defn connSess[^MXSession sess connstring username password]
  (doto sess
    (.setUserName username)
    (.setPassword password)
    (.setHost connstring)
    (.connect)))

(def no-cache-header  {"content-type" "text/plain" "Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0" "Pragma", "no-cache" "Expires", "0" , "Last-Modified" "1 January, 1970 00:00:01 GMT", "If-Modified-Since" "1 January, 1970 00:00:01 GMT" })

(defn response-no-cache [x & status]
  (let [_stat (if status (first status) 200)
        ]
    {:status _stat, :headers no-cache-header, :body x }))

(defn get-cors-allowed
  []
  (if-let [c1 (.getProperty @properties "corsAllowed")]
    c1
    (System/getProperty "corsAllowed")))

(defn add-cors-to-response
  [response request]
  (try
    (if-let [csites (get-cors-allowed)]
      (let [allowed-sites ( ->> (.split csites ",")  seq (map #(.trim %)))
            headers (:headers request)
            origin (get headers "origin")]
        (if-let [origin-filtered (if (= "*" csites) origin (->> allowed-sites (filter #(= % origin)) first))]
          (assoc response :headers (assoc (:headers response) "Access-Control-Allow-Origin" origin-filtered  "Access-Control-Allow-Credentials" "true" "Access-Control-Allow-Methods" "GET,PUT,POST,DELETE" "Access-Control-Allow-Headers" "Origin, X-Requested-With, Content-Type, Accept" ))
          response
          ))
      response)
    (catch Exception ex
      (.printStackTrace ex)
      response
      )))

(def session-variables (atom {}))

(defn connect [connstring username password]
  (connSess (MXSession/getNewSession) connstring username password))

(defn dissoc-for-ui
  [ref ui]
  (doseq [k
          (filter (fn[[kf ks]] (= kf ui)) (keys @ref))]
    (swap! ref dissoc k)))


(defn kill-session-data [ui]
  (swap! session-variables dissoc ui)
  )

(declare build-session-variables)

(defn empty-session-variables 
  "everything except the mxsessions"
  [ui]
  (let [sesui (@session-variables ui)
        mxses @(:mxsessions sesui)
        ]
    (swap! session-variables dissoc ui)
    (build-session-variables ui)
    (reset! (:mxsessions (@session-variables ui)) mxses)))

(defn t-write [s]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer s)
    (.toString out)))

(defn json-format [x]
  ;;(json/write-str x)
  (t-write x))

(defn get-exc-details
  [^String exc-message]
  (json-format exc-message))

(declare get-inline-mboset)

(defn inline-col?
  [^java.lang.String col]
  (not= -1 (.indexOf col "."))
  )

(defn filter-nonrels [columns]
  (->> columns
       (filter  #(not (re-matches #"\w+\..+" %)))
       vec))

(defn get-inline-rels [columns]
  (let [filtered-cols (filter #(re-matches #"\w+\..+" %) columns)
        splitted (map #(clojure.string/split % #"\.") filtered-cols)
        vals (map #(vector (->> % butlast (clojure.string/join ".")) (last %)) splitted)
        ]
    (reduce
     (fn [col v]
       (assoc col (first v)
              (conj (col (first v)) (second v))))
     {}
     vals
     )))

(defn get-user-info [^psdi.mbo.MboSetRemote mboset]
  (if (instance? psdi.mbo.FauxMboSet mboset) 
    (-> mboset (.getOwner) (.getThisMboSet) (.getUserInfo) )
    (.getUserInfo mboset))
  )

(declare get-metadata)

(defn is-faux-mbo
  [control ui]
  (let [obj  (get @(:objectstore (@session-variables ui)) control) ]
    (instance? psdi.mbo.FauxMboSet obj)))

(defmacro with-faux-mboset
  [control ui mbo-set-symbol & body]
  `(let [~(vary-meta mbo-set-symbol assoc :tag `psdi.mbo.FauxMboSetRemote) (get @(:objectstore (@session-variables ~ui)) ~control)]
     ~@body))

(defmacro with-faux-mbo
  [control ui mbo-symbol & body]
  (let [ss (gensym)]
    `(let [~(vary-meta  ss assoc :tag `psdi.mbo.FauxMboSet)  (get @(:objectstore (@session-variables ~ui)) ~control)
           ~(vary-meta mbo-symbol assoc :tag `psdi.mbo.Mbo)  (.getMbo ~ss)]
       ~@body)))

(defmacro with-mboset
  [control ui mbo-set-symbol & body]
  `(let [~(vary-meta mbo-set-symbol assoc :tag `psdi.mbo.MboSetRemote) (get @(:objectstore (@session-variables ~ui)) ~control)]
     ~@body))

(defmacro with-mbo
  [control ui mbo-symbol & body]
  (let [ss (gensym)]
    `(let [~(vary-meta  ss assoc :tag `psdi.mbo.MboSet)  (get @(:objectstore (@session-variables ~ui)) ~control)
           ~(vary-meta mbo-symbol assoc :tag `psdi.mbo.Mbo)  (.getMbo ~ss)]
       ~@body)))



(defmacro maxloop [bind-symbol ^psdi.mbo.MboSetRemote mbo-set & bd]
  `(do
     (.moveFirst ~(vary-meta  mbo-set assoc :tag `psdi.mbo.MboSetRemote))
     (loop [~(vary-meta  bind-symbol assoc :tag `psdi.mbo.MboRemote) (.getMbo ~mbo-set)]
       (when ~bind-symbol
         ~@bd
         (recur (.moveNext ~mbo-set))))))

;the difference is: we will not destroy the current position of the mboset, instead looping with the counter
(defmacro maxloop-over [bind-symbol ^psdi.mbo.MboSetRemote mbo-set & bd]
  `(loop  [i# 0]
     (let [~(vary-meta  bind-symbol assoc :tag `psdi.mbo.MboRemote) (.getMbo ~mbo-set i#)]
       (when ~bind-symbol
         ~@bd
         (recur (inc i#))))))

(defn mboset->seq_ [^psdi.mbo.MboSetRemote mbo-set cnt]
  (let [mbo (.getMbo mbo-set)]
    (if(= (.getCurrentPosition mbo-set) (- cnt 1))
      (if mbo [mbo] nil)
      (do
        (.moveNext mbo-set)
        (if mbo
          (cons mbo (lazy-seq (mboset->seq_ mbo-set cnt)))
          (lazy-seq (mboset->seq_ mbo-set cnt)))))))

(defn mboset->seq  [^psdi.mbo.MboSetRemote mbo-set]
  (.moveFirst mbo-set)
  (mboset->seq_ mbo-set (.count mbo-set)))

(defn set-current-app [^java.lang.String control-name ^java.lang.String app  ^psdi.security.UserInfo ui]
                                        ;ovo ce da se zove samo za glavni mboset
  (with-mboset control-name ui mboset
    (if mboset
      (let [current-app (:currentapp (@session-variables ui))]
        (swap! current-app assoc control-name app)
;         (set-main-object control-name app ui)

        (.setApp mboset app)
        (.setQueryBySiteQbe mboset)
         )
      (log/error  "no mboset for  " control-name ".. while setting the current app")
      )))

(defn set-unique-app  [^java.lang.String control-name ^java.lang.String app  ^java.lang.String uniqueId ^psdi.security.UserInfo ui]
  (with-mboset control-name ui mboset
    (locking mboset
      (let [current-app (:currentapp (@session-variables ui))]
        (swap! current-app assoc control-name app)
                                        ;      (set-main-object control-name app ui)

        (.setApp mboset app))
      ( .getMboForUniqueId mboset (java.lang.Long/parseLong uniqueId)))))

(defn set-unique-id   [^java.lang.String control-name ^java.lang.String uniqueId ^psdi.security.UserInfo ui]
  (with-mboset control-name ui mboset
    (locking mboset
      ( .getMboForUniqueId mboset (java.lang.Long/parseLong uniqueId)))))

(defn get-current-app [control-name ui]
  (let [current-app (:currentapp (@session-variables ui))]
    (@current-app control-name)))


(def option-synonyms
  {"UNDELETE" "DELETE"})
           ;one option synonym for more methods, so 

(defn send-to-immutant-chan;this version uses the immutant channel
  [batch ui & immediate?]
  (log/debug "trying to send to immutant 1")
  (when-let [_imc (:immutant-channel (@session-variables ui))]
    (log/debug "trying to send to immutant 2")
    (let [imc @_imc]
      (when-not (and immediate? (not (realized? imc)))
        (log/debug "trying to send to immutant 3")
        (let [im-chan @imc]
          (when (and
                 (not= im-chan :kill)
                 (async/open? im-chan)) ;;channel was never delivered, the kill is done from the logout thread
            (log/debug "send data to immutant channel " imc)
            (if @(:sse-session (@session-variables ui))
              (sse/send! im-chan  (json-format batch))
              (async/send! im-chan  (json-format batch) :close? true
                           :on-success (fn [] (log/debug "async snd success"))
                           :on-error (fn [err] (log/debug "async send error" err))))))))))


(defn build-session-variables
  [^psdi.security.UserInfo ui]
  (let [^psdi.security.SecurityService sec-service (get-security-service)
        ^psdi.security.Profile profile (.getProfile sec-service ui)
        message-queue (chan 1e5) ;TODO make this configurable
        ses-var {:objectstore (atom {})
                 :currentapp (atom {})
                 :mbostore (atom {})
                 :control-columns (atom {})
                 :inline-rels (atom {})
                 :mxsessions (atom {})
                 :main-object (atom {})
                 :wfdirectors (atom {})
                 :glformats (atom {})
                 :pending-offline-data (atom {})
                 :lock (Object.)
                 :message-queue message-queue 
                 :immutant-channel (atom (promise))
                 :sse-session (atom false)
                 :profile profile
                 }]
    (swap! session-variables assoc ui ses-var)
    (go-loop []
      (when-let [v (<! message-queue)]
        (loop [batch [v] timeout-ch (timeout 15)]
          (let [[v ch] (alts! [message-queue timeout-ch])]
            (if (= message-queue ch)
              (recur (conj batch v) timeout-ch)
              (send-to-immutant-chan batch ui))))
        (recur)))))



(defn to-sql-list [sekv]
  (str "("
       (loop [_str "" sekv sekv]
         (let [f (first sekv)
               r (rest sekv)]
           (if (empty? r)
             (str _str "'" f "'")
             (recur (str _str "'" f "',") (rest sekv)))))
       ")"))

(defn get-option-descriptions-full [control ui]
  (with-mboset control ui mboset
    (locking mboset
      (let [curr-app (get-current-app control ui)
            mbo (.getZombie mboset)]
        (when curr-app
          (let [^psdi.security.Profile profile (:profile (@session-variables ui))
                ^java.util.HashSet app-options (.getAppOptions profile curr-app)
                optseq (->  (.iterator app-options) iterator-seq to-sql-list)
                ^psdi.mbo.MboSetRemote sigset (.getMboSet mbo "$so$12" "sigoption" (str "app='" curr-app "' and optionname in " optseq))]
            (.reset sigset)
            (.moveFirst sigset)
            [curr-app 
             (reduce
              (fn[x y] (assoc x (first y) (second y)))
              {}
              (loop [rez [] cnt 0]
                (let [mbo (.getMbo sigset cnt)]
                  (if-not mbo
                    rez
                    (let [a [(.getString mbo "OPTIONNAME") (.getString mbo "DESCRIPTION")]]
                      (recur (conj rez a) (inc cnt)))))))]))))))

(defn remove-underscore-and-after
  [txt]
  (if-let [m (re-matches #"(.*)(_.*)" txt)]
    (second m)
    txt))

(defn get-full-object-path 
  "This is for the signature security, for the main object it returns the name of the object, for the objects got by relationship it is parent#child1#child2... ,where child1 is child of a parent, child2 is a child of a child1"
  [control ui]
  (with-mboset control ui mboset
    (loop [ms mboset name ""]
      (if (.isMainObject (.getMboSetInfo ms))
        (let [main-obj-name (-> (@session-variables ui) :mbostore deref (get ms) first)]
          (str  (get-current-app main-obj-name ui)  name))
        (recur (.getThisMboSet (.getOwner ms)) (str "#" (.. ms getMboSetInfo getObjectName) name))))))


(defn get-options
  [control ui]
  (let [od (get-option-descriptions-full control ui)]
    (map (fn [[k ^java.lang.String v]] 
           [(if (.startsWith v "#") (str (first od) v) (first od)) 
            (remove-underscore-and-after k)])
         (second od))))

(defn has-access?
  [control ^java.lang.String option ui]
  (log/debug "Checking access for option " option " and control " control " and ui " ui)
  (let [op (get-full-object-path control ui)]
    (log/debug "OP:" op)
    (log/debug  "Options: " (get-options control ui))
    (some #(= % [op (.toUpperCase option)]) 
          (get-options control ui)
          ))
  )



(defn bypass-option-check?
  [control ^java.lang.String option ui]
  ;;to simplify graphql processing, remove the check on save and rollback for the time being
  (if (or
       (.equalsIgnoreCase option "save")
       (.equalsIgnoreCase option "rollback"))
    true
    (with-mboset control ui mboset
      (log/debug  "calling  bypass access check")
      (when-let [virtual? (-> (get-mbo-set-info mboset) :persistent not)]
        (log/debug "virtual mbo, option-name:" option)
        (.equalsIgnoreCase option "setup")))))

(defn access-to-option [control option ui]
  (with-mboset control ui mboset
    (when-not (or (bypass-option-check? control option ui) (has-access? control option ui)) 
      (log/error "Acess denied for '" control "' and '" option "'")
      (throw (psdi.util.MXSystemException. "system" "systemerror" (Throwable. "Access denied"))))))

;arg-control-name is the optional mboset control passed to the method
(defn run-mbo-command [control-name ^java.lang.String _command _arg-control-name ^psdi.security.UserInfo ui]
  (log/debug "run-mbo-command " control-name " command '" _command "' arg-control-name '" _arg-control-name "'" (= "null" _arg-control-name))
  (with-mbo control-name ui mbo
    (log/debug "control-name mbo" mbo)
    (let [command (.trim _command)
          arg-control-name (when _arg-control-name (.trim _arg-control-name))
          ]
      (access-to-option control-name (.toUpperCase command) ui)
      (if (or (nil? arg-control-name) (= "null" arg-control-name))
        (clojure.lang.Reflector/invokeNoArgInstanceMember mbo command)
        (with-mboset arg-control-name ui arg-mboset
          (clojure.lang.Reflector/invokeInstanceMethod mbo command (to-array [arg-mboset]))
          )))))

(declare save)

(defn run-mboset-command [control-name ^java.lang.String _command _arg-control-name ^psdi.security.UserInfo ui]
  (with-mboset control-name ui mbo-set
    (let [command (.trim _command)
          arg-control-name (when _arg-control-name (.trim _arg-control-name))
          ]
      (access-to-option control-name (.toUpperCase command) ui)
      (if (= (.toUpperCase command) "SAVE")
        (save control-name ui)
        (if (or (nil? arg-control-name) (= "null" arg-control-name))
          (clojure.lang.Reflector/invokeNoArgInstanceMember mbo-set command)
          (with-mboset arg-control-name ui arg-mboset
            (clojure.lang.Reflector/invokeInstanceMethod mbo-set command (to-array [arg-mboset])))
          )))))

(defn register-mbo-command  [control-name parent-control ^java.lang.String command arg-control-name ^psdi.security.UserInfo ui]
  (if-let [new-mboset (run-mbo-command parent-control command arg-control-name ui)]
    (register-the-mboset new-mboset control-name)
    "empty"))

(defn register-mboset-command  [control-name parent-control ^java.lang.String command arg-control-name ^psdi.security.UserInfo ui]
  (if-let [new-mboset (run-mboset-command parent-control command arg-control-name ui)]
    (register-the-mboset new-mboset control-name)
    "empty"))

(defn start-mx-server [^java.util.Properties maxprop]
  (.setProperty maxprop "mxe.crontask.donotrun" "ALL")
  (reset! properties maxprop)
  (swap! server
         (fn[x]
           (do
             (MXServer/start maxprop)
             (MXServer/getMXServer)))))

(defn get-properties-from-path [version prop-path]
  (log/debug "version:" version)
  (if (= 6 version)
    (doto
        (java.util.Properties.)
      (.load (java.io.FileInputStream. prop-path)))
    (let [^java.io.FileInputStream fis (java.io.FileInputStream. prop-path)]
      (psdi.util.MXProperties/loadProperties fis true))))

(defn get-properties-from-classloader [version]
  (if (= 7 version)
    (psdi.util.MXProperties/loadProperties  (.openStream (ClassLoader/getSystemResource "maximo.properties")) true)
    (doto (java.util.Properties.) (.load (.openStream (ClassLoader/getSystemResource "maximo.properties")))))
  )

(def license-check-time (atom nil))

(defn check-number-of-running-servers
  []
  (when (or ;;expensive operation, do it every hour
         (= @license-check-time nil)
         (>
          (- (System/currentTimeMillis) @license-check-time)
          3600000
          ))
    (log/debug "Checking the number of running servers")
    (reset! license-check-time (System/currentTimeMillis))
    (let [_lic-servers (:number-of-servers (get-lic-keys))
          licensed-servers (if (integer? _lic-servers)
                             _lic-servers
                             (Integer/parseInt _lic-servers))
          ^psdi.mbo.MboSetRemote sess (.getMboSet @server "maxsession" (.getSystemUserInfo @server))]
      (log/debug "Number of licensed servers" licensed-servers)
      (.setWhere sess "application='maximoplus' ")
      (.reset sess)
      (when (< licensed-servers
               (->> 
                (loop [server-map {} counter 0]
                  (let [^psdi.mbo.MboRemote mbo (.getMbo sess counter)]
                    (if-not mbo
                      server-map
                      (let [key (str (.getString mbo "serverhost") "_" (.getString mbo "servername"))
                            val (get server-map key )]
                        (log/debug key "---->" val)
                        (recur (assoc server-map key (if val (inc val) 0)) (inc counter))))))
                keys
                count))
        (.println System/err  "Maximum number of licenced servers exceeded")
        ;;delete the existing sessions so the other server can keep on running
        (let [shost (.getServerHost @server)
              sname (.getName @server)]
          (.setWhere sess (str  "servername='" sname "' and serverhost='" shost "'"))
          (.reset sess)
          (.deleteAll sess)
          (.save sess))
        (System/exit 0)))))


(defn start-server [version & prop-path]
  (let [maximo-properties (if prop-path 
                            (get-properties-from-path version (first prop-path))
                            (get-properties-from-classloader version)
                            )]
    (check-lic-date)
    (start-mx-server maximo-properties)
))


                                        ;ovo nije login, login module treba odvojeno da se napravi, i tada da zove ovo. napravicu nekoliko login funkcija, ali treba nazalost i genericki login

(defn get-mx-session[^psdi.security.UserInfo ui]
  (proxy [psdi.util.RMISession] []
    (loadUserInfo [] ui)
    (getMXServer [] @server)
    
    ))

(defn  create-user-info[username & password]
;password is for a Maximo-based login
  (log/debug "create-user-info for " username)
  (let [
        ^psdi.security.SecurityService sec-service (get-security-service)
        ui (if password 
             (.authenticateUser sec-service username (first password) "minimo")
             (.authenticateUser
              sec-service
              username))
        ^psdi.util.MXSession sess (get-mx-session ui)]
    (.connect sess)
    (build-session-variables ui)
    (reset! (:mxsessions (@session-variables ui)) sess)
    (update-maxsession ui)
    ui))

(defn add-control-columns [control ui cols]
  (log/debug "add-control-columns " control ".." cols " at " (System/currentTimeMillis))
  (when (@(:objectstore (@session-variables ui)) control)
    (log/debug "!!add-control-columns " control ".." cols " at " (System/currentTimeMillis))
    (let [control-columns (:control-columns (@session-variables ui))
          old-cols (@control-columns control)]
      (swap! control-columns assoc control (clojure.set/union (set  (map upper-case cols))  (@control-columns control)))
      (let [mda (get-metadata control ui)
            attrs (map :attributeName mda)]
        (doseq [col (map upper-case cols)]
          (when-not  (some #(= % col) attrs )
            (swap! control-columns assoc control old-cols)
            (do
              (log/error (str "####Metadata for control " control "...." mda  "error while adding control columns:" (System/currentTimeMillis) ".. " (@(:objectstore (@session-variables ui)) control)) )
              (throw (psdi.util.MXSystemException. "system" "invalidcolumn" (Throwable. (str "Invalid column " col)))))))
        (log/debug (@control-columns control))))))

(defn remove-control-columns [control ui cols]
  (log/debug "Remove control columns  " cols " " (type cols) 
           )
  (let [control-columns (:control-columns (@session-variables ui))]
    (swap! control-columns assoc control
           (clojure.set/difference (set (@control-columns control)) 
                                   (set  (map upper-case cols))))))


(defn register-the-mboset [^psdi.mbo.MboSetRemote mboset control-name &  skip-setup]
  (locking mboset
    (log/debug "Registering the " control-name " at " (System/currentTimeMillis))
    (let [ui (get-user-info mboset)
          control-for-user (:objectstore (@session-variables ui))
          mbos-for-user (:mbostore (@session-variables ui))
          not-init-np (first skip-setup)
          ^psdi.mbo.MboSetInfo ms-info (get-ms-info mboset)
          ]
      (if (or (instance? psdi.mbo.FauxMboSet mboset) (-> ms-info (.isPersistent)))
        (log/debug "registering the persistent mboset " mboset)
        (when (instance? psdi.mbo.NonPersistentMboSet mboset)
          (log/debug "Setting up the non persistent mboset")
          (when-not not-init-np
            (.setup mboset) 
            (.moveFirst mboset))))
      [(do
         (log/debug "registering the "  control-name " the normal way and mboset " mboset)
         (when-let [bound-mboset (get @control-for-user control-name)]
           (log/debug "got the bound mboset")
           (swap! mbos-for-user dissoc bound-mboset))
         (swap! control-for-user assoc control-name mboset )
         (swap! mbos-for-user assoc mboset (if-let [bound (@mbos-for-user mboset)]
                                             (conj bound control-name)
                                             [control-name]))
         (log/debug "controls for user " control-for-user)

         "none")
       (-> mboset get-mbo-set-info first :objectName)
       ])))

(defn unregister-the-mboset [control-name ^psdi.security.UserInfo ui]
  (log/debug "Unregistering the " control-name " u " (System/currentTimeMillis))
  (let [control-for-user (:objectstore (@session-variables ui))
        mbos-for-user (:mbostore (@session-variables ui))
        bound-mbo-set (@control-for-user control-name)
        ]
    (when bound-mbo-set
      (swap! mbos-for-user dissoc bound-mbo-set)
      (swap! control-for-user dissoc control-name)))
  (log/debug "Unregistering se zavrsio za " control-name " u " (System/currentTimeMillis) (:objectstore (@session-variables ui)))
   )



(defn register-main-mboset [^java.lang.String name control-name ^psdi.security.UserInfo ui]
  (-> (.getMboSet ^psdi.server.MXServer @server name ui) (register-the-mboset control-name))
  )

(defn get-menu-apps [^psdi.security.UserInfo ui]
  (let [apps (.getMboSet ^psdi.server.MXServer @server "maxapps" ui)
        profile (.getProfile apps)
        maxmenu (.getMboSet ^psdi.server.MXServer @server "maxmenu" ui)]
    (.setWhere maxmenu (str "menutype='MODULE' and (elementtype!='APP' or (elementtype='APP' and keyvalue in "
                            (to-sql-list
                             (filter #(-> % nil? not)
                                     (map (fn[mbo] 
                                            (let [app (.getString mbo "app")
                                                  opts (set (.getAppOptions profile app))]
                                              (when (opts "READ") app)))
                                          (mboset->seq apps))))
                            "))"))
    (.setOrderBy maxmenu "position, subposition")
    maxmenu
    ))

(defn register-maximo-menu [control-name ^psdi.security.UserInfo ui]
  (register-the-mboset (get-menu-apps ui) control-name)
  )

(defn register-mboset-byrel [control-name ^java.lang.String rel-name parent-control ui]
  (log/debug "Registering relationship " control-name ".." rel-name "<-" parent-control)
  (when-let [parent-set ^psdi.mbo.MboSetRemote (get @(:objectstore (@session-variables ui)) parent-control)]
    (let [^psdi.mbo.MboRemote curr-mbo (if-let [mbocurr (.getMbo parent-set)] mbocurr (.getZombie parent-set))
          ms-info (get-ms-info parent-set)
          pers? (.isPersistent ms-info)]
      (when curr-mbo
        (let [current-app (:currentapp (@session-variables ui))]
          (swap! current-app assoc control-name (@current-app parent-control)))
        (let [^psdi.mbo.MboSetRemote rel-mbo-set (.getMboSet curr-mbo rel-name)]
          (locking rel-mbo-set
            (log/debug "register mboset-by-rel, first record")
            (let [rez (register-the-mboset rel-mbo-set  control-name)]
              (try
                (when (.getMbo rel-mbo-set 0)
                                        ;          (log/debug "ppomeram")
                  (.moveFirst rel-mbo-set))
                (catch Exception e (log/error "registrer-mboset-byrel internal" e)))
              (conj  rez (if pers? (.getUniqueIDValue curr-mbo) "np")))))))))

(defn re-register-mboset-byrel [control-name ^java.lang.String rel-name parent-control ui]
  (unregister-the-mboset control-name ui)
  (register-mboset-byrel control-name rel-name parent-control ui))

(defn reg-one-helper [control-name parent-control-name ^psdi.mbo.Mbo parent-mbo ui]
  (let [mbo-name (.getName parent-mbo)
        mbo-id (.getUniqueIDValue parent-mbo)
        mboset (.getMboSet ^psdi.server.MXServer @server mbo-name ui)
        mainapp (get-current-app parent-control-name ui)
        ret (register-the-mboset mboset control-name)]
    (.getMboForUniqueId mboset mbo-id)
    (when mainapp
      (log/debug "reg-one helper, setting the current app for " control-name ".. " mainapp ".." ui)
      (set-current-app control-name mainapp ui))
    (let [^psdi.mbo.MboSet parent-set (.getThisMboSet parent-mbo)
          ^psdi.mbo.Mbo parent-owner (.getOwner parent-set)]
      (when parent-owner
        ;;this is used to preserve the hierarchy, so the security check will work for the new container
        (.setOwner mboset parent-owner)))
    (conj ret (.getUniqueIDValue parent-mbo))))

(defn register-empty-mboset [control-name ^psdi.mbo.MboSet parent-mboset ui]
                                        ;the point is that we need an empty set in special case to initialize the empty container on the client side
  (let [mbo-name (.getName parent-mboset)
        mboset (.getMboSet ^psdi.server.MXServer @server mbo-name ui)
        ret (register-the-mboset mboset control-name ui)]
    (.setWhere mboset "1=2")
    ret))

(defn register-mboset-with-one-mbo-ind [control-name parent-control-name parent-index ui]
  (let [parent-set ^psdi.mbo.MboSetRemote (@(:objectstore (@session-variables ui)) parent-control-name)]
    (when-let [parent-mbo (.getMbo parent-set parent-index)]
      (reg-one-helper control-name parent-control-name parent-mbo ui))))


(defn register-mboset-with-one-mbo [control-name parent-control-name parent-unique-id ui]
  (let [parent-set ^psdi.mbo.MboSetRemote (@(:objectstore (@session-variables ui)) parent-control-name)
        ms-info (get-ms-info parent-set)
        persistent? (.isPersistent ms-info)]
    (if parent-unique-id
      (reg-one-helper control-name parent-control-name (.getMboForUniqueId parent-set parent-unique-id) ui)
      (if-let [parent-mbo (.getMbo parent-set)]
        (if persistent?;;for non-persistent, register the name to existing mboset
          (reg-one-helper control-name parent-control-name parent-mbo ui)
          (register-the-mboset parent-set control-name)
          )
        (register-empty-mboset control-name parent-set ui)
        ))))

(defn re-register-mboset-with-one-mbo [control-name parent-control-name unique-id  ui]
  (let [control-set ^psdi.mbo.MboSetRemote (@(:objectstore (@session-variables ui)) control-name)
        ^psdi.mbo.Mbo control-mbo (.getMbo control-set)]
    (unregister-the-mboset control-name ui)
    (register-mboset-with-one-mbo control-name parent-control-name unique-id  ui)))

(defn register-mboset-byrel-inline [rel-name parent-control ui & rownum]
  (log/debug "Register mboset by inline relation:::" rel-name ":" parent-control ";" rownum)
  (with-mboset parent-control ui ms
    (let [rn (when rownum (first rownum))
          irui (:inline-rels (@session-variables ui))
          ]
      (loop [rels (clojure.string/split rel-name #"\.")
             control-name parent-control
             ms ms
             ]
        (let [fr (first rels)
              cn (str control-name "." fr )
              mbo (if-let [mbocurr (if rn (.getMbo ms rn)(.getMbo ms))] mbocurr (.getZombie ms))
              ]
          (if (empty? (rest rels))
            (let [
                  irs-for-control (if-let [_irsc (@irui parent-control)] _irsc #{})]
              (swap! irui assoc  control-name (conj irs-for-control rel-name))
              (register-the-mboset (.getMboSet mbo fr) (str cn (when rn (str "#" (first rn))))))
            (recur (rest rels) cn (.getMboSet mbo fr))
            ))))))

(defn get-inline-mboset [rel-name ^psdi.mbo.MboRemote mbo]
  (log/debug "getting the inline mboset for " rel-name " and mbo " mbo)
  (when mbo
    (let [ms ^psdi.mbo.MboSetRemote (.getThisMboSet mbo)
          ]
      (loop [rels (clojure.string/split rel-name #"\.")
             ms ms
             mbo mbo
             ]
        (let [fr (first rels)]
          (if (empty? (rest rels))
            (.getMboSet mbo fr)
            (let [mset  (.getMboSet mbo fr)
                  _mr  (if (.isEmpty mset) (.getZombie mset) (.getMbo mset 0))]
              (recur (rest rels) mset _mr))))))))

(defn register-list
  [list-control-name control-name column-name force-qbe? ^psdi.security.UserInfo ui]
  (let [^psdi.mbo.MboSetRemote mbo-set (@(:objectstore (@session-variables ui)) control-name)
        ^psdi.mbo.MboRemote mbo (.getMbo mbo-set)
        ]
    (log/debug "Calling registration  for list " list-control-name " control-name " control-name " mbo : " mbo "mbo-set :" mbo-set " and force-qbe? " force-qbe?)
    (if-let [lista
             (if force-qbe?
               (let [lst (.getList (.getZombie mbo-set) column-name)]
                 (.unselectAll lst)
                 lst)
               (if mbo
                 (.getList mbo column-name) (.getList mbo-set column-name)))]
      (register-the-mboset lista list-control-name)
      (log/error "Failed to register the list")
      )))

(defn get-key-attributes
  [control-name ^psdi.security.UserInfo ui]
  (with-mboset control-name ui mboset
    (let [^psdi.mbo.MboSetInfo mboset-info (.getMboSetInfo mboset)]
      (seq (.getKeyAttributes mboset-info)))))

(defn register-qbe-list
  [list-control-name control-name column-name ^psdi.security.UserInfo ui]
  (let [^psdi.mbo.MboSetRemote mbo-set (@(:objectstore (@session-variables ui)) control-name)
        row (int -1)
        ]
    (when-let [lista (.getList mbo-set row column-name)]
      (.unselectAll lista)
      (register-the-mboset lista list-control-name))))

(defn set-value-from-list
  [control-name list-control-name ^java.lang.String column-name ^psdi.security.UserInfo ui]
  (if (is-faux-mbo control-name ui)
    (with-faux-mbo control-name ui mbo
      (with-mboset list-control-name ui list-set
        (.setValue mbo column-name list-set)
        (log/debug "Done set-value-from-list and the value is " (.getString mbo column-name))
        (.getString mbo column-name)))
    (with-mbo control-name ui mbo
      (log/debug "column " column-name)
      (log/debug mbo)
      (with-mboset list-control-name ui list-set
        (.setValue mbo column-name list-set)
        (log/debug "Done set-value-from-list and the value is " (.getString mbo column-name))
        (.getString mbo column-name)))))



(defn smart-fill
  [command-control-name control-name attribute ^java.lang.String value ^psdi.security.UserInfo ui]
  (with-mbo control-name ui mbo
    (when mbo
      (when-let [command-set (.smartFill mbo attribute value false)]
        (register-the-mboset command-set command-control-name)))))

(defn set-qbe [control-name ^java.lang.String qbe-attr ^java.lang.String qbe-expr ^psdi.security.UserInfo ui]
  (with-mboset control-name ui control-ms
    (when control-ms
      (.setQbe control-ms qbe-attr qbe-expr))))

(defn set-qbe-from-list
  [control-name list-control-name ^java.lang.String column-name  ^psdi.security.UserInfo ui]
  (with-mboset control-name ui mbo-set
    (with-mboset list-control-name ui list-set
      ;;in Maximo if I send the empty list it will not reset the qbe, here we need to clear qbe in that case
      (if (empty? (.getSelection list-set))
        (.setQbe mbo-set column-name "")
        (.setQbe mbo-set column-name list-set)))))

(defn initiate-hier [control-name mboname attribute value  ^psdi.security.UserInfo ui]
  (when-let [^psdi.mbo.MboSetRemote origmboset ( .getMboSet ^psdi.server.MXServer @server mboname ui)]
    (.setWhere origmboset (str attribute "='" value "'"))
    (when-let [mbo (.getMbo origmboset 0)]
      (let [hier-mboset ( .getMboSet ^psdi.server.MXServer @server mboname ui) ]
        (.setWhere hier-mboset "1=2")
        (.blindCopy mbo hier-mboset)
        (register-the-mboset hier-mboset control-name)))))

(defn add-hier-children [control-name k v ^psdi.security.UserInfo ui]
  (with-mboset control-name ui mboset
    (log/debug "add-hier-children"  control-name ":" mboset ":" k ":" v)
    (let [control-columns (:control-columns (@session-variables ui))
          child-seq (mboset->seq 
                     (.getChildren
                      (->>
                       (mboset->seq mboset)
                       (filter (fn [^psdi.mbo.Mbo x] (= v (.getString x k))))
                       first)))
          concols (@control-columns control-name)
          ]
      (doseq [^psdi.mbo.MboRemote c child-seq]
        (.blindCopy c mboset))
      (for [^psdi.mbo.MboRemote y child-seq] {(.getString y k) (reduce merge (for[x concols] {x (.getString y x)}))}))))

(defn filter-by-mboset [^psdi.mbo.MboSetRemote mbo-set]
  (let [ui (get-user-info mbo-set)]
    (when-let [mbosetovi (:mbostore (@session-variables ui))]
      (@mbosetovi mbo-set))))


(defn info-static [^psdi.mbo.MboSetRemote ms ^java.lang.String attr-name]
  (.getMboValueInfoStatic ms attr-name))

(defn remarks [ms attr-name]
  (let [is  ^psdi.mbo.MboValueInfoStatic  (info-static ms attr-name)]
    (.getRemarks is)))

(defn get-ms-info
  [^psdi.mbo.MboSetRemote ms]
    (.getMboSetInfo ms))
  
(defn get-mbo-set-info
  ([^psdi.mbo.MboSetRemote ms]
   (let [ms-info (.getMboSetInfo ms)
           st  (fn [^psdi.mbo.MboValueInfo x] (.getMboValueInfoStatic ms (.getAttributeName x)))]
       (cons
        {:mboPersistent (.isPersistent ms-info) :objectName (.getObjectName ms-info) :app (.getApp ms)}
        (map
         (fn [^psdi.mbo.MboValueInfo x]
           (let [mis ^psdi.mbo.MboValueInfoStatic (st x)]
             {:attributeName (.getAttributeName x)
              :objectName (.getObjectName x)
              :maxType (.getMaxType x)
              :length (when mis (.getLength mis))
              :title (when mis (.getTitle mis))
              :remarks (when mis(.getRemarks mis))
              :defaultValue (.getDefaultValue x)
              :domainId (.getDomainId x)
              :isALNDomain (when-let [dif (.getDomainInfo x)]  (instance?  psdi.mbo.ALNDomainInfo dif ))
              :persistent (.isPersistent x)
              :required (.isRequired x)
              :scale (.getScale x)
              :hasLD (.hasLongDescription x)
              :numeric (.isNumeric x)
              }
             ))
         (iterator-seq (.getAttributes ms-info)))
        )
       ))
  ([control ui]
     "gives the map of the mboset attributes:objectname, attributename, remarks, title, size. Later on I may add mre attributes, but the goal now is to keep the system simple"
     (when-let [ms ^psdi.mbo.MboSetRemote (@(:objectstore (@session-variables ui)) control)]
       (get-mbo-set-info ms))))

                                        ;(defn send-metadata
                                        ;  "this one is going to pour the metadata of the control straight to aleph."
                                        ;  [control ui]
                                        ;  (when-let [cm (@client-message-queues ui)]
                                        ;    (enqueue cm [:metadata control (pr-str (get-mbo-set-info control ui))])))


(defn get-metadata-no-control [_columns mbo-set ui]
                                        ;ovo ce biti isto kao i get-mbo-set-info samo sto ce da 1. vrati samo za attribute koji su registrovani 2. automatski pronadje metapodatke za sve inline relacije i stavi ih kao direktne metapodatke u glavni mbo
  (let [columns (map (fn[c](.toUpperCase c)) _columns)]
    (concat
     (->> (get-mbo-set-info mbo-set)
          (filter
           #(or
             (not (find % :attributeName ))
             (some (fn [x] (= (:attributeName %) x) )
                   (-> columns filter-nonrels))) ))
     (let [inlines (get-inline-rels  columns)]
       (let  [ ^psdi.mbo.MboRemote main-zombie (.getZombie mbo-set)]
         (apply
          concat
          (for [rel (keys inlines)]
            (let [rel-columns (inlines rel)
                  inline-mboset (get-inline-mboset rel main-zombie)
                  ]
              (->> (get-mbo-set-info inline-mboset)
                   (filter
                    #(some (fn [x] (= (:attributeName %) x) )
                           rel-columns) )
                   (map #(assoc % :attributeName (str rel "." (:attributeName %)))))))))))))

(defn get-metadata [control ui]
                                        ;ovo ce biti isto kao i get-mbo-set-info samo sto ce da 1. vrati samo za attribute koji su registrovani 2. automatski pronadje metapodatke za sve inline relacije i stavi ih kao direktne metapodatke u glavni mbo
  (let [control-columns (:control-columns (@session-variables ui))]
    (concat
     (->> (get-mbo-set-info control ui)
          (filter
           #(or
             (not (find % :attributeName ))
             (some (fn [x] (= (:attributeName %) x) )
                   (->  (@control-columns control) filter-nonrels))) ))
     (let [inlines (get-inline-rels  (@control-columns control))
           ^psdi.mbo.MboSetRemote mboset-main (@(:objectstore (@session-variables ui)) control)
           ]
       (when mboset-main
         (let  [ ^psdi.mbo.MboRemote main-zombie (.getZombie mboset-main)]
           (apply
            concat
            (for [rel (keys inlines)]
              (let [rel-columns (inlines rel)
                    inline-mboset (get-inline-mboset rel main-zombie)
                    ]
                (->> (get-mbo-set-info inline-mboset)
                     (filter
                      #(some (fn [x] (= (:attributeName %) x) )
                             rel-columns) )
                     (map #(assoc % :attributeName (str rel "." (:attributeName %))))))))))))))

(defn get-control-inline-rels [control ui]
  (@(:inline-rels (@session-variables ui)) control)
  )

(defn fetch-mbovalues-for-mbo
  [ui ^psdi.mbo.MboSetRemote mboset   attrs & row]
                                        ;  (locking (:lock (@session-variables ui))) ; prebacio na fetch-mbovalues
  (when mboset
    (let [ms-info (get-ms-info mboset)
          pers? (.isPersistent ms-info)
          rnum (first row)
          rw (if rnum rnum (.getCurrentPosition mboset))
          mboset-size (.getSize mboset)
          ^psdi.mbo.Mbo mbo (when (and mboset rnum) 
                              (.getMbo mboset rnum))
          attrf (fn [^java.lang.String x]
                  (let [^psdi.mbo.MboValue mboval (.getMboValue mbo x)]
                    (.moveFieldFlagsToMboValue mbo mboval)
                    (if (.isReadOnly mboval)
                      7
                      (if (.isRequired mboval) 8 0))))]
      (when mbo
        [(if rnum rnum (.getCurrentPosition mboset))
         (assoc
          (apply hash-map
                 (interleave attrs
                             (map (fn [x] [ (.getString mbo x) (attrf x)]) attrs)))
          "_uniqueid" (if pers?
                        (.getUniqueIDValue mbo)
                        "np")
          "_selected" (if (.isSelected mbo) "Y" "N")
          :lastRow (= rw mboset-size)
          :new (.isNew mbo)
          :deleted (.toBeDeleted mbo)
          :readonly (not= 0 (bit-and  (.getFlags mbo) 7)))
         ]))))

(defn append-rel-to-fethced-inlines [row]
  
  )

(defn fetch-one-mbo-values
  [control mbo ui]
  (let [attrs (@(:control-columns (@session-variables ui)) control)
        pers?   (not (instance? psdi.mbo.NonPersistentMbo mbo))
        attrf (fn [^java.lang.String x]
                (let [^psdi.mbo.MboValue mboval (.getMboValue mbo x)]
                  (.moveFieldFlagsToMboValue mbo mboval)
                  (if (.isReadOnly mboval)
                    7
                    (if (.isRequired mboval) 8 0))))]
    (when mbo
      (assoc
       (apply hash-map
              (interleave attrs
                          (map (fn [x] [ (.getString mbo x) (attrf x)]) attrs)))
       "_uniqueid" (if pers? (.getUniqueIDValue mbo) "np")
       "_selected" (if (.isSelected mbo) "Y" "N")
       :new (.isNew mbo)
       :deleted (.toBeDeleted mbo)
       :readonly (not= 0 (bit-and  (.getFlags mbo) 7))))))

(defn fetch-mbovalues
  [control ui & row]
  (when-let [^psdi.mbo.MboSetRemote mboset (@(:objectstore (@session-variables ui)) control)]
    (locking  mboset ;(:lock (@session-variables ui))
      (let [contr-columns (@(:control-columns (@session-variables ui)) control)
            attrs (-> contr-columns set vec filter-nonrels)
            inlines (get-inline-rels contr-columns)
            mbo  (if row (.getMbo mboset (first row)) (.getMbo mboset))
            ]
        (let [[rownum  main-data] (fetch-mbovalues-for-mbo ui mboset  attrs (first row)) ]
          [rownum (reduce
                   merge main-data
                   (apply concat (for [k (keys inlines)]
                                   (let [_cls (inlines k)]
                                     (let [[_ inline-data] (fetch-mbovalues-for-mbo ui (get-inline-mboset k mbo) _cls 0)]
                                       (for [field (keys inline-data) :when (and (not= field "_selected")  (not= field "_uniqueid") (-> field keyword? not)) ]
                                         [(str k "." field) (inline-data field)]))))))])))))

(declare forward)

(defn fetch-multi-rows-old
  [control ui numrows]
  (loop [rows [] nrs numrows]
    (if (= 0 nrs)
      rows
      (do
        (when-not (= numrows nrs) (forward control ui))
        (recur (conj rows (fetch-mbovalues control ui)) (dec nrs)))))
  )

;;it will also include the movement to the first row after the fetch is done, to simplify things for the client side
(defn fetch-multi-rows
  [control ui start-row numrows]
  (with-mboset control ui mboset
    (log/debug "fetch-multi-rows for the  " control " starts at  " (System/currentTimeMillis) (:objectstore (@session-variables ui)))
    (when mboset
      (locking mboset
        (let [_numrows (if  (-> (get-ms-info mboset) (.isPersistent))
                         numrows
                         (.getSize mboset))]
          (.moveTo mboset start-row)
          (loop [rows [] nrs 0]
            (if (= _numrows nrs)
              rows
              (let [frows
                    (try (fetch-mbovalues control ui (+ start-row nrs))
                         (catch Exception e
                           (log/error "exception when fetching " e)
                           nil))]
                (if-not frows
                  rows
                  (recur (conj rows frows) (inc nrs)))))))))))

(defn fetch-multi-rows-no-reset
  [control ui start-row numrows]
  ;;react native is sensitive to the frequent list updates, this improves the performance
  (loop [rows [] nrs 0]
    (if (= numrows nrs)
      rows
      (let [frows  (fetch-mbovalues control ui (+ start-row nrs))]
        (if-not frows
          rows
          (recur (conj rows frows) (inc nrs)))))))

(defn command-on-selection
  [control command ui]
  (with-mboset control ui mboset
    (doseq [sel_ (seq (.getSelection mboset))]
      (run-mbo-command control command ui))))

(defn get-qbe [control ui]
  (with-mboset control ui mboset
    (let [control-columns (:control-columns (@session-variables ui))
          cols  (-> (@control-columns control) set )
          ^"[Ljava.lang.String;" attrs (into-array String cols )
          ]
      (when mboset
        (interleave cols
                    (seq (.getQbe mboset attrs)))))))

(defn get-columns-qbe [control columns ui]
  (with-mboset control ui mboset
    (log/debug "getting qbe columns " columns)
    (let [^"[Ljava.lang.String;" attributes  (into-array String columns )]
      (when mboset
        (interleave columns
                    (seq (.getQbe mboset attributes)))))))

(defn set-order-by [control column ui]
  (with-mboset control ui mboset
    (.setOrderBy mboset column)
    (.reset mboset)
    (when (.getMbo mboset 0)
      (.moveFirst mboset))))

(defn add-at-end
  "creates the new mbo and adds it at and, calls the standard maximo method. For the time being, I don't see the need for the other types of adding"
  [control ui]
  (with-mboset control ui mboset
    (when (.getApp mboset)
      (access-to-option control "INSERT" ui))
    (.addAtEnd mboset)
    (fetch-mbovalues control ui)))

(defn add-at-index
  [control ui i]
  (with-mboset control ui mboset
    (when (.getApp mboset)
      (access-to-option control "INSERT" ui))
    (.addAtIndex mboset ^int i)
    (fetch-mbovalues control ui)))

(defn fetch-current
  [control ui]
  (fetch-mbovalues control ui))
  

(defn delete
  [control ui]
  (with-mbo control ui mbo
    (let [mbo-set (.getThisMboSet mbo)]
      (when (.getApp mbo-set)
        (access-to-option control  "DELETE" ui)))
    (.delete mbo)))

(defn undelete
  [control ui]
  (with-mbo control ui mbo
    (let [mbo-set (.getThisMboSet mbo)]
      (when (.getApp mbo-set)
        (access-to-option control  "DELETE" ui))
      )
    (.undelete mbo)))

                                        ;prethodna tri su specijalni slucajevi mbo comandi koji mogu da se zovu van application  containera. Ako se zovu iz application containera treba proveriti  mbo
(defn forward
  "moving the mboset forward"
  [control ui]
  (with-mboset control ui mboset
    (.moveNext mboset)))

(defn fetch-forward
  "mboset starts with the -1 position, so here it is"
  [control ui]
  (when (forward control ui)
    (fetch-mbovalues control ui)))

(declare transform-message)

(defn move-to
  [control  row ui]
  (log/debug "moving  " control "to  row " row)
  (with-mboset control ui mboset
    (when mboset
                                        ;         (log/debug "!!!!")
                                        ;        (log/debug "#################" control "  ->  " row )
      (locking mboset
        (.moveTo mboset row)
        (let [mbo (.getMbo mboset)
              unique-id (when mbo (.getUniqueIDValue mbo))]
          (when-not (nil? mbo)
            (transform-message ["set-current-index" mboset mbo unique-id row])))
        row
        ))))

(defn set-value
  "setting the value of the current mbo"
  [control ^java.lang.String attribute ^java.lang.String value ui]
  (if (is-faux-mbo control ui)
    (with-faux-mbo control ui mbo
      (if (= "_SELECTED" attribute)
        (if (= "Y" value)
          (.select mbo)
          (.unselect mbo))
        (.setValue mbo attribute value))
      (.getString mbo attribute))
    (with-mbo control ui mbo
      (if (= "_SELECTED" attribute)
        (if (= "Y" value)
          (.select mbo)
          (.unselect mbo))
        (.setValue mbo attribute value))
      (.getString mbo attribute))))

(defn set-zombie
  "setting the value of the current mbo"
  [control ^java.lang.String attribute ^java.lang.String value ui]
  (with-mbo control ui mbo
    (.setValue mbo attribute value 11)))


(defn multi-select
  [control ^java.lang.String value start-row   num-rows ui]
  (with-mboset control ui mboset
    (if (= "Y" value)
      (.select mboset ^int start-row ^int num-rows)
      (.unselect mboset ^int start-row ^int num-rows))))

(defn mboset-count
  "vraca count mboseta"
  [control ui]
  (with-mboset control ui mboset
    (when mboset
      (.count mboset))))

(defn save
  "saving mboset"
  [control ui]
  (access-to-option control "save" ui)
  (log/debug "saving the mboset")
  (if (is-faux-mbo control ui)
    (with-faux-mboset control ui mboset
      (let [owner (.getOwner mboset)]
        (.appValidate  owner)
        (.save (.getThisMboSet owner))))
    (do
      (with-mbo control ui mbo
        (log/debug "doing the appvalidate")
        (.appValidate mbo))
      (with-mboset control ui mboset
        (log/debug "doing the save on the mboset")
        (.save mboset)
    ;moving to the first row is important becuase after the save the related containers are reset, and if this wasn't moved to the previous position they would be empty
        (when (.getMbo mboset 0)
          (.moveFirst mboset))))))


(defn reset
  "if the qbe changes this has to be called first before the data change is visible on the client"
  [control ui]
  (with-mboset control ui mboset
    (when mboset
      (.reset mboset)
      (if (instance? psdi.mbo.NonPersistentMboSet mboset)
        (do
          (.setup mboset)
          (.moveFirst mboset)
          )
        (when (.getMbo mboset 0)
          (.moveFirst mboset))))))

(defn backward
  "moving the mboset forward"
  [control ui]
  (let [^psdi.mbo.MboSetRemote mboset  (@(:objectstore (@session-variables ui)) control)]
    (.movePrev mboset)))

(defn mbo-to-mboset [^psdi.mbo.MboRemote mbo]
  (.getThisMboSet mbo))

(defn mbovalue-to-mboset [^psdi.mbo.MboValue mboval]
  (-> mboval (.getMbo) (.getThisMboSet)))

(defn register-wf-director [control-name ^java.lang.String app-name ^java.lang.String process-name ^java.lang.String director-name ui]
  (with-mbo control-name ui mbo
    (let [sess @(:mxsessions (@session-variables ui))
          ^psdi.workflow.WorkflowDirector wfdir (psdi.workflow.WorkflowDirector. sess)
          ]

      (.setProcessName wfdir process-name)
      (swap! (:wfdirectors (@session-variables ui)) assoc director-name wfdir)
      wfdir)))

(defn unregister-wf-director [director-name ui]
  (swap!  (:wfdirectors (@session-variables ui)) dissoc director-name))

(defn register-wf-actions-set [ ^java.lang.String director-name actionsset-name ui & from-reassign]
  (let [^psdi.workflow.WorkflowDirector wf-dir (@(:wfdirectors (@session-variables ui)) director-name)
        ^Lpsdi.util.MXException warn-exceptions         (when-let [controlled (.getControlled wf-dir)]
                                                          (when-let [tm (.getThisMboSet controlled)]
                                                            (.getWarnings tm)
                                                            ))
        warn-messages (when warn-exceptions (map (fn [^psdi.util.MXException x] (.getDisplayMessage x)) warn-exceptions))
        ]
    {"title" (.getNextDirectionTitle wf-dir)
     "body" (.getNextDirectionBody wf-dir)
     "nextAction" (.getNextAction wf-dir)
     "nextApp" (.getNextApp wf-dir)
     "nextTab" (.getNextTab wf-dir)
     "nextUniqueId" (.getNextUniqueId wf-dir)
     "atInteractionNode"(.isAtInteraction wf-dir)
     "warnings" warn-messages
     "actions"     (if from-reassign
                     "empty"
                     (if-let [wf-set  (.getWfSet wf-dir)]
                       (register-the-mboset wf-set actionsset-name)
                       "empty"))
     }))

(defn route-wf [control-name ^java.lang.String app-name  ^java.lang.String director-name actionsset-name ui]
  (log/debug "calling  route-wf for" control-name " " app-name " " director-name " " actionsset-name )
  (with-mboset control-name ui mboset
    (log/debug "mboset:" mboset "isEmpty" (.isEmpty mboset) "getMbo" (.getMbo mboset) "getMbo0" (.getMbo mboset 0)))
  (with-mbo control-name ui mbo
    (log/debug "Routewf, calling  start input for mbo :" mbo )
    (let [^psdi.workflow.WorkflowDirector wf-dir (@(:wfdirectors (@session-variables ui)) director-name)]
      (.clearInteraction wf-dir)
      (.startInput wf-dir app-name mbo psdi.workflow.DirectorInput/workflow)
      (register-wf-actions-set director-name actionsset-name ui))))

(defn choose-wf-action [^java.lang.String director-name actionsset-name  ui]
                                        ;control will set the values in the wf action set, here just run the execute on non-persistent mbo set which in turn will route the workflow
  (let [^psdi.workflow.WorkflowDirector wf-dir (@(:wfdirectors (@session-variables ui)) director-name)
        ^psdi.mbo.NonPersistentMboSet wf-set (.getWfSet wf-dir)
        ]
    (.clearInteraction wf-dir)
    (log/debug "choosing the workflow action")
    (.execute wf-set)
    (log/debug "execute on the wf non persistent set finised")
    (when-let [controlled (.getControlled wf-dir)]
      (log/debug "got controlled" controlled)
      (let [warns (.getWarnings (.getThisMboSet controlled))
            warn-message (when warns (map (fn [^psdi.util.MXException x] (.getDisplayMessage x)) warns))]
        (log/debug "got warnings:" (if warn-message warn-message "no warnings"))
        (.input wf-dir  psdi.workflow.DirectorInput/ok)
        (let [ret (register-wf-actions-set director-name actionsset-name ui)]
          (if warns 
            (assoc ret "warnings" warn-message  )
            ret))))))

(defn initiate-wf [control-name ^java.lang.String app ^java.lang.String director-name actionsset-name ui ]
  (with-mbo control-name ui wfmbo
    (let [^psdi.workflow.WorkflowDirector wf-dir (@(:wfdirectors (@session-variables ui)) director-name)]
      (.clearInteraction wf-dir)
      (.startInput wf-dir app wfmbo psdi.workflow.DirectorInput/workflow)
      (register-wf-actions-set  director-name actionsset-name ui))))



(defn cancel-wf [director-name ui]
  (let [^psdi.workflow.WorkflowDirector wf-dir (@(:wfdirectors (@session-variables ui)) director-name)]
    (.clearInteraction wf-dir)
    (.input wf-dir    psdi.workflow.DirectorInput/cancel)))

(defn reassign-wf [actionsset-name director-name ui]
  (let [^psdi.workflow.WorkflowDirector wf-dir (@(:wfdirectors (@session-variables ui)) director-name)]
    (.clearInteraction wf-dir)
    (.input wf-dir  psdi.workflow.DirectorInput/reassign)
    (register-wf-actions-set  director-name actionsset-name ui)))

(defn execute-reassign-wf [actionsset-name director-name ui]
  (let [^psdi.workflow.WorkflowDirector wf-dir (@(:wfdirectors (@session-variables ui)) director-name)
        ^psdi.mbo.NonPersistentMboSet wf-set (.getWfSet wf-dir)
        ]
    (.clearInteraction wf-dir)
    (.execute wf-set)
    (register-wf-actions-set director-name actionsset-name ui true)
    )
  )

(defn is-active-workflow [control-name ui & use-zero ]
  ;use zero means that we should get the mbo from mboset at zero position , meaning it is not yet initialized
  (log/debug "is active wf zovem for  control-name" control-name)
  (with-mboset control-name ui mboset
    (when-let [mbo (if use-zero (.getMbo mboset 0)  (.getMbo mboset))]
      (let [
            unique-id (.getUniqueIDValue mbo)
            object-name  (-> (get-mbo-set-info control-name ui) first :objectName)
            wfinstanceset (.getMboSet mbo "$processactive$" "wfinstance"  (str "active = 1 and ownertable = '" object-name "' and ownerid =" unique-id ))
            ]
        (not (.isEmpty wfinstanceset))))))


(defn unregister-wf-director [director-name ui]
  (swap! (:wfdirectors (@session-variables ui)) dissoc director-name))

;this is the method for getting the offlien workflow actions. First we have to get the starting node (start node or the task node if there is an assignment , and then start offloading from that level.
(defn get-wf-starting-node [control-name process-name ui]
  (if-not (is-active-workflow control-name ui);if the workflow is not active take the start node, otherwise the node in assignment
    (let [object-name  (-> (get-mbo-set-info control-name ui) first :objectName)
          process (.getMboSet @server "wfprocess" ui)]
      (.setWhere process (str " active=1 and objectname='" object-name "' and processname='" process-name "'"))
      (when-let [p (.getMbo process 0)] 
        (let [process-rev (.getString p "processrev")
              start-set (.getMboSet @server "WFSTART" ui)
              _  (.setWhere start-set (str "processname='" process-name "' and processrev=" process-rev ))
              node-id (->
                       (.getMbo start-set 0)
                       (.getString "nodeid"))
              wf-nodeset (.getMboSet @server "WFNODE" ui)
              _ (.setWhere wf-nodeset (str "processname='" process-name "' and processrev=" process-rev " and nodeid=" node-id))]
          (.getMbo wf-nodeset 0))))
    (let [sqlf (psdi.mbo.SqlFormat. "ownertable = :1 and ownerid = :2 and assigncode = :3 and assignstatus in (select value from synonymdomain where domainid = 'WFASGNSTATUS' and maxvalue = 'ACTIVE')")]
      (with-mbo control-name ui mbo
        (.setObject sqlf 1 "WFASSIGNMENT" "OWNERTABLE" (.getName mbo))
        (.setLong sqlf 2 (.getUniqueIDValue mbo))
        (.setObject sqlf 3 "WFASSIGNMENT" "ASSIGNCODE" (.getPersonId ui))
        (let [assign-set (.getMboSet mbo "$CURMBOASGN" "WFASSIGNMENT" (.format sqlf))]
          (.reset assign-set)
          (when-let [assignment (.getMbo assign-set 0)]
            (let [wf-nodeset (.getMboSet @server "WFNODE" ui)
                  pr-name (.getString assignment "PROCESSNAME")
                  pr-rev (.getString assignment "processrev")
                  node-id (.getString assignment "nodeid")
                  ]
              (.setWhere wf-nodeset (str "processname='" pr-name "' and processrev=" pr-rev " and nodeid=" node-id))
              (.getMbo wf-nodeset 0))))))))

(defn get-filtered-actions
                                        ;get just the actions that pass the condition check
  [^psdi.mbo.MboRemote curr-mbo ^psdi.workflow.WFNode node]
  (when (and node (not= "STOP" (.getString node "nodetype")))
    (log/debug "Filtered actions for node " node " of type " (.getString node "nodetype") )
    (let [actions (mboset->seq (.getWorkflowActions node))]
      (filter (fn [^psdi.workflow.WFAction action]
                (if (and (.isNull action "CONDITION")  (.isNull action "CONDITIONCLASS"))
                  true
                  (let [^psdi.workflow.WorkFlowService wfs (.getMboServer action)]
                    (.evaluateCondition wfs action (.getString action "CONDITIONCLASS") (.getString action "CONDITION") curr-mbo))))
              actions))))

(defn get-eligible-node
  [^psdi.mbo.MboRemote curr-mbo ^psdi.workflow.WFAction action]
  (log/debug "Action for node " action)
  (when (and action (.isNull action "ACTION")) ;if there is an action stop, we can't perform the action while offline
    (when-let [member-node (.. action (getMboSet "MEMBERNODE") (getMbo 0))]
      (let [ntype (.getString member-node "nodetype")]
        (log/debug "Node type " ntype)
        (when-not (or  (= "INTERACTION" ntype) (= "TASK" ntype) (= "WAIT" ntype)) ; these types are not traversable while offline. task node can be just the starting point
          (if (= "STOP" ntype)
            [member-node (.getString action "ispositive")]
            (if-not (= "CONDITION" ntype)
              (let [filtered-actions (get-filtered-actions curr-mbo member-node)]
                (if-not (empty? (rest filtered-actions))
                  [member-node (.getString action "ispositive")]
                  (get-eligible-node curr-mbo (first filtered-actions))))
              (let [condition (.. member-node (getMboSet "CONDITION") (getMbo 0))
                    ^psdi.workflow.WorkFlowService wfs (.getMboServer condition)
                    ec (.evaluateCondition wfs condition (.getString condition "CUSTOMCLASS") (.getString condition "CONDITION") curr-mbo)
                    next-actions (.getWorkflowActions member-node)]
                (get-eligible-node curr-mbo (.getAction next-actions ec))))))))))

(defn get-first-real-node
  ;we don't want the start node in the output, just the nodes where the actual user interaction occurs
  [curr-mbo node]
  (if (= "START" (.getString node "nodetype") )
    (get-eligible-node curr-mbo (.. node (getWorkflowActions) (getMbo 0)))
    [node "Y"]))

(defn get-wf-objects-metadata
[ui]
  ;when prefetching for the offline, we must not start the actual workflow proces, but we still need the metadata to insert offline. Therefore we return the meta with the prefetch, and the offline code will create the offline tables
(let [iws (.getMboSet @server "INPUTWF" ui)
      sws (.getMboSet @server "COMPLETEWF" ui)
      wfa (.getMboSet @server "WFACTION" ui)]
  (register-the-mboset iws :iws true)
  (register-the-mboset sws :sws true)
  (register-the-mboset wfa :wfa true)

  (add-control-columns :iws ui ["actionid" "memo"])
  (add-control-columns :sws ui ["actionid" "taskdescription" "memo"])
  (add-control-columns :wfa ui ["actionid" "instruction" "ispositive"])
  (let [ret (map #(get-metadata % ui) [:iws :sws :wfa])]
    (doall ret)
    (doall (map #(unregister-the-mboset % ui) [:iws :sws :wfa]))
    ret
    )))

;this function will return the data required for offline
(defn- replace-subprocess-stop-nodes
  [[sp-node sp-actions sp-nextnodes] [original-node original-actions original-nextnodes]]
                                        ;this will inline the subprocess. To continue where the subprocess has finished (for offline) will be possible just if the subprocess have reached the STOP node. If not it means it will stop there until online.
;  (log/error "Original next-nodes:" original-nextnodes)
  (if-not sp-node
                                        ;subprocess-node is nill - just assume the positive subprocess outcome. Check why is null!!!
    (first (filter #(= "Y" ((first %) "frompositive")) original-nextnodes))
    (let [node-type (sp-node "type")
          from-positive (sp-node "frompositive")]
      (if (= "STOP" node-type)
        (first (filter #(= from-positive ((first %) "frompositive"))original-nextnodes ))
        [sp-node sp-actions (map (fn [n]
 ;                                  (log/error "^^^^^^^^^^^^^^^^^^^" n)
                                   (replace-subprocess-stop-nodes n [original-node original-actions original-nextnodes])) sp-nextnodes)]
        ))))

(defn pre-fetch-wf [control-name ui ^psdi.mbo.MboRemote curr-mbo [^psdi.workflow.WFNode node act-positive]]
  (let [get-node-data (fn [^psdi.workflow.WFNode x]
                        (when x
                          (let [node-type (.getString x "nodetype")
                                subprocess (if (= "SUBPROCESS" node-type)
                                             (let [sp-name (.getString  (.getMbo (.getMboSet node "subprocess") 0) "subprocessname")]
  ;                                             (log/error "Subproces name " sp-name)
                                               (pre-fetch-wf control-name ui curr-mbo  
                                                             (get-first-real-node curr-mbo
                                                                                  (get-wf-starting-node control-name sp-name ui)))                             ))
                                ret  {"type" node-type "frompositive" act-positive "description" (.getString x "description") "nodeid" (.getString x "nodeid")}]
                                        ;                         (log/error "Ret: " ret)
    ;                        (log/error "!!!!!!!!Subprocess:" subprocess)
                            [ret subprocess]
                            )))
        get-action-data (fn [x] {"actionid" (.getString x "actionid") "instruction" (.getString x "instruction") "ispositive" (.getString x "ispositive") "ownernodeid" (.getString x "ownernodeid") "membernodeid" (.getString x "membernodeid")})
        filtered-actions (get-filtered-actions curr-mbo node)
        [_dta subprocess] (get-node-data node)
        ret [_dta
             (map get-action-data filtered-actions)
             (map (partial pre-fetch-wf control-name ui curr-mbo)
                  (filter #(-> % nil? not)
                          (map (partial get-eligible-node curr-mbo)
                               filtered-actions)))]]
    (if subprocess (replace-subprocess-stop-nodes subprocess ret) ret)))

(defn prefetch-wf-for-offline
  [^String control-name ^String process-name ui]
  (with-mbo control-name ui mbo
    [(get-wf-objects-metadata ui)
     (pre-fetch-wf control-name ui mbo  
                   (get-first-real-node mbo
                                        (get-wf-starting-node control-name process-name ui)))
     ]))

;replay will be called one by one from the front-end, so the user can see how many workflows have been completed or stop the processinf
(defn offline-replay-wf
  [^String appContainer ^String appName  ^String processName wf-steps ui]
  (let [wf-id (.toString (java.util.UUID/randomUUID))
        wf-cont (register-wf-director appContainer appName processName wf-id ui)
        rez
        (route-wf appContainer appName wf-id "action-set" ui)]
    (loop [curr-rez rez wf-steps wf-steps]
      (if (or (empty? wf-steps) (= "empty" (rez "actions")))
        (do
          (unregister-the-mboset "action-set" ui)
          (unregister-wf-director wf-id ui)
          (curr-rez "warnings"))
        (with-mbo "action-set" ui action-mbo
          (let [action-id (-> wf-steps first first)
                memo (-> wf-steps first second)]
            (.setValue action-mbo "actionid" action-id)
            (.setValue action-mbo "memo" memo)
            (unregister-the-mboset "action-set" ui);it is still referenced from wf director, we deregister here just for the cleanup purposes
            (recur (choose-wf-action wf-id "action-set" ui) (rest wf-steps))))))))

(defn replay-mboset-changes [^psdi.mbo.MboSet mboset changes & not-top-level?]
  ;exception catching will be there just on the top level mbo, for children mbosets, the exceptions propagate to top level
  (let [one-ch-replay (fn [m c]
                        (try
                          (do
                            (log/debug "Replaying changes on " (.getClass m) " and " (.getUniqueIDValue m))
                            (if (c "_delete")
                              (.delete m)
                              (doseq [k (keys c)]
                                (when-not (or (= k "uniqueid") (= k "_new") (= k "parentid"))
                                  (if-not (= k "children")
                                    (.setValue m k (c k))
                                    (doseq [child (c k)]
                                      (let [rel-name (child "relationship")]
                                        (log/debug "relationship name is " rel-name)
                                        (replay-mboset-changes (.getMboSet m rel-name) (child "data") true)))))))
                            "ok")
                          (catch psdi.util.MXException mex (if not-top-level?
                                                             (throw mex)
                                                             (get-exc-details [:mx (.getDisplayMessage mex) (.getErrorGroup mex) (.getErrorKey mex)])))
                          (catch java.lang.Exception e (if not-top-level?
                                                         (throw e)
                                                         (get-exc-details [:general (.getMessage e)])))))
        ch-new (filter (fn [c] (c "_new")) changes);for better performance, first proces the new records, then just search the rest
        ch-rest (remove (fn [c] (c "_new")) changes)]
    (concat
     (doall (map (fn [c]
                   (let [m (.add mboset)]
                     [(.getUniqueIDValue m) (one-ch-replay m c)])
                   ) ch-new))
     (loop [i 0 changes ch-rest rez []]
       (let [mbo (.getMbo mboset i)]
         (if (or (not mbo) (empty? changes))
           rez
           (let [uid (.getUniqueIDValue mbo)
                 ff  (fn [d]
                       (= uid 
                          (d "uniqueid")))
                 c (first (filter ff changes))
                 _chgs (if c (remove ff changes) changes)]
             (recur (inc i) _chgs
                    (if c
                      (conj rez
                            [uid
                             (one-ch-replay mbo c)])
                      rez
                      )))))))))


(defn post-offline-changes
  [^String appContainer ^String offline-data ui]
  (let [offline-changes  (json/read-str offline-data)]
    (log/debug "Posting offline changes " offline-changes)
    (with-mboset appContainer ui mbo-set
      (let [data (offline-changes "data")]
       (replay-mboset-changes mbo-set data)))))

(defn save-offline-changes
  [^String appContainer ui & rollback?]
  (with-mboset appContainer ui mbo-set
    (if rollback? 
      (.rollback mbo-set)
      (.save mbo-set))))

(defn save-offline-changes-old
  [^String appContainer ui & rollback?]
  (with-mbo appContainer ui mbo
    (let [mbo-name (.getName mbo)
          changes (@(:pending-offline-data (@session-variables ui)) mbo-name)]
      (doseq [[^psdi.mbo.MboSet mbo-set r] changes]
        (if (or rollback? (not (= r "ok")))
          (.rollback mbo-set)
          (.save mbo-set))
        (.close mbo-set))
      (swap! (:pending-offline-data (@session-variables ui)) dissoc mbo-name))))

(defn rollback-offline-changes
  [^String appContainer ui]
  (save-offline-changes appContainer ui true))

(defn move-to-uniqueid
  [^String appContainer ^long uniqueid ui]
  (with-mboset appContainer ui mbo-set
    (loop [i 0]
      (when-let [mbo (.getMbo mbo-set i)]
        (if (= (.getUniqueIDValue mbo) uniqueid)
          (locking mbo-set
            (.moveTo mbo-set i)
            i)
          (recur (inc i)))))))


(defn get-gl-segment-count [glname ui]
  (let [gl-format ^psdi.mbo.GLFormat (-> (@session-variables ui) :glformats deref (get glname))]
    (.getSegmentCount gl-format)))

(defn get-gl-segment-info [glname _segment-no ui]
  (let [gl-format ^psdi.mbo.GLFormat (-> (@session-variables ui) :glformats deref (get glname))
        segment-no (int _segment-no)]
    {"segment-placeholder" (.segmentPlaceHolder gl-format segment-no )
     "segment-name" (.getSegmentName gl-format segment-no)
     "segment-length" (.getSegmentLength gl-format segment-no)
     "segment-required" (.isSegmentRequired gl-format segment-no)
     "screen-delimiter" (java.lang.Character/toString (.getScreenDelimiter gl-format segment-no))
     }))

(defn register-gl-format [glname orgid ui]
  (swap! (:glformats (@session-variables ui)) assoc glname (psdi.mbo.GLFormat. "" false orgid))
  (register-main-mboset "GLCOMPONENTS" glname ui)
  (for [s (range 0 (get-gl-segment-count glname ui))]
    (get-gl-segment-info glname s ui)
    ))

(defn set-segment [glname segment-values segment-no orgid ui]
  (log/debug  " set setgment++++++++++++++++++" segment-values "++" segment-no "+" orgid )
  (with-mboset glname ui mboset
    (log/debug "set segment, mboset:" mboset "$"(into-array String segment-values))
    (.findValidComponents mboset segment-no  (into-array String segment-values) false false orgid )))

(defn post-yes-no-cancel-input [^java.lang.String ex-id user-input ui]
  (let [uint (int user-input)
        uinto (java.lang.Integer. uint)]
    (.postUserInput @server ex-id uinto ui)))

(defn remove-yes-no-cancel-input [^java.lang.String ex-id  ui]
  (.removeUserInput @server ex-id  ui))

(defn register-inbox-mboset
  [control ^psdi.security.UserInfo ui]
  (let [^psdi.server.MXServer ser @server
        ^psdi.workflow.WFAssignmentSetRemote wis (.getMboSet ser "wfassignment" ui)]
    (.getInboxAssignments wis)
    (register-the-mboset wis control)
    )
)

(defn register-person-mboset
  [control ^psdi.security.UserInfo ui]
  (let [^psdi.server.MXServer ser @server
        ^psdi.mbo.MboSetRemote ms (.getMboSet ser "person" ui)]
    (.setWhere ms (str "personid='" (.getPersonId ui) "'"))
    (.reset ms)
    (register-the-mboset ms control)
    )
)


(defn register-query-mboset
  [control ^java.lang.String app ^psdi.security.UserInfo ui]
  (let [^java.lang.String username (.getUserName ui)
        ^psdi.server.MXServer ser @server
        ^psdi.mbo.MboSetRemote qs (.getMboSet ser "query" ui)]
    (.setWhere qs (str "app='" app "' and (owner='" username "' or ispublic=1)"))
    (register-the-mboset qs control)))

(defn register-bookmark-mboset
  [control ^java.lang.String app ^psdi.security.UserInfo ui]
  (let [^java.lang.String username (.getUserName ui)
        ^psdi.server.MXServer ser @server
        ^psdi.mbo.MboSetRemote qs (.getMboSet ser "bookmark" ui)]
    (.setWhere qs (str "app='" app "' and userid='" username "'"))
    (register-the-mboset qs control)))

(defn use-stored-query
  [control ^java.lang.String query-name ui]
  (with-mboset control ui mboset
    (.useStoredQuery mboset query-name)))

(defn control-from-mboset [^psdi.mbo.MboSetRemote mbo-set]
  (when-let [ui (get-user-info mbo-set)]
    (when-let [sess (@session-variables ui)]
      (get @(:mbostore sess) mbo-set))))

(defmacro oncontrol-enqueue [^psdi.mbo.MboSetRemote mbo-set message-cf]
  `(when-let [control# (control-from-mboset ~mbo-set)]
     (let [ui# (get-user-info ~mbo-set)]
       (when-let [bq# (:message-queue (@session-variables ui#))]
         (>!! bq#  (~message-cf control#))))))


(defn transform-message [x]
  (letfn [(commandmbo-param-f[x]
            (let [ar  ^"[Ljava.lang.Object;" (nth x 4)]
              (oncontrol-enqueue ^psdi.mbo.MboSetRemote(mbo-to-mboset ^psdi.mbo.Mbo(nth x 2))
                                 #(-> (assoc x 2 %) (assoc 4 (->> ar vec (map str)))))))
          (commandmbo-f[x]
            (oncontrol-enqueue ^psdi.mbo.MboSetRemote (mbo-to-mboset (nth x 2)) #(assoc x 2 %)))
          (commandmboset-f[x]
            (oncontrol-enqueue  ^psdi.mbo.MboSetRemote (nth x 2) #(assoc x 2 %)))
          (crdmbo-f[x]
            (oncontrol-enqueue ^psdi.mbo.MboSetRemote (mbo-to-mboset (second x)) #(assoc x 1 %)))
          (updatemboval-f[x]
            (let [mboval ^psdi.mbo.MboValue (second x)
                  maxt ^psdi.util.MaxType (nth x 3)]
              (oncontrol-enqueue ^psdi.mbo.MboSetRemote (mbo-to-mboset (.getMbo mboval))
                                 #(-> x (assoc 1 %)(assoc 3 [(-> mboval (.getMboValueInfo) (.getAttributeName)), (.asString maxt)])))))
          (setcurrind-f [x]
            (oncontrol-enqueue ^psdi.mbo.MboSetRemote (nth x 1) #(assoc
                                                                     (vec
                                                                      (concat
                                                                       (subvec x 0 2)
                                                                       (subvec x 3)
                                                                       )
                                                                      )
                                                                   1 %)))]
    ((case (first x)
       "commandmbo-param" commandmbo-param-f
       "commandmbo" commandmbo-f
       "commandmboset" commandmboset-f
       "updatembovalue" updatemboval-f
       "set-current-index" setcurrind-f
       crdmbo-f)x)))

(defn >** [message]
                                        ;for the time being, synchronoulsy dispatch call transform message
                                        ;We can also try to put futures here, but the probem is that the messages have to be in order. Another option is to use queue(maybe just a vector), and insert into the queue, and have another thread processing
                                        ;  (log/error "From maximo " message)
  (when-not (= "set-current-index-internal" (first message))
    (transform-message message)))
