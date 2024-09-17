(ns maximoplus.hook
  (:require [maximoplus.maxinterop.core :as c :refer [>**]])
  (:import [psdi.util MXSession]
           [psdi.server MXServer]
           [psdi.mbo MboValueListener Mbo Mbo MboSet MboRemote MboSetRemote])
  (:gen-class
   :name maximoplus.Hook
   :prefix "-"
   :methods [^{:static true}[addMbo [psdi.mbo.MboRemote,long,long] void]
             ^{:static true}[createMbo [psdi.mbo.MboRemote, long] void]
             ^{:static true}[updateMbo [psdi.mbo.MboValue, long, psdi.util.MaxType] void]
             ^{:static true}[removeMbo [psdi.mbo.MboRemote,long] void]
             ^{:static true}[deleteMbo [psdi.mbo.MboRemote,long] void]
             ^{:static true}[commandMbo [psdi.mbo.MboRemote,long,java.lang.String] void]
             ^{:static true}[commandMboWithParam [psdi.mbo.MboRemote,long,java.lang.String, "[Ljava.lang.Object;"] void]
             ^{:static true}[commandMboSet [psdi.mbo.MboSetRemote,java.lang.String] void]
             ^{:static true}[setMboSetCurrIndex [psdi.mbo.MboSetRemote ,psdi.mbo.MboRemote,long,long] void]]))

(defn -addMbo
  [^psdi.mbo.MboRemote mbo ^long position ^long uniqueid ]
  (>**  ["addmbo" mbo uniqueid position])
  )

(defn -createMbo
  [^psdi.mbo.MboRemote mbo ^long uniqueid ]
  (>**  ["creatembo" mbo uniqueid])
  )

(defn -removeMbo
  [^psdi.mbo.MboRemote mbo ^long uniqueid]
  (>**  ["removembo" mbo uniqueid]
           )
  )

(defn -updateMbo
  [^psdi.mbo.MboValue mboval ^long mbouniqueid ^psdi.util.MaxType val]
  (>**  ["updatembovalue" mboval mbouniqueid val])
  )

(defn -deleteMbo
  [^psdi.mbo.MboRemote mbo ^long uniqueid]
  (>**  ["deletembo" mbo uniqueid])
  )

(defn -commandMbo
  [^psdi.mbo.MboRemote mbo ^long uniqueid ^String command]
  (>**  ["commandmbo" command mbo uniqueid])
  )

(defn -commandMboSet
  [^psdi.mbo.MboSetRemote mboset command]
  (>**  ["commandmboset" command mboset])
  )

(defn -commandMboWithParam
  [^psdi.mbo.MboRemote mbo ^long uniqueid ^String command ^"[Ljava.lang.Object;" parametar]
  (>**  ["commandmbo-param" command mbo uniqueid parametar])
  )

(defn -setMboSetCurrIndex
  [^psdi.mbo.MboSetRemote mboset ^psdi.mbo.MboRemote mbo ^long position ^long uniqueid ]
  (>**  ["set-current-index-internal" mboset  mbo uniqueid position]))

(defn -main []
  (println "main")
  )



