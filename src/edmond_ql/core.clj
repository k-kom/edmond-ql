(ns edmond-ql.core
  (:require [integrant.core :as ig]
            [edmond-ql.schema :as schema]
            [edmond-ql.server :as server]
            [edmond-ql.db :as db])
  (:gen-class))

(def config
  {:server/pedestal {:port 8080 :graphql/schema (ig/ref :graphql/schema)}
   :graphql/schema  {:fname "edmond-schema.edn"}
   :data/db         {:book-api {:url-fn (fn [isbn]
                                          (str "https://api.openbd.jp/v1/get?isbn=" isbn))}}})

(defn restart-system [s c]
  (ig/halt! s)
  (ig/init c))

(defn -main
  ""
  []
  (ig/init config))
