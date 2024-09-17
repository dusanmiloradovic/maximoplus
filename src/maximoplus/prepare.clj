(ns maximoplus.prepare
  (:use clojure.repl)
  (:use clojure.java.io)
  (:require [clojure.string :as str])
  (:gen-class)
  )

(defn mk-tmp-dir!
  "Creates a unique temporary directory on the filesystem. Typically in /tmp on
  *NIX systems. Returns a File object pointing to the new directory. Raises an
  exception if the directory couldn't be created after 10000 tries."
  []
  (let [base-dir (file (System/getProperty "java.io.tmpdir"))
        base-name (str (System/currentTimeMillis) "-" (long (rand 1000000000)) "-")
        tmp-base (str base-dir (System/getProperty "file.separator") base-name)
        max-attempts 10000]
    (loop [num-attempts 1]
      (if (= num-attempts max-attempts)
        (throw (Exception. (str "Failed to create temporary directory after " max-attempts " attempts.")))
        (let [tmp-dir-name (str tmp-base num-attempts)
              tmp-dir (file tmp-dir-name)]
          (if (.mkdir tmp-dir)
            tmp-dir
            (recur (inc num-attempts))))))))

(defn entries [zipfile]
  (enumeration-seq (.entries zipfile)))

(defn walkzip [fileName f]
  (with-open [z (java.util.zip.ZipFile. fileName)]
    (doall (map (fn [e] (f z e)) (entries z)) 
           )))

(defn printzip [filename]
  (walkzip filename (fn [z e] (println (.getSize e) (.getName e))))
  )

(defn get-current-directory []
  (.getAbsolutePath (file ""))
  )


(defn zcopy [^java.io.InputStream zip-stream  fileName]
  (let [out-file (file fileName)]
    (if (.isDirectory out-file)
      (.mkdir out-file)
      (let [out (java.io.BufferedOutputStream. (java.io.FileOutputStream. out-file) 1024)
            data (byte-array 1024)]
        (loop [len (.read zip-stream data 0 1024)]
          (if (not (= -1 len))
            (do (.write out data 0 len)
                (recur (.read zip-stream data 0 1024))))
          )
        (.flush out)
        (.close out)
        (.toURL out-file)
        ))))

(defn copy-directory 
  "to-dir is a root dir"
  [ffrom fTo]
                                        ;  (println "copy " ffrom " to " fTo)
  (if-not (.isDirectory ffrom)
    (try (copy ffrom fTo) (catch Exception e (.printStackTrace e)))
    (do
      (.mkdir fTo)
      (doseq [ch (seq (.listFiles ffrom))]
        (copy-directory ch (java.io.File. fTo (.getName ch)))))))


(defn make-directory-structure [project-name maximo-ear]
  (let [rt (file (str  "deployment/" project-name))
        ff (file (str  "deployment/" project-name "/lib"))
        mjrs (file (str "deployment/" project-name "/maximo_jars"))
        maximoplus_jars (file (str "deployment/" project-name "/maximoplus_jars"))
        tmp-dir (mk-tmp-dir!)
        tmp-dir-path (.getAbsolutePath tmp-dir)
        ]
    (.mkdirs rt)
    (.mkdirs ff)
    (.mkdirs mjrs)
    (.mkdirs maximoplus_jars)
    (println "Copying Maximo files")
    (let [jars (filter 
                #(-> % nil? not)
                (walkzip maximo-ear
                         (fn [z e]
                           (try
                             (let [len (.getSize e)
                                   fileName (.getName e)
                                   ^InputStream is (.getInputStream z e)]
                               (when (and (.endsWith fileName ".jar")
                                          (not (str/includes? fileName "test/lib/")))
                                 (if (.startsWith fileName "lib/")
                                   (when (and
                                          (not (.startsWith fileName "lib/db2"))
                                          (not= fileName "lib/tools.jar"))
                                     (zcopy is (str "deployment/" project-name "/" fileName)))
                                   (zcopy is (str "deployment/" project-name "/maximo_jars/" fileName)))))
                             (catch Exception e (.printStackTrace e))))))
          new-cl (java.net.URLClassLoader. (into-array java.net.URL jars))
          ]
      (println "Preparing MaximoPlus environment")
      (.setContextClassLoader (Thread/currentThread) new-cl)
      (.doit (maximoplus.asm.MinimoClassChanger.))

      (walkzip (str "deployment/" project-name "/maximo_jars/businessobjects.jar")
               (fn [z e]
                 (let [fileName (.getName e)
                       ^InputStream is (.getInputStream z e)]
                   (if (.isDirectory e)
                     (.mkdirs (file (str tmp-dir-path "/" fileName)))
                     (zcopy is (str tmp-dir-path "/" fileName) )))))
      )
    (println "Using temporary directory:" tmp-dir-path)
    (doseq [c ["Mbo.class" "MboSet.class" "MboValue.class" "FauxMboSet.class"]]
      ;;      (println "!!" c)
      (let [fjl (file (str tmp-dir-path "/psdi/mbo/" c))
            fjlTemp (file (str   (System/getProperty "java.io.tmpdir") "/" c))]
        (.delete fjl)
        (copy fjlTemp (file (str tmp-dir-path "/psdi/mbo/" c)))))
    (copy (file (str   (System/getProperty "java.io.tmpdir") "/ServiceStorage.class")) (file (str tmp-dir-path "/psdi/server/ServiceStorage.class")))
    (copy (file (str   (System/getProperty "java.io.tmpdir") "/CommonUtil.class")) (file (str tmp-dir-path "/psdi/util/CommonUtil.class")))
    (copy (file (str   (System/getProperty "java.io.tmpdir") "/ReportAdminService.class")) (file (str tmp-dir-path "/com/ibm/tivoli/maximo/report/birt/admin/ReportAdminService.class")))
    
    (println "Copying MaximoPlus files")
    (.delete (file (str "deployment/" project-name "/maximo_jars/businessobjects.jar")))
    (maximoplus.asm.MinimoClassChanger/zip tmp-dir (file (str "deployment/" project-name "/maximo_jars/businessobjects.jar") ))

    (.mkdir (file (str "deployment/" project-name "/public")))
    (let [st (file (str (System/getProperty "user.dir") "/target"))
          fl (first (filter #(.endsWith (.getName %) "standalone.jar") (seq (.listFiles st))))
          tof  (file (str "deployment/" project-name "/maximoplus_jars/mp.jar"))
          ]
                                        ;      (.createNewFile tof)
      (copy fl tof)
      (copy-directory (file "extra-libs") (file (str "deployment/" project-name "/maximoplus_jars"))))
    (let [script8 "java -DcorsAllowed=* -cp .:\"maximoplus_jars/*\":\"lib/*\":\"maximo_jars/*\" maximoplus.core"
          script  "java -DcorsAllowed=* --illegal-access=warn --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED  -cp .:\"maximoplus_jars/*\":\"lib/*\":\"maximo_jars/*\" maximoplus.core"]
      (with-open [wr8 (writer (str "deployment/" project-name "/start8.sh"))]
        (.write wr8 script8))
      (with-open [wr (writer (str "deployment/" project-name "/start.sh"))]
        (.write wr script))
      )
    (let [script8 "java -DcorsAllowed=* -cp .;\"maximoplus_jars/*\";\"lib/*;maximo_jars/*\" maximoplus.core"
          script "java -DcorsAllowed=* --illegal-access=warn --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED -cp .;\"maximoplus_jars/*\";\"lib/*;maximo_jars/*\" maximoplus.core"]
      (with-open [wr8 (writer (str "deployment/" project-name "/start8.bat"))]
        (.write wr8 script8))
      (with-open [wr (writer (str "deployment/" project-name "/start.bat"))]
        (.write wr script)))
    (.setExecutable (file  (str "deployment/" project-name "/start8.sh")) true false)
    (.setExecutable (file  (str "deployment/" project-name "/start8.bat")) true false)
    (.setExecutable (file  (str "deployment/" project-name "/start.sh")) true false)
    (.setExecutable (file  (str "deployment/" project-name "/start.bat")) true false)
    ))




(defn -main [& args]
  (println "Preparing MaximoPlus server")
  (make-directory-structure (first args) (second args))
  )
