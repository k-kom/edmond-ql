(ns edmond-ql.core
  (:require [integrant.core :as ig]
            [edmond-ql.schema :as schema]
            [edmond-ql.server :as server])
  (:gen-class))

(def config
  {:server/pedestal {:port 8080 :graphql/schema (ig/ref :graphql/schema)}
   :graphql/schema {:fname "edmond-schema.edn"}})

(defn -main
  ""
  []
  (ig/init config))
