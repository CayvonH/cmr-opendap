(ns cmr.opendap.http.request
  (:require
   [cmr.opendap.components.config :as config]
   [cmr.opendap.const :as const]
   [org.httpkit.client :as httpc]
   [taoensso.timbre :as log])
  (:refer-clojure :exclude [get]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Header Support   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-header
  [req field]
  (get-in req [:headers field]))

(defn add-header
  ([field value]
    (add-header {} field value))
  ([req field value]
    (assoc-in req [:headers field] value)))

(defn add-accept
  ([value]
    (add-accept {} value))
  ([req value]
    (add-header req "Accept" value)))

(defn add-token-header
  ([token]
    (add-token-header {} token))
  ([req token]
    (add-header req "Echo-Token" token)))

(defn add-user-agent
  ([]
    (add-user-agent {}))
  ([req]
    (add-header req "User-Agent" const/user-agent)))

(defn add-content-type
  ([ct]
    (add-content-type {}))
  ([req ct]
    (add-header req "Content-Type" ct)))

(defn add-form-ct
  ([]
    (add-form-ct {}))
  ([req]
    (add-content-type req "application/x-www-form-urlencoded")))

(defn add-client-id
  ([]
    (add-client-id {}))
  ([req]
    (add-header req "Client-Id" const/client-id)))

(defn add-payload
  ([data]
    (add-payload {} data))
  ([req data]
    (assoc req :body data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   HTTP Client Support   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-options
  {:user-agent const/user-agent
   :insecure? true})

(defn options
  [req & opts]
  (apply assoc (concat [req] opts)))

(defn request
  [method url req & [callback]]
  (httpc/request (-> default-options
                     (add-client-id)
                     (add-user-agent)
                     (merge req)
                     (assoc :url url :method method)
                     ((fn [x] (log/trace "Options to httpc:" x) x)))
                  callback))

(defn async-get
  ([url]
    (async-get url {}))
  ([url req]
    (async-get url req nil))
  ([url req callback]
    (request :get url req callback)))

(defn async-post
  ([url]
    (async-post url {:body nil}))
  ([url req]
    (async-post url req nil))
  ([url req callback]
    (request :post url req callback)))

(defn get
  [& args]
  @(apply async-get args))

(defn post
  [& args]
  @(apply async-post args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Accept Header/Version Support   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def accept-pattern
  "The regular expression for the `Accept` header that may include version
  and parameter information splits into the following groups:
  * type: everything before the first '/' (slash)
  * subtype: everything after the first '/'

  The subtype is then further broken down into the following groups:
  * vendor
  * version (with and without the '.'
  * content-type (with and without the '+' as well as the case where no
    vendor is supplied))

  All other groups are unused."
  (re-pattern "(.+)/((vnd\\.([^.+]+)(\\.(v[0-9]+))?(\\+(.+))?)|(.+))"))

(def accept-pattern-keys
  [:all
   :type
   :subtype
   :vendor+version+content-type
   :vendor
   :.version
   :version
   :+content-type
   :content-type
   :no-vendor-content-type])

(defn default-accept
  [system]
  (format "application/vnd.%s%s+%s"
          (config/vendor system)
          (config/api-version-dotted system)
          (config/default-content-type system)))

(defn parse-accept
  [system req]
  (->> (or (get-in req [:headers :accept])
           (get-in req [:headers "accept"])
           (get-in req [:headers "Accept"])
           (default-accept system))
       (re-find accept-pattern)
       (zipmap accept-pattern-keys)))

(defn accept-api-version
  [system req]
  (let [parsed (parse-accept system req)
        version (or (:version parsed) (config/api-version system))]
    version))

(defn accept-media-type
  [system req]
  (let [parsed (parse-accept system req)
        vendor (or (:vendor parsed) (config/vendor system))
        version (or (:.version parsed) (config/api-version-dotted system))]
    (str vendor version)))

(defn accept-format
  [system req]
  (let [parsed (parse-accept system req)]
    (or (:content-type parsed)
        (:no-vendor-content-type parsed)
        (config/default-content-type system))))
