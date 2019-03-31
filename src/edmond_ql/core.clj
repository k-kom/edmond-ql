(ns edmond-ql.core
  (:require [integrant.core :as ig]
            [environ.core :refer [env]]
            [edmond-ql.schema :as schema]
            [edmond-ql.server :as server]
            [edmond-ql.db :as db])
  (:gen-class))

(def default-config {:book-api-url      "https://api.openbd.jp/v1/get?isbn="
                     :elasticsearch-url "http://localhost:9200"
                     :schema-file       "edmond-schema.edn"
                     :port              8080})

(defn config [c]
  (let [{:keys [schema-file port] :as c} (merge c default-config)]
    {:server/pedestal (merge {:port port} {:graphql/schema (ig/ref :graphql/schema)})
     :graphql/schema  {:fname schema-file}
     :data/db         (select-keys c [:book-api-url :elasticsearch-url])}))

(defn restart-system [s c]
  (ig/halt! s)
  (ig/init c))

(defn -main
  ""
  []
  (ig/init (config env)))
