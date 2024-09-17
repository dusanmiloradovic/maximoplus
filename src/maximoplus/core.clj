(ns maximoplus.core
  (:gen-class)
  (:use clojure.repl)
  (:use [ring.middleware.file]
        [ring.middleware.file-info]
        [ring.middleware.content-type]
        [ring.middleware.params]
        [ring.middleware.multipart-params]
        [ring.middleware.session]
        [maximoplus.maxinterop.core]
        [maximoplus.listener]
        )
  (:require [ring.util.response :as r]
            ;;    [clojure.tools.logging :as log]
            [maximoplus.logger :as log]
            [clj-time.core :as tm]
            [clojure.data.json :as json]
            [cognitect.transit :as transit]
            [clojure.walk :refer [stringify-keys]]
            [clojure.core.async :refer [timeout alts! go go-loop <! >! chan buffer poll!]]
            [compojure.core :refer [ANY POST defroutes]]
            [immutant.web :as web]
            [immutant.web.async :as async]
            [immutant.web.sse :as sse]
        ;;    [immutant.web.middleware :refer [wrap-session]]
            [maximoplus.lic :refer :all]
            [ring.logger :as logger]
            [maximoplus.sqlite.core :as sqlitecore :refer [get-generated-server-file]]
            [clojure.set :as s :refer [difference]]
            )         
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream])
  )

(defn exc-response
  [exc-message]
  {:status  500
   :headers {}
   :body    (json-format exc-message)}
  )

(defn process-exception
  [^java.lang.Exception exception exc-message & ui]
  (.printStackTrace exception)
  (log/error "GOT THE EXCEPTION:")
  (when ui (log/error "For the user" (.getUserName (first ui))))
  (log/error "E^^^^^^^^^^^^^^" exc-message (System/currentTimeMillis))
  (log/error exception )
  (exc-response exc-message)
  )

(defn process-error
  [^java.lang.Error error exc-message]
  (log/error "FATAL ERROR:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::")
  (log/error "E^^^^^^^^^^^^^^" exc-message (System/currentTimeMillis))
  (log/error error)
  (exc-response exc-message))

(def max-sessions (atom {}))



(def not-logged-response
  "Not logged in"
  {:status  401
   :headers {}
   :body    (json-format [:general "Not logged in"])})

(defn send-logout-signal
  [ui sse-session? imc]
  ;;it can happen that the channel is not realized yet, so realize it with the keyword
  (when imc
    (if-not (realized? imc)
      (deliver imc :kill)
      (let [im-chan @imc]
        (when (async/open? im-chan)
          (if sse-session?
            (sse/send! im-chan (json-format [["logout"]]) :close? true)
            (async/send! im-chan (json-format [["logout"]]) :close? true)))))))

(declare get-ui-from-sym)

(declare get-ui-from-request)

(defn disconnect-the-session [sess-sym virt-session]
  (when-let [mx-sess (:mxsessions (@session-variables (get-ui-from-sym sess-sym virt-session)))]
    (when-let [^psdi.util.MXSession sess @mx-sess]
      (.disconnect sess)
      )))


(defn get-ui-from-sym [sess-sym virt-session]
  ((:ui  (@max-sessions sess-sym)) virt-session))

(defn kill-session
  [sess-sym virt-session]
  (log/debug "Killing the " sess-sym " and " virt-session)
  (let [ui (get-ui-from-sym sess-sym virt-session)]
    (let [_sse-session (:sse-session (@session-variables ui))
          sse-session? (when _sse-session @_sse-session)
          _imc (:immutant-channel (@session-variables ui))
          imc (when _imc @_imc)]
      (disconnect-the-session sess-sym virt-session)
      (kill-session-data ui)
      (let [gg (@max-sessions sess-sym)
            _ui (dissoc (:ui gg) virt-session)
            _lat (dissoc (:last-accessed-time gg) virt-session)
            _llt (dissoc (:last-longpoll-time gg) virt-session)
            ]
        (if (empty? _ui)
          (swap! max-sessions
                 dissoc sess-sym)
          (swap! max-sessions assoc sess-sym
                 (assoc gg :ui _ui :last-accessed-time _lat :last-longpoll-time _llt))))
      (try 
        (send-logout-signal ui sse-session? imc)
        (catch Exception e
          (log/error "Error when sending logout")
          (log/error e)))
      "ret")))

(defn kill-all-sessions
                                        ;for testing of course, will not be exposed
  []
  (doseq [s (keys @max-sessions)]
    (doseq [vs (-> @max-sessions (get s) :ui keys)]
      (kill-session s vs))))

(defn login [login-credntials login-f]
  (login-f login-credntials)
  )

(def ^:dynamic *login-method* :nologin)
                                        ;TODO ovo treba iz konfig fajla, ili jos bolje parametar
(declare get-login-method)

(defn create-new-login-session
  [sess-sym username ui]
  (check-number-of-running-servers)
  (let [_nrus (:users-per-server (get-lic-keys))
        number-of-users  (if (integer? _nrus)
                           _nrus
                             (Integer/parseInt _nrus) )
        ex-users (-> (conj  (map (fn[[k v]] (:username v)) @max-sessions) username) distinct count)]
    (when (> ex-users number-of-users)
      (throw (Exception. (str "Maximum number of users per server:" number-of-users " has been exceeded!")))))
  (log/debug "create-new-login-session started")
  (swap! max-sessions assoc sess-sym {:username username :login-time (tm/now) :last-accessed-time {:dummy (tm/now)} :loginui ui :last-longpoll-time {:dummy (tm/now)} :ui {}}) ;dummy sessions : if the user logs in, but quits after page-init(it hapeens for slow logins), session variable never gets expired. We put this just for any case
  (log/debug "after alter session " @max-sessions)
  )

(defn create-new-virt-session
  [sess-sym virt-sess]
  (log/debug "calling create-new-virt-session " sess-sym " and " virt-sess)
  (let [s (@max-sessions sess-sym)
        ui (if-let [_ui (:loginui s)]
             _ui
             (create-user-info (:username s)))
        new-s (assoc s :loginui nil :ui (assoc (:ui s) virt-sess ui) :last-accessed-time  (assoc (:last-accessed-time s) virt-sess (tm/now)) :last-longpoll-time (assoc (:last-longpoll-time s) virt-sess (tm/now)))]
    (swap! max-sessions assoc sess-sym new-s)
    (log/debug "after new virt session " @max-sessions)
    ))


(defn gen-max-sess-id
  []
  (str (java.util.UUID/randomUUID)))

(defn browser-login [username password]
  (log/debug "calling browser-login for  " username)
  (let [new-sess (gen-max-sess-id)
        ui (if (= "maximo" (get-login-method)) 
             (create-user-info username password)
             (create-user-info username))]
    (create-new-login-session new-sess username ui)
    (log/debug "returning new-sess" new-sess)
    new-sess)
  )

(defn general-login [login-method headers credentials]
  (log/debug "calling general-login for " login-method " i " credentials)
  (let [^maximoplus.Login loginO (clojure.lang.Reflector/invokeConstructor (java.lang.Class/forName  login-method) (into-array []))
        ^maximoplus.LoginResponse login-response (.login loginO headers credentials) 
        ^java.lang.String logged-in-user (.getUserName login-response)]
    (if logged-in-user
      (let [new-sess (gen-max-sess-id)
            ui (create-user-info logged-in-user)]
        (create-new-login-session new-sess logged-in-user ui)
        new-sess)
      login-response
      ))
  )

(defn get-ui-from-session
  [session params]
                                        ;todo get the t parameter and fetch from the ui map
  (log/debug "ui from sess 1")
  (when-let [sess-id (:max-sess-id session)]
    (log/debug "get ui from sesssion" )
    (log/debug "params" params)
    (let [virt-sess (when params (params "t"))
          s (@max-sessions sess-id)]
      (when s
        (if-let [lui (:loginui s)]
          lui
          ((:ui s) virt-sess))))))

(defn longpoll-channel
  [session parameters request]
  (if-let [ui (get-ui-from-session session parameters)]
    (let [sess-sym  (:max-sess-id session)
          virt-sess (parameters "t")
          sess (@max-sessions sess-sym)
          immutant-chan (:immutant-channel (@session-variables ui))
          sse-session (:sse-session (@session-variables ui))
          ]
      (swap! max-sessions assoc sess-sym (assoc sess :last-longpoll-time (assoc (sess :last-longpoll-time) virt-sess (tm/now))))
      (reset! sse-session false)
      (->
       (async/as-channel request
                         {:on-open (fn [channel]
                                     (deliver @immutant-chan channel)
                                     )
                          :on-close (fn [channel {:keys [code reason]}]
                                      (log/debug "immutant chan closed" reason)
                                      (reset! immutant-chan (promise)))})
       (update-in [:headers] assoc "Channel-Type" "MaximoPlus LongPoll Channel")
       (update-in [:headers] merge no-cache-header)
       (add-cors-to-response request )))
    not-logged-response))

(defn sse-channel
  [{session :session, parameters :params :as request}]
  (log/debug "entering the sse channel" session " and params" parameters)
  (if-let [ui (get-ui-from-session session parameters)]
    (let [sess-sym  (:max-sess-id session)
          virt-sess (parameters "t")
          sess (@max-sessions sess-sym)
          immutant-chan (:immutant-channel (@session-variables ui))
          sse-session (:sse-session (@session-variables ui))
          ]
      (when (realized? @immutant-chan);replace the old one
        (log/debug "replacing the immutant channel")
        (reset! immutant-chan  (promise)))
      (swap! max-sessions assoc sess-sym (assoc sess :last-longpoll-time (assoc (sess :last-longpoll-time) virt-sess (tm/now))))
      (reset! sse-session true)
      (->
       (sse/as-channel request
                       {:on-open (fn [channel]
                                   (log/debug "open inutant channel" channel)
                                   (deliver @immutant-chan channel)
                                   )
                        :on-close (fn [channel {:keys [code reason]}]
                                    (log/debug "immutant chan closed" reason)
                                    (reset! immutant-chan  (promise)))})
       (update-in [:headers] assoc "Channel-Type" "MaximoPlus Stream Channel")
       (update-in [:headers] merge no-cache-header)
       (add-cors-to-response request ))
      )
    not-logged-response)
  )

(defn logout [{session :session parameters :params}]
  (let [sess-symbol (:max-sess-id session)
        virt-session (parameters "t")]
    (kill-session sess-symbol virt-session)
    not-logged-response
    ))


(defn longpoll-batch-channel
  [{session :session, parameters :params :as request}]
  (longpoll-channel session parameters request))

(defn page-init [{uri :uri session :session body :body params :params :as req}]
  (log/debug "page init " uri " session " session " params " params " requenst " req)
  (let [bd (let [r (-> (rand) str)
                 i (.lastIndexOf r ".")]
             (.substring r (inc i)))];random number, used as virtual session (so the multi-tab is possible, one common real-session + virtual session for each tab)
    (log/debug "generated random virtual session on page init " bd)
    (let [sess-id  (:max-sess-id session)]
      (log/debug "page init sess-str" sess-id)
      (log/debug @session-variables)
      (if (@max-sessions sess-id)
        (do
          (log/debug "page init calling create new virtual session")
          (create-new-virt-session sess-id bd)
          (log/debug "page init finished create new virtual session")
          (response-no-cache (json-format  bd)))
        not-logged-response))))


(defn json-format-response-no-cache [x]
  (-> x json-format response-no-cache))

(defn get-login-method
  []
  (if-let [props @properties]
    (if-let [login-method (.getProperty props "maximoplus.loginMethod")]
      login-method
      "none"
      )
    "none"))

(defn web-login [{params :params session :session headers :headers :as request}]
  (try
    (log/debug "headers" headers)
    (log/debug "params:" params)
    (let [credentials (when (params "credentials") (json/read-str (params "credentials")))
          _ (log/debug "credentials:" credentials)
          login-method (get-login-method)
          username (if credentials (credentials "username") (params "username"))
          password(if credentials (credentials "password") (params "password"))] 
      (if-let [sess-id 
               (if (= "none" login-method)
                 (browser-login username nil)
                 (if (= "maximo" login-method)
                   (browser-login username password)
                   (general-login login-method  (assoc headers "remote-addr" (:remote-addr request)) credentials)))]
        (if (string? sess-id)
          (-> (json-format-response-no-cache (if-let [last-uri (:last-uri session)]
                                               last-uri "index.html")) (assoc :session (assoc session :max-sess-id sess-id)))
                                        ;no username, additional call from the browser is required. For the time being this is just the fix for the SPNEGO SSO, but maybe it will be required for somethig else as well.
          (let [^maximoplus.LoginResponse resp sess-id
                http-headers (into {} (.getHttpHeaders resp))
                status  (.getStatus resp)
                body (.getBody resp)
                ]
            {:status status, :headers http-headers, :body "OK"}
            ))
        not-logged-response))
    (catch java.lang.Exception e (process-exception e [:general (.getMessage e)] ))
    (catch java.lang.Error e (process-error e [:general (.getMessage e)]))))

(defn wrap-exception [f]
                                        ;all the errors should be normally handled by the framework. If the error propagates to the ring itself, this should handle the issue.
  (fn [request]
    (try (f request)
         (catch Exception e
           (process-exception e [:general (.getMessage e)])
           ))))

(defn wrap-maxsec [app]
  (fn[request]
    (log/debug "calling wra-maxsec with the request" request)
    (let [response (app request)
          uri (:uri request)
          session (:session request)
          body (:body response)
          parameters (:params request)]
      (log/debug "wrap-maxsec uri" uri)
      (log/debug "wrap-maxsec session" session)
      (log/debug "wrap-masec parameteres" parameters)
      (when response
        (if (some #(= uri %) ["/server/login" "/server/general-login" "/server/init" ])
          response
          (if (get-ui-from-session session parameters) response not-logged-response))))))



(defn wrap-add-cors-header [app]
  (fn [request]
    (let [response (app request)
          _ (log/debug "before the cors wrap " response)
          _newresp       (add-cors-to-response response request)]
      (log/debug "after the cors wrap" _newresp)
      _newresp
      )))


(defn get-additional-headers
  []
  (when-let
      [headers(.getProperty @properties "staticHeaders")]
    (let [splitted (.split headers "@@@")]
      (into {}
            (map (fn [s]
                   (let [i (.indexOf s ":")
                         k (.substring s 0 i)
                         v (.substring s (inc i))]
                     {k v}))
                 splitted)))))

(defn wrap-additional-headers [app]
  ;;for static pages to add headers
  (fn [request]
    (let [response (app request)
          add-headers (get-additional-headers)]
      (if-not add-headers
        response
        (assoc response :headers
               (merge (:headers response)
                      add-headers
                      ))))))

                                        ;delete the following once it goes to production. It is used to inspect why the connection is closed even though the actions are serialezed
(defn hack-field
  "Access to private or protected field. field-name must be something Named

   class - the class where the field is declared
   field-name - Named
   obj - the instance object, or a Class for static fields"
  [class field-name obj]
  (-> class (.getDeclaredField (name field-name))
      (doto (.setAccessible true))
      (.get obj)))

(def ^:dynamic *debugexc* false)

                                        ;(defn- debug-exception
                                        ;  [control-name ui]
                                        ;  (with-mboset control-name ui mboset
                                        ;    (when (and mboset #'*debugexc*)
                                        ;      (let [cD (hack-field psdi.mbo.MboSet "connectionDetail" mboset)
                                        ;            lC (hack-field psdi.mbo.MboSetConnectionDetails "lastClose" cD)
                                        ;            ]
                                        ;        (log/error ">> Last close")
                                        ;        (.printStackTrace lC)
                                        ;        (log/error "<< Last close")))))


(defn dispatch-c
  [command data ui]
                                        ;  (log/error "inside the dispatch-c method "command ":" data "time:" (System/currentTimeMillis))
  (let [control-name (first data)
        _len (count data)]
    (try
      (case command
        "set-qbe" (do
                    (let [ qbe-attr ^java.lang.String (nth data 1)
                          qbe-expr ^java.lang.String (nth data 2)]
                      (set-qbe control-name qbe-attr qbe-expr ui))
                    (json-format-response-no-cache  (get-qbe control-name ui)))
        "register-main-mboset"    (let [mbo-name (second data)]
                                    (log/debug "Calling register main mbo set for mbo-name" mbo-name)
                                    (response-no-cache (json-format (register-main-mboset mbo-name control-name ui))))
        "get-metadata" (response-no-cache
                        (json-format
                         (get-mbo-set-info control-name ui)))
        "add-control-columns" (let [columns (second data)]
                                (add-control-columns control-name ui (set columns))
                                (response-no-cache (json-format (get-metadata control-name ui)))
                                )
        "remove-control-columns" (
                                  let [columns (second data)]
                                  (remove-control-columns control-name ui (set columns))
                                  (response-no-cache (json-format (get-metadata control-name ui)))
                                  )
        "forward" (if (forward control-name ui)
                    (json-format-response-no-cache "ok")
                    (json-format-response-no-cache "norow"))
        "backward" (if (backward control-name ui)
                     (json-format-response-no-cache "ok")
                     (json-format-response-no-cache "norow"))
        "fetch" (if-let [row (fetch-forward control-name ui)] 
                  (response-no-cache (json-format row))
                  (json-format-response-no-cache "norow"))
        "fetch-no-move" (if-let [row (fetch-mbovalues control-name ui)] 
                          (response-no-cache (json-format row))
                          (json-format-response-no-cache "norow"))
        "fetch-current" (if-let [row (fetch-current  control-name ui)]
                          (response-no-cache (json-format row))
                          (json-format-response-no-cache "norow"))
        "fetch-multi-rows" (let [start-row  ^java.lang.Long (second data)
                                 sr (.intValue start-row)
                                 numrows ^java.lang.Long (nth data 2)
                                 nrs (.intValue numrows)
                                 ]
                             (if-let [rows (fetch-multi-rows control-name ui sr nrs)] 
                               (response-no-cache (json-format rows))
                               (json-format-response-no-cache "norow")))
        "fetch-multi-rows-no-reset" (let [start-row  ^java.lang.Long (second data)
                                          sr (.intValue start-row)
                                          numrows ^java.lang.Long (nth data 2)
                                          nrs (.intValue numrows)
                                          ]
                                      (if-let [rows (fetch-multi-rows-no-reset control-name ui sr nrs)] 
                                        (response-no-cache (json-format rows))
                                        (json-format-response-no-cache "norow")))
        "multi-select" (let [value ^java.lang.String (second data)
                             startrow ^java.lang.Long (nth data 2)
                             sr (.intValue startrow)
                             numrows ^java.lang.Long (nth data 3)
                             nrs (.intValue numrows)
                             ]
                         (multi-select control-name value sr nrs ui)
                         (json-format-response-no-cache "ok")
                         )
        "add-at-end" (if-let [row (add-at-end control-name ui)] 
                       (response-no-cache (json-format row))
                       (json-format-response-no-cache "norow"))
        "add-at-index" (let [ind (nth data 1)]
                         (if-let [row (add-at-index control-name ui ind)] 
                           (response-no-cache (json-format row))
                           (json-format-response-no-cache "norow"))
                         )
        "delete" (do
                   (delete control-name ui)
                   (json-format-response-no-cache "ok")
                   )
        "undelete" (do
                     (undelete control-name ui)
                     (json-format-response-no-cache "ok")
                     )
        "reset" (do
                  (reset control-name ui)
                  (json-format-response-no-cache "ok"))
        "register-mboset-with-one-mbo" (let [parent-control (nth data 1)
                                             parent-unique-id (nth data 2)]
                                         (response-no-cache (json-format (register-mboset-with-one-mbo control-name parent-control parent-unique-id ui))))
        "register-mboset-with-one-mbo-ind" (let [parent-control (nth data 1)
                                                 parent-index (nth data 2)]
                                             (response-no-cache (json-format (if-let [resp (register-mboset-with-one-mbo-ind control-name parent-control parent-index ui)] resp "empty"))))
        "re-register-mboset-with-one-mbo" (let [parent-control (nth data 1)
                                                parent-index (nth data 2)
                                                ]
                                            (response-no-cache (json-format
                                                                (if-let [resp (re-register-mboset-with-one-mbo control-name parent-control parent-index ui)] resp "empty"))))
        "register-mboset-byrel"   (let [rel-name (nth data 1)
                                        parent-control (nth data 2)
                                        reg-resp (register-mboset-byrel control-name rel-name parent-control ui)
                                        ]
                                    (response-no-cache (json-format reg-resp)))
        "re-register-mboset-byrel"   (let [rel-name (nth data 1)
                                           parent-control (nth data 2)
                                           reg-resp (re-register-mboset-byrel control-name rel-name parent-control ui)
                                           ]
                                       (response-no-cache (json-format reg-resp)))
        "register-mboset-byrel-inline"   (let [rel-name control-name
                                               parent-control (nth data 1)
                                               rownum (nth data 2)
                                               reg-resp (register-mboset-byrel-inline  rel-name parent-control ui rownum)
                                               ]
                                           (response-no-cache (json-format reg-resp)))
        "register-list"            (let [mbocontrol-name (nth data 1)
                                         column-name (nth data 2)
                                         force-qbe? (nth data 3)
                                         reg-resp (register-list control-name mbocontrol-name column-name force-qbe? ui)
                                         ]
                                     (response-no-cache (json-format reg-resp))
                                     )
        "register-qbe-list"        (let [mbocontrol-name (nth data 1)
                                         column-name (nth data 2)
                                         reg-resp (register-qbe-list control-name mbocontrol-name column-name ui)
                                         ]
                                     (response-no-cache (json-format reg-resp))
                                     )
        "register-maximo-menu" (do
                                 (register-maximo-menu control-name ui)
                                 (json-format-response-no-cache "ok")
                                 )
        "get-key-attributes" (if-let [key-attrs (get-key-attributes control-name ui)]
                               (json-format-response-no-cache key-attrs)
                               (json-format-response-no-cache "no key attributes" )
                               )
        "set-value-from-list"  (let [list-name (nth data 1)
                                     column-name (nth data 2)
                                     resp (set-value-from-list control-name list-name column-name ui)]
                                 (json-format-response-no-cache resp))
        "set-qbe-from-list"  (let [list-name (nth data 1)
                                   column-name (nth data 2)]
                               (set-qbe-from-list control-name list-name column-name ui)
                               (json-format-response-no-cache "ok"))
        "smart-fill"      (let [mbocontrol-name (nth data 1)
                                column-name (nth data 2)
                                value (nth data 3)
                                reg-resp (smart-fill control-name mbocontrol-name column-name value ui)
                                ]
                            (response-no-cache (json-format reg-resp)))
        "unregister-control" (do
                               (unregister-the-mboset control-name ui)
                               (json-format-response-no-cache "ok"))
        "move-to" (let [row (nth data 1)]
                    (-> (move-to control-name row ui) json-format response-no-cache))
        "mboset-count" (-> (mboset-count control-name ui) json-format response-no-cache)
        "set-value" (let [attribute (nth data 1)
                          value (nth data 2)
                          resp (set-value control-name attribute value ui)
                          ]
                      (json-format-response-no-cache resp))
        "set-zombie" (let [attribute (nth data 1)
                           value (nth data 2)
                           ]
                       (set-zombie control-name attribute value ui)
                       (json-format-response-no-cache "ok"))
        "save" (do
                 (save control-name ui)
                 (json-format-response-no-cache "ok"))
        "command-on-selection" (do
                                 (command-on-selection control-name (second data) ui)
                                 (json-format-response-no-cache "ok"))
        "get-qbe" (let [qbe (json-format (get-qbe control-name ui))]
                    ( response-no-cache qbe))
        "get-columns-qbe" (let [qbe (json-format (get-columns-qbe control-name (second data) ui))]
                            (response-no-cache qbe))
        "set-current-app" (do
                            (set-current-app control-name (second data)  ui)
                            (json-format-response-no-cache "ok"))
        "set-unique-app" (do
                           (set-unique-app control-name (second data) (nth data 2)  ui)
                           (json-format-response-no-cache "ok"))
        "set-unique-id" (do
                          (set-unique-id control-name (second data) ui)
                          (json-format-response-no-cache "ok"))
        "access-to-option" (do
                             (access-to-option control-name (second data) ui)
                             (json-format-response-no-cache "ok")
                             )
        "run-mbo-command" (do
                            (log/debug "running mbo command")
                            (if (> _len 2)
                              (run-mbo-command control-name (second data) (nth data 2) ui)
                              (run-mbo-command control-name (second data) "null" ui)
                              )
                            (log/debug "finish running the mbo command " (second data))
                            (json-format-response-no-cache "ok"))
        "run-mboset-command" (do
                               (if (> _len 2)
                                 (run-mboset-command control-name (second data) (nth data 2) ui)
                                 (run-mboset-command control-name (second data) "null" ui))
                               (json-format-response-no-cache "ok"))
        "register-mbo-command"     (-> 
                                    (if (> _len 3)
                                      (register-mbo-command control-name (second data) (nth data 2) (nth data 3) ui)
                                      (register-mbo-command control-name (second data) (nth data 2) "null" ui)) 
                                    json-format
                                    response-no-cache)

        "register-mboset-command" (-> 
                                   (if (> _len 3)
                                     (register-mboset-command control-name (second data) (nth data 2) (nth data 3) ui)
                                     (register-mboset-command control-name (second data) (nth data 2)  "null" ui)) 
                                   json-format
                                   response-no-cache)
        "set-order-by" (do
                         (set-order-by control-name (second data) ui)
                         (json-format-response-no-cache "ok"))
        "get-option-descriptions" (-> (get-options control-name ui)
                                      json-format
                                      response-no-cache)
        "initiate-hier" (do
                          (initiate-hier control-name (second data) (nth data 2) (nth data 3) ui)
                          (json-format-response-no-cache "ok")
                          )
        "add-hier-children" (->
                             (add-hier-children control-name (second data) (nth data 2) ui)
                             json-format-response-no-cache
                             )
        "register-wf-director" (let [app-name (second data)
                                     process-name (nth data 2)
                                     director-name (nth data 3)]
                                 (log/debug "calling  wf-director of workflow director (control-name, app-name, process-name director-name): " control-name " " app-name " " process-name " " director-name)
                                 (register-wf-director control-name app-name process-name director-name ui)
                                 (json-format-response-no-cache "ok")
                                 )
        "unregister-wf-director" (let [director-name (first data)]
                                   (unregister-wf-director director-name ui)
                                   (json-format-response-no-cache "ok"))
        "route-wf" (let [app-name (nth data 2)
                         director-name (nth data 3)
                         actionsset-name (first data)
                         control-name (second data)]
                     (-> (route-wf control-name app-name director-name actionsset-name ui)
                         json-format-response-no-cache)
                     )
        "choose-wf-actions" (let [director-name (second data)
                                  actionsset-name (first data)]
                              (-> (choose-wf-action director-name actionsset-name ui)
                                  json-format-response-no-cache))
        "initiate-wf" (let [app-name (nth data 2)
                            director-name (nth data 3)
                            actionsset-name (first data)
                            control-name (second data)
                            ]
                        (-> (initiate-wf control-name app-name director-name actionsset-name ui)
                            json-format-response-no-cache))
        "cancel-wf" (let [director-name (first data)]
                      (cancel-wf director-name ui))
        "is-active-wf" (-> (is-active-workflow control-name ui) json-format-response-no-cache)
        "reassign-wf" (-> (let [director-name (second data)
                                actionsset-name (first data)]
                            (reassign-wf actionsset-name director-name ui)
                            )
                          json-format-response-no-cache)
        "execute-reassign-wf" (-> (let [director-name (second data)
                                        actionsset-name (first data)]
                                    (execute-reassign-wf actionsset-name director-name ui)
                                    )
                                  json-format-response-no-cache)
        "prefetch-wf-for-offline" (-> (let [process-name (second data)]
                                        (prefetch-wf-for-offline control-name process-name ui))
                                      json-format-response-no-cache
                                      )
        "offline-replay-wf" (-> (let [app-name (second data)
                                      process-name (nth data 2)
                                      wf-steps (nth data 3)]
                                  (offline-replay-wf control-name app-name process-name wf-steps ui))
                                json-format-response-no-cache
                                )
        "post-offline-changes" (-> (let [offline-changes (second data)]
                                     (post-offline-changes control-name offline-changes ui)) json-format-response-no-cache)
        "save-offline-changes" (do
                                 (save-offline-changes control-name ui)
                                 (json-format-response-no-cache "ok"))
        "rollback-offline-changes" (do
                                     (rollback-offline-changes control-name ui)
                                     (json-format-response-no-cache "ok"))
        "move-to-uniqueid" (do
                             (move-to-uniqueid control-name (second data) ui)
                             (json-format-response-no-cache "ok"))
        "register-gl-format" (let [glname (first data)
                                   orgid (second data)]
                               (->  (register-gl-format glname orgid ui) json-format-response-no-cache)
                               )
        "register-bookmark-mboset" (let [app (second data)]
                                     (-> (register-bookmark-mboset control-name app ui) json-format-response-no-cache))
        "register-query-mboset" (let [app (second data)]
                                  (-> (register-query-mboset control-name app ui) json-format-response-no-cache))
        "register-inbox-mboset" (-> (register-inbox-mboset control-name ui) json-format-response-no-cache)
        "register-person-mboset" (-> (register-person-mboset control-name ui) json-format-response-no-cache)
        "use-stored-query" (let [query-name (second data)]
                             (use-stored-query control-name query-name ui)
                             (json-format-response-no-cache "ok"))
        "get-gl-segment-count" (let [glname (first data)]
                                 (-> (get-gl-segment-count glname ui)
                                     json-format-response-no-cache))
        "get-gl-segment-info" (let [gl-name (first data)
                                    segment-no (second data)]
                                (-> (get-gl-segment-info gl-name segment-no ui) 
                                    json-format-response-no-cache))
        "set-segment" (let [gl-name (first data)
                            segment-values (second data)
                            segment-no (nth data 2)
                            orgid (nth data 3)]
                        (set-segment gl-name segment-values segment-no orgid ui)
                        (json-format-response-no-cache "ok"))
        "post-yes-no-cancel-input" (let [ex-id (first data)
                                         user-input (second data)]
                                     (post-yes-no-cancel-input ex-id user-input ui)
                                     (json-format-response-no-cache "ok"))
        "remove-yes-no-cancel-input" (let [ex-id (first data)]
                                       (remove-yes-no-cancel-input ex-id  ui)
                                       (json-format-response-no-cache "ok")
                                       )
        "not yet implemented")
      (catch psdi.util.MXApplicationYesNoCancelException ync
        (process-exception ync [:yesnocancel (.getId ync) (.getDisplayMessage ync) (.getErrorGroup ync) (.getErrorKey ync)])
        )
      (catch psdi.util.MXException mex (process-exception mex [:mx (.getDisplayMessage mex) (.getErrorGroup mex) (.getErrorKey mex)] ui))
      (catch java.lang.Exception e 
                                        ;        (debug-exception control-name ui)
        (process-exception e [:general (.getMessage e)] ui))
      (catch java.lang.Error er (do (.printStackTrace er) (process-error er [:general (.getMessage er)]))); becuase aleph is silently swallowing this, we have to notify the user
      )))


(declare get-command-timeout)


(def ^:dynamic *string-encoding* "UTF-8")

(defn t-read [x]
  (let [byts  (.getBytes x)
        in (ByteArrayInputStream. byts)
        rdr (transit/reader in :json)]
    (transit/read rdr)))



(defn send-command
  "for all the commands from the client, like 'register', 'fetch-meta-data', 'set-qbe'....the list is really long, and this is the case for the big dispatch. Dispatch will be very simple and happen on the first parameter(command). The point is, all these commands don't require the value returned from the server, downstream will be the long-polling (with the similar dispatch, but on the client side"
  [{params :params session :session :as req}]
  (log/debug "send-command params " params)
  (if-let [_ss (:max-sess-id session)]
    (let [sess-sym  _ss
          virt-sess (params "t")
          s (@max-sessions sess-sym)
          kommand (params "command")
          ^String dta (params "data")
          dd (t-read dta)
          ui (get-ui-from-session session params)
          _timeout (get-command-timeout)
          ]
      (if ui
        (do
          (log/debug "command " kommand " starts at " (System/currentTimeMillis))
          (swap! max-sessions assoc sess-sym (assoc s :last-accessed-time (assoc (s :last-accessed-time) virt-sess (tm/now))))
          (let [rez (dispatch-c kommand dd ui)]
            (log/debug "command " kommand " ends at " (System/currentTimeMillis))
            rez))
        not-logged-response))
    not-logged-response))

(defn upload-file
  [{params :params session :session :as req}]
  (log/info params)
  (let  [control-name (params "control-name")
         method (params "method")
         ui (get-ui-from-session session params)
         method-class (case method
                        "test" "maximoplus.TestUpload"
                        "doclinks" "maximoplus.DoclinksUpload"
                        (.getProperty @properties method))]
    (log/debug "Calling file upload for " control-name " and " method)
    (if ui
      (try
        (with-mboset control-name ui mboset
          (when-not (nil? @properties)
            (when method-class  
              (let [^maximoplus.Upload uploader  (clojure.lang.Reflector/invokeConstructor (java.lang.Class/forName  method-class) (into-array []))
                    ^maximoplus.BinaryOutput t(.upload uploader  (stringify-keys params) mboset)
                    is (.getInputStream t)
                    content-type (.getContentType t)
                    ret-headers (assoc no-cache-header "Content-Type" content-type)
                    ]
                {:status 200 :body (t-write (slurp is)) :headers ret-headers}
                ))))
        (catch java.lang.Exception e (process-exception e [:general (.getMessage e)] ui))
        (catch java.lang.Error e (process-error e [:general (.getMessage e)])))
      not-logged-response
      )))

(defn serve-binary
  [{params :params session :session :as req}]
  (try
    (log/debug "Serve binary, session:" session)
    (let [control-name (params "control-name")
          method (params "method")
          ui (get-ui-from-session session params)
          method-class (case method
                         "doclinks" "maximoplus.DoclinksDownload"
                         "doclinksredirect" "maximoplus.DoclinksRedirectDownload"
                         (.getProperty @properties method))]
      (log/debug "Calling serve binary for ui " ui)
      (if ui
        (with-mboset control-name ui mboset
          (when-not (nil? @properties)
            (log/debug "Reading properties for" method) 
            (when method-class
              (log/debug "Got the method-class:" method-class)
              (let [^maximoplus.BinaryOutput downloader (clojure.lang.Reflector/invokeConstructor (java.lang.Class/forName  method-class) (into-array []))
                    columns (into-array java.lang.String (@(:control-columns (@session-variables ui)) control-name))
                    ^maximoplus.BinaryOutputT out (.getOutput downloader  (stringify-keys params) mboset columns)
                    is (.getInputStream out)
                    file-name (.getFileName out)
                    content-type (.getContentType out)
                    additional-header (.getAdditionalHttpHeader out)
                    ret-headers (assoc no-cache-header "content-type" content-type)

                    ]
                (log/debug "isDownload:" (.isDownload out))
                (log/debug "content-type:" content-type)
                (log/debug "headers" (if (.isDownload out) (assoc ret-headers "Content-Disposition" (str "attachment; filename=" file-name)) (assoc ret-headers "Content-Disposition" (str "inline; filename=" file-name))))
                {:status 200
                 :body is
                 :headers (if (.isDownload out) (assoc ret-headers "Content-Disposition" (str "attachment; filename=" file-name)) (assoc ret-headers "Content-Disposition" (str "inline; filename=" file-name)))
                 }
                ))))
        not-logged-response))
    (catch java.lang.Exception e (process-exception e [:general (.getMessage e)] ))
    (catch java.lang.Error e (process-error e [:general (.getMessage e)]))
    ))

(defn get-sqlite-file
  [{body :body session :session params :params}]
  (let [ui (get-ui-from-session session params)
        body-content (slurp body)
        input-data (t-read body-content)
        session-id (.getMaxSessionID ui)
        file-name (str session-id ".sqlite")]
    {:status 200
     :body (if ui
             (get-generated-server-file ui input-data file-name)
             "No sesion"
             )
     :headers (if ui
                (assoc no-cache-header "Content-Type" "application/vnd.sqlite3" "Content-Disposition" (str "attachment; filename=" file-name))
                (assoc no-cache-header "Content-Type " "text/plain"))}

    ))

(defroutes rts
  (ANY "/server/sse" [] sse-channel)
  (ANY "/server/longpoll" []  longpoll-batch-channel)
  (ANY "/server/longpoll-batch" [] longpoll-batch-channel)
  (ANY "/server/login" []  web-login)
  (ANY "/server/general-login" [] web-login)
  (ANY "/server/logout" [] logout)
  (ANY "/server/init" [] page-init)
  (ANY "/server/command" [] send-command)
  (ANY "/server/binary-content" [] serve-binary)
  (ANY "/server/upload" [] upload-file)
  (POST "/server/get-sqlite-db" [] get-sqlite-file)
  )

(def ruter
  (-> rts
      (wrap-maxsec)
      (wrap-params)
      (wrap-multipart-params)
      ;;      (wrap-session {:cookie-attrs {:same-site :none }});;todo make this configurable
      (wrap-session)
      (wrap-content-type)
      (wrap-file "public")
      (wrap-file-info)
      (wrap-additional-headers)
      (wrap-add-cors-header)
      (wrap-exception)))


(def ^:dynamic *session-expire* 30)
(def ^:dynamic *longpoll-expire* 2);mutilple  open tabs increase the memory footprint on the server, idle sessions should be terminated, if nothing comes from the long poll, user might have closed the tab or window

(defn- get-expr-gen
  [key minutes]
  (apply concat
         (map (fn [[session-key session-value]]
                (map (fn [x]
                       [session-key (first x)]
                       )
                     (filter (fn [[k v]]
                               (tm/after? (tm/now) (tm/plus  v (tm/minutes minutes))) )
                             (key session-value))))
              @max-sessions
              )))

(defn get-expired-sessions []
  (concat
   (get-expr-gen :last-accessed-time (if-let [sexp (System/getProperty "session.expire") ] (Double/parseDouble  sexp) *session-expire*))
                                        ;(get-expr-gen :last-longpoll-time (if-let [lexp (System/getProperty "longpoll.expire") ] (Double/parseDouble lexp) *longpoll-expire*))
   ))

(defn background-session-clear []
  (while true
    (try 
      @(future
         (loop []
           (do
             (Thread/sleep 60000)
             (let [deb-sess (map (fn[[k v]]
                                   [(:username v)
                                    k (count (map (fn[[kk vv]]
                                                    kk)
                                                  (:last-accessed-time  v)
                                                  ) )])
                                 @max-sessions)]
               (when-not (empty? deb-sess)
                 (log/debug "Live sessions:")
                 (log/debug deb-sess)))
             (log/debug "Background session clear....")
             (doseq [[sess virt-sess]  (get-expired-sessions)]
               (log/debug "Killing session " sess " " virt-sess)
               (kill-session sess virt-sess)))
           (recur)))
      (catch Exception ex
        (log/error "Background session clear exception")
        (log/error ex)
        (Thread/sleep 5000)))))

(def ^String default-port "8080")
(def ^:dynamic *default-version* 7)
(def ^:dynamic *default-command-timeout* 120000)

(defn get-version []
  (if-let [vers (System/getProperty "maximo.version")]
    vers
    *default-version*))

(defn get-port []
  (when-not (System/getProperty "maximoplus.port")
    (System/setProperty "maximoplus.port" default-port))
  (Integer/parseInt (System/getProperty "maximoplus.port")))

(defn get-command-timeout []
  (if-let [_timeout (System/getProperty "command.timeout")]
    (Integer/parseInt _timeout)
    *default-command-timeout*))


(defn get-prop-path [] (System/getProperty "maximo.properties"))

(defn init
  "do the initialization before the http server starts"
  [& prop-path]
  (let [mp-port (get-port)]
    (log/info "Initializig the server with properties:" (first prop-path))
    (log/debug "Full properties thing:" (System/getProperties))
    (if prop-path
      (start-server (get-version) (first prop-path))
      (start-server (get-version)))
    (web/run #'ruter  {:port mp-port :host "0.0.0.0"})) ;if we put out the host immutant listens just on the localhost
  ;;    (web/run (logger/wrap-with-logger #'ruter {:log-fn (fn [{:keys [level throwable message]}]
  ;;                                             (.println (System/out)  message))}) {:port (get-port) :host "0.0.0.0"})
  (start-nrepl-server)
  (background-session-clear))

(defn  -main [& args]
  (if-let [properties (get-prop-path)]
    (init properties)
    (init)
    ))


(defn brissi2
  [vl]
  (loop [vl vl]
        (let [vvals (vals vl)
              parents (set (filter some? (map :parent vvals)))
              all-keys (set (keys vl))
              non-parents (difference all-keys parents)]
          (if (or
               (empty? non-parents)
               (empty? parents))
            vl
            (recur
             (loop [new-val vl non-parents non-parents]
               (if (empty? non-parents)
                 new-val
                 (let [np (first non-parents)
                       npval (get vl np)
                       parent-npval (:parent npval)
                       parent-npval-val (update-in (get vl parent-npval)
                                                   [:children]
                                                   conj
                                                   npval)]
                   (recur
                    (dissoc
                           (assoc new-val parent-npval parent-npval-val)
                           np)
                    (rest non-parents))))))))))

(def tttt {:a {:name :a :value :a :parent nil} 
                            :b {:name :b :value :b :parent :a}
                            :c {:name :c :value :c :parent :b}
                            :d {:name :d :value :d :parent :a}}) ;;(brissi2 tttt)



