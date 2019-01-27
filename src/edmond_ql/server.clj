(ns edmond-ql.server
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia.pedestal :refer [service-map]]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [io.pedestal.http :as http])
  (:gen-class))

(defn resolver-map []
  {:query/book-by-id (fn [_ _ _])
   :query/book-by-title (fn [_ _ _])})


(defn ^:private edmond-schema
  []
  (-> "edmond-schema.edn"
      io/resource
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))

(defn graphql-service-map []
  (-> (edmond-schema)
      (service-map {:graphiql true})))

(defn start-server []
  (-> (graphql-service-map)
      http/create-server
      http/start))

(defn -main
  ""
  [_]
  (start-server))

(defonce servlet  (atom nil))

(defn servlet-init
  [_ config]
  ;; Initialize your app here.
  (reset! servlet  (http/servlet-init (graphql-service-map) nil)))

(defn servlet-service
  [_ request response]
  (http/servlet-service @servlet request response))

(defn servlet-destroy
  [_]
  (http/servlet-destroy @servlet)
  (reset! servlet nil))

