(ns cmr.opendap.http.response
  "This namespace defines a default set of transform functions suitable for use
  in presenting results to HTTP clients.

  Note that ring-based middleeware may take advantage of these functions either
  by single use or composition."
  (:require
   [cheshire.core :as json]
   [clojure.data.xml :as xml]
   [clojure.string :as string]
   [cmr.authz.errors :as authz-errors]
   [cmr.http.kit.response :as response]
   [cmr.opendap.errors :as errors]
   [ring.util.http-response :as ring-response]
   [taoensso.timbre :as log]
   [xml-in.core :as xml-in])
  (:refer-clojure :exclude [error-handler]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Backwards-compatible Aliases   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def parse-json-body response/parse-json-body)
(def json-errors response/json-errors)
(def parse-xml-body response/parse-xml-body)
(def xml-errors response/xml-errors)
(def ok response/ok)
(def not-found response/not-found)
(def cors response/cors)
(def add-header response/add-header)
(def version-media-type response/version-media-type)
(def errors response/errors)
(def error response/error)
(def not-allowed response/not-allowed)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Custom Response Functions   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn error-handler
  [status headers body]
  (response/error-handler status headers body (format errors/status-code status)))

(defn client-handler
  ([response]
    (client-handler response identity))
  ([response parse-fn]
    (response/client-handler response error-handler parse-fn)))

(def json-handler #(client-handler % response/parse-json-body))

(defn process-ok-results
  [data]
  {:headers {"CMR-Took" (:took data)
             "CMR-Hits" (:hits data)}
   :status 200})

(defn process-err-results
  [data]
  (cond (authz-errors/any-errors? data)
        {:status authz-errors/error-code}

        (errors/any-server-errors? data)
        {:status errors/server-error-code}

        (errors/any-client-errors? data)
        {:status errors/client-error-code}

        :else
        {:status errors/default-error-code}))

(defn process-results
  [data]
  (if (:errors data)
    (process-err-results data)
    (process-ok-results data)))

(defn json
  [_request data]
  (log/trace "Got data for JSON:" data)
  (-> data
      process-results
      (assoc :body (json/generate-string data))
      (ring-response/content-type "application/json")))

(defn text
  [_request data]
  (-> data
      process-results
      (assoc :body data)
      (ring-response/content-type "text/plain")))

(defn html
  [_request data]
  (-> data
      process-results
      (assoc :body data)
      (ring-response/content-type "text/html")))
