(defproject maximoplus "1.0.0-SNAPSHOT"
  :description "maximoplus server"
  :dependencies [
                 [org.maximoplus/j2ee_jar "0.0.1"]
                 ;;                 [org.maximoplus/log4j_jar "0.0.1"]
                 ;;                 [org.maximoplus/properties_jar "0.0.1"]
                 ;;                 [org.maximoplus/resources_jar "0.0.1"]
                 ;;               ;;  [org.maximoplus/oraclethin_jar "0.0.1"]
                 ;;                 [org.maximoplus/businessobjects "0.0.1_patched"]
                 ;;above are the jars compiled with maximo 6. There is no difference in the end product, all binaries are removed
                 [org.maximoplus/businessobjects "7.5.0"]
                 [org.maximoplus/properties "7.5.0"]
                 [org.maximoplus/log4j "7.5.0"]
                 [oracle/ojdbc7 "1.0.0"]
                 [org.maximoplus/icu4 "7.5.0"]
                 [org.maximoplus/jviews-chart "7.5.0"]
                 [org.maximoplus/jviews-diagrammer "7.5.0"]
                 [org.maximoplus/jviews-framework-lib "7.5.0"]
                 [org.maximoplus/jviews-gantt "7.5.0"]
                 [org.maximoplus/json4j "7.5.0"]
                 [org.maximoplus/jdom "7.5.0"]
                 
                 ;;the above ones are localrepo

                 [org.ow2.asm/asm "7.0"]
                 [org.ow2.asm/asm-commons "7.0"]
                 [org.ow2.asm/asm-util "7.0"]
                 [org.ow2.asm/asm-analysis "7.0"]

                 [compojure "1.6.0"]
                 [org.immutant/immutant "2.1.10"  :exclusions [ch.qos.logback/logback-classic]]
                 [org.apache.logging.log4j/log4j-core "2.0.2"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.0.2"]
                 [com.cognitect/transit-clj "0.8.300" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/clojure "1.10.0"]
                 [buddy/buddy-sign "1.5.0" ]
                 [buddy/buddy-core "1.2.0"]
                 [ring-logger "1.0.1"]
                 [ring/ring-core "1.8.1"]
                 [clj-time "0.15.2"]
                 [org.xerial/sqlite-jdbc "3.34.0"]
                 [com.github.seancorfield/next.jdbc "1.1.646"]
                 ]
  :source-paths ["src"]
  ;;  :repl-options  {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]  :source-paths ["src-cljs"]}


  :java-source-paths [ "workspace/maximoplus-java"]
  :jvm-opts ["-Xmx1g" "-server"] 
                                        ;  :jvm-opts ["-javaagent:agent/agent.jar" ]

                                        ;  :jvm-opts ["-Xdebug" "-Xnoagent" "-Xrunjdwp:transport=dt_socket,address=8002,server=y,suspend=n"]
                                        ;:extra-classpath-dirs ["/usr/lib/jvm/java-6-openjdk/lib/tools.jar"]
;  :javac-options ["-g" "-target" "1.7" "-source" "1.7"]

                                        ;  :offline
                                        ;  :offline? true

  :aot :all
  :omit-source :true
  :main maximoplus.core
  :plugins [
            [lein-localrepo "0.5.4"]
            [cider/cider-nrepl "0.27.2"]
	    ]
  :uberjar-exclusions  [#"^psdi" #"^custom"  #"COM.mro" #"oracle" #"maximo\\..*.properties" #"ldapsync" #"logging.properties" #"^createch" #"^javax.jms" #"webclient.properties" #"encrypt.properties" #"actuate" #"maximo.properties" #"^resources" #"doclink.properties" #"project.clj" #"^org.bouncycastle" #"^com.ibm.ism" #"^com.ibm.tivoli"]
  )

(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))
