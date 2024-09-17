(ns maximoplus.lic
  (:require [buddy.sign.jwt :as jwt]
            [buddy.core.keys :as keys]
;;            [clojure.tools.logging :as log]
            [maximoplus.logger :as log]
            [clojure.java.io :as io]
            [clojure.string :refer [split]])
  
)

(defn get-license
  "licence key must be on the classpath. private key password is the company name"
  []
  (let [lic-key (-> "lic.key" io/resource slurp)
        company (second (re-find #"startcomp(.*)123endcomp" lic-key))
        [license priv-key] (split lic-key (re-pattern (str "startcomp" company "123endcomp")))]
    (jwt/decrypt license (keys/str->private-key priv-key company)
                 {:alg :rsa-oaep
                  :enc :a128cbc-hs256})))


(def lic-keys (atom nil))

(defn get-lic-keys
  []
  (when-not @lic-keys
    (let [lic (get-license)]
      (log/error lic)
      (reset! lic-keys lic )))
  @lic-keys)

(defn check-lic-date
  []
  (when (.after (java.util.Date.)
                (.parse (java.text.SimpleDateFormat. "ddMMyyyy") (:expiry-date (get-lic-keys))))
    (throw (Exception. "MAXIMOPLUS LICENCE EXPIRED!"))))


