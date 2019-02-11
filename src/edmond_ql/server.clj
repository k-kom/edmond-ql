(ns edmond-ql.server
  (:require [io.pedestal.http :as http]
            [integrant.core :as ig]))

(defn start-server [schema _]
  (-> schema
      http/create-server
      http/start))

(defmethod ig/init-key :server/pedestal [_ {:keys [graphql/schema] :as opts}]
  (start-server schema opts))

(defmethod ig/halt-key! :server/pedestal [_ server]
  (http/stop server))
