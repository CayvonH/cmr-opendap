(ns cmr.opendap.auth.roles
  "Roles for CMR OPeNDAP are utilized in the application routes when it is
  necessary to limit access to resources based on the role of a user.

  Roles are included in the route definition along with the route's handler.
  For example:
  ```
  [...
   [\"my/route\" {
    :get {:handler my-handlers/my-route
          :roles #{:admin}}
    :post ...}]
   ...]"
  (:require
   [clojure.set :as set]
   [cmr.opendap.auth.acls :as acls]
   [cmr.opendap.components.caching :as caching]
   [cmr.opendap.components.config :as config]
   [reitit.ring :as ring]
   [taoensso.timbre :as log]))

(def management-acl
  "The canonical ingest management ACL definition."
  :INGEST_MANAGEMENT_ACL)

(def echo-management-query
  "The query formatter used when making a roles query to the CMR Access Control
  API. Note that only the management ACL is currently supported, and that this
  maps below to `admin`."
  {:system_object (name management-acl)})

(defn admin-key
  "Generate a key to be used for caching role data."
  [token]
  (str "admin:" token))

(defn cmr-acl->reitit-acl
  [cmr-acl]
  (if (seq (management-acl cmr-acl))
    #{:admin}
    #{}))

(defn route-annotation
  "Extract any roles annotated in the route associated with the given request."
  [request]
  (get-in (ring/get-match request) [:data :get :roles]))

(defn admin
  "Query the CMR Access Control API to get the roles for the given token+user."
  [base-url token user-id]
  (let [perms @(acls/check-access base-url
                                  token
                                  user-id
                                  echo-management-query)]
    (log/debug "Got permissions:" perms)
    (cmr-acl->reitit-acl perms)))

(defn cached-admin
  "Look up the roles for token+user in the cache; if there is a miss, make the
  actual call for the lookup."
  [system token user-id]
  (caching/lookup system
                  (admin-key token)
                  #(admin (config/get-access-control-url system)
                          token
                          user-id)))

(defn admin?
  "Check to see if the roles of a given token+user match the required roles for
  the route."
  [system route-roles token user-id]
  (seq (set/intersection (cached-admin system token user-id)
                         route-roles)))
