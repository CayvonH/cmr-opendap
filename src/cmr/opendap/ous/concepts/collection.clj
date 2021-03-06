(ns cmr.opendap.ous.concepts.collection
  (:require
   [clojure.string :as string]
   [cmr.opendap.components.config :as config]
   [cmr.opendap.http.request :as request]
   [cmr.opendap.http.response :as response]
   [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Collection API   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-query
  [concept-id]
  (str "concept_id=" concept-id))

(defn async-get-metadata
  "Given a data structure with :collection-id, get the metadata for the
  associated collection."
  [search-endpoint user-token params]
  (let [concept-id (:collection-id params)
        url (str search-endpoint
                 "/collections?"
                 (build-query concept-id))]
    (log/debug "Collection query to CMR:" url)
    (request/async-get
     url
     (-> {}
         (request/add-token-header user-token)
         (request/add-accept "application/json"))
     response/json-handler)))

(defn extract-metadata
  [promise]
  (let [results @promise]
    (log/trace "Got results from CMR granule collection:" results)
    (first (get-in results [:feed :entry]))))

(defn get-metadata
  [search-endpoint user-token params]
  (let [promise (async-get-metadata search-endpoint user-token params)]
    (extract-metadata promise)))

(defn extract-variable-ids
  [entry]
  (sort (get-in entry [:associations :variables])))

(defn extract-service-ids
  [entry]
  (sort (get-in entry [:associations :services])))


