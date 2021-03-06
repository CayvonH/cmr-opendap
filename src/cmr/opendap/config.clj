(ns cmr.opendap.config
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [cmr.opendap.util :as util]
   [environ.core :as environ])
  (:import
    (clojure.lang Keyword)))

(def config-file "config/cmr-opendap/config.edn")

(defn cfg-data
  ([]
    (cfg-data config-file))
  ([filename]
    (with-open [rdr (io/reader (io/resource filename))]
      (edn/read (new java.io.PushbackReader rdr)))))

(defn cmr-only
  [[k v]]
  (let [key-name (name k)]
    (when (string/starts-with? key-name "cmr-")
      [(mapv keyword (string/split key-name #"-"))
       (try
        (Integer/parseInt v)
        (catch Exception _e
          v))])))

(defn nest-vars
  [acc [ks v]]
  (assoc-in acc ks v))

(defn env-props-data
  []
  (->> (#'environ/read-system-props)
       (util/deep-merge (#'environ/read-system-env))
       (map cmr-only)
       (remove nil?)
       (reduce nest-vars {})))

(defn data
  []
  (util/deep-merge (cfg-data)
                   (env-props-data)))

(defn service-keys
  "We need to special-case two-word services, as split by the environment and
  system property parser above.

  Note: this function originally had more in it, but was moved into cmr.authz."
  [^Keyword service]
  [service])

(defn service->base-url
  [service]
  (format "%s://%s:%s"
          (or (:protocol service) "https")
          (:host service)
          (or (:port service) "443")))

(defn service->url
  [service]
  (format "%s%s"
          (service->base-url service)
          (or (get-in service [:relative :root :url])
              (:context service)
              "/")))

(defn service->base-public-url
  [service]
  (let [protocol (or (get-in service [:public :protocol]) "https")
        host (get-in service [:public :host])]
    (if (= "https" protocol)
      (format "%s://%s" protocol host)
      (format "%s://%s:%s" protocol host (get-in service [:public :port])))))

(defn service->public-url
  [service]
  (format "%s%s"
          (service->base-public-url service)
          (or (get-in service [:relative :root :url])
              (:context service)
              "/")))
