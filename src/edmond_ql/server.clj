(ns edmond-ql.server
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.cors :refer [allow-origin]]
            [com.walmartlabs.lacinia.pedestal :refer [service-map]]
            [integrant.core :as ig]))

(defn start-server [schema _]
  (-> schema
      (service-map {:graphiql true})
      (merge {:creds                 true
              ::http/allowed-origins (constantly true)})
      http/create-server
      http/start))

(defmethod ig/init-key :server/pedestal [_ {:keys [graphql/schema] :as opts}]
  (start-server schema opts))

(defmethod ig/halt-key! :server/pedestal [_ server]
  (http/stop server))
