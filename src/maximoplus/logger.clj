(ns maximoplus.logger
  (:import [psdi.util.logging MXLoggerFactory MXLogger]))


(def logger (atom nil))

(defn get-logger []
  (if @logger
    @logger
    (let [lgr (MXLoggerFactory/getLogger "maximoplus")]
      (reset! logger lgr)
      lgr)))

(defn log* [level message]
  (let [lgr (get-logger)]
    (condp = level
      :debug (.debug lgr message)
      :info (.info lgr message)
      :error (.error lgr message)
      :warn (.warn lgr message))))

(defmacro log
  [level x & more]
  (if (nil? more)
    `(log* ~level ~x)
    `(log* ~level (pr-str ~x ~@more ))))

(defmacro debug
  [& args]
  `(log :debug ~@args))

(defmacro info
  [& args]
  `(log :info ~@args))

(defmacro error
  [& args]
  `(log :error ~@args))

(defmacro warn
  [& args]
  `(log :warn ~@args))
