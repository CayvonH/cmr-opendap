(ns cmr.opendap.ous.granule
  (:require
   [clojure.string :as string]
   [cmr.opendap.components.config :as config]
   [cmr.opendap.const :as const]
   [cmr.opendap.errors :as errors]
   [cmr.opendap.http.request :as request]
   [cmr.opendap.http.response :as response]
   [cmr.opendap.ous.query.results :as results]
   [cmr.opendap.ous.util :as ous-util]
   [cmr.opendap.util :as util]
   [ring.util.codec :as codec]
   [taoensso.timbre :as log]))

(defn build-include
  [gran-ids]
  (string/join
   "&"
   (conj
    (map #(str (codec/url-encode "concept_id[]")
               "="
               %)
         gran-ids)
    (str "page_size=" (count gran-ids)))))

(defn build-exclude
  [component gran-ids]
  (string/join
   "&"
   (conj
    (map #(str (codec/url-encode "exclude[echo_granule_id][]")
               "="
               %)
         gran-ids)
    ;; We don't know how many granule ids will be involved in an exclude,
    ;; so we use CMR's max page size.
    (str "page_size=" (config/cmr-max-pagesize component)))))

(defn build-query
  "Build the query string for querying granles, bassed upon the options
  passed in the parameters."
  [component params]
  (let [coll-id (:collection-id params)
        gran-ids (util/remove-empty (:granules params))
        exclude? (:exclude-granules params)
        bounding-box (:bounding-box params)
        temporal (:temporal params)]
    (str "collection_concept_id=" coll-id
         (when (seq gran-ids)
          (str "&"
               (if exclude?
                 (build-exclude component gran-ids)
                 (build-include gran-ids))))
         (when (seq bounding-box)
          (str "&bounding_box="
               (ous-util/seq->str bounding-box)))
         (when (seq temporal)
          (str "&"
               (ous-util/temporal-seq->cmr-query temporal))))))

(defn async-get-metadata
  "Given a data structure with :collection-id, :granules, and :exclude-granules
  keys, get the metadata for the desired granules.

  Which granule metadata is returned depends upon the values of :granules and
  :exclude-granules"
  [component search-endpoint user-token params]
  (let [url (str search-endpoint "/granules")
        payload (build-query component params)]
    (log/debug "Granules query CMR URL:" url)
    (log/debug "Granules query CMR payload:" payload)
    (request/async-post
     url
     (-> {}
         (request/add-token-header user-token)
         (request/add-accept "application/json")
         (request/add-form-ct)
         (request/add-payload payload))
     response/json-handler)))

(defn extract-metadata
  [promise]
  (let [rslts @promise]
    (if (errors/erred? rslts)
      (do
        (log/error errors/granule-metadata)
        rslts)
      (do
        (log/debug "Got results from CMR granule search:"
                   (results/elided rslts))
        (log/trace "Remaining results:" (results/remaining-items rslts))
        (get-in rslts [:feed :entry])))))

(defn get-metadata
  [component search-endpoint user-token params]
  (let [promise (async-get-metadata component search-endpoint user-token params)]
    (extract-metadata promise)))

;; XXX The following may need to change once CMR-4912 is addressed ...
(defn match-datafile-link
  "The criteria defined in the prototype was to iterate through the links,
  only examining those links that were not 'inherited', and find the one
  whose :rel value matched a particular string.

  It is currently unclear what the best criteria for this decision is."
  [link-data]
  (let [rel (:rel link-data)]
    (and (not (:inherited link-data))
              (= const/datafile-link-rel rel))))

(defn extract-datafile-link
  [granule-entry]
  (let [link (->> (:links granule-entry)
                  (filter match-datafile-link)
                  first)]
    {:granule-id (:id granule-entry)
     :link-rel (:rel link)
     :link-href (:href link)}))
