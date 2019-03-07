(ns edmond-ql.schema
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia.pedestal :refer [service-map]]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [integrant.core :as ig]))

(def initial-books
  [{:title     "My friend Edmond has been lived in Japan."
    :borrowed  false
    :isbn      "9784873117119"
    :volume    2
    :series    1
    :publisher "You"
    :cover     "http://books.google.com/books/content?id=GCokrgEACAAJ&printsec=frontcover&img=1&zoom=5&source=gbs_api"
    :author    "Otani"}])

(defn resolver-map []
  {:query/book-by-id (fn [_ _ _])
   :query/book-by-title (fn [_ _ _] (first initial-books))
   :query/books-by-user (fn [_ _ _])})

(defn ^:private edmond-schema
  [fname]
  (-> fname
      io/resource
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))

(defn graphql-service-map [fname]
  (-> (edmond-schema fname)
      (service-map {:graphiql true})))

(defmethod ig/init-key :graphql/schema [_ {:keys [fname]}]
  (graphql-service-map fname))

(defmethod ig/halt-key! :graphql/schema [_ _])
