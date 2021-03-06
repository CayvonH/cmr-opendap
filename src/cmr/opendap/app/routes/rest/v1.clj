(ns cmr.opendap.app.routes.rest.v1
  "This namespace defines the Version 1 REST routes provided by this service.

  Upon idnetifying a particular request as matching a given route, work is then
  handed off to the relevant request handler function."
  (:require
   [cmr.opendap.app.handler.collection :as collection-handler]
   [cmr.opendap.app.handler.core :as core-handler]
   [cmr.opendap.health :as health]
   [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   REST API Routes   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ous-api
  [httpd-component]
  [["/opendap/ous/collections" {
    :post {:handler collection-handler/batch-generate
          ;; XXX CMR-4864, CMR-4863
          ;;     Protecting collections will be a little different than
          ;;     protecting a single collection, since the concept-id isn't in
          ;;     the path-params. Instead, we'll have to parse the body,
          ;;     extract the concepts ids from that, create an ACL query
          ;;     containing multiple concept ids, and then check those results.
          ;; :permission #{...?}
          }
    :options core-handler/ok}]
   ["/opendap/ous/collection/:concept-id" {
    :get {:handler (collection-handler/generate-urls httpd-component)
          :permissions #{:read}}
    :post {:handler (collection-handler/generate-urls httpd-component)
           :permissions #{:read}}
    :options core-handler/ok}]
   ["/opendap/ous/streaming-collection/:concept-id" {
    :get (collection-handler/stream-urls httpd-component)}]])

(defn admin-api
  [httpd-component]
  [["/opendap/health" {
    :get (core-handler/health httpd-component)
    :options core-handler/ok}]
   ["/opendap/ping" {
    :get {:handler core-handler/ping
          :roles #{:admin}}
    :post {:handler core-handler/ping
           :roles #{:admin}}
    :options core-handler/ok}]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Testing Routes   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def testing
  [["/testing/401" {:get (core-handler/status :unauthorized)}]
   ["/testing/403" {:get (core-handler/status :forbidden)}]
   ["/testing/404" {:get (core-handler/status :not-found)}]
   ["/testing/405" {:get (core-handler/status :method-not-allowed)}]
   ["/testing/500" {:get (core-handler/status :internal-server-error)}]
   ["/testing/503" {:get (core-handler/status :service-unavailable)}]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Assembled Routes   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all
  [httpd-component]
  (concat
   (ous-api httpd-component)
   (admin-api httpd-component)
   testing))
