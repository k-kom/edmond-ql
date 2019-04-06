(ns edmond-ql.schema
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [integrant.core :as ig]
            [edmond-ql.db :as db]))

(defn book-by-text [_ {:keys [text]} _]
  (db/books-by-text text))

(defn register-book [_ {:keys [isbn]} _]
  (println "about to register the book: " isbn)
  (db/register-book (BigInteger/valueOf isbn)))

(defn resolver-map []
  {:query/book-by-id       (fn [_ _ _])
   :query/book-by-text     book-by-text
   :mutation/register-book register-book})

(defn ^:private edmond-schema
  [fname]
  (-> fname
      io/resource
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))

(defmethod ig/init-key :graphql/schema [_ {:keys [fname]}]
  (edmond-schema fname))

(defmethod ig/halt-key! :graphql/schema [_ _])
