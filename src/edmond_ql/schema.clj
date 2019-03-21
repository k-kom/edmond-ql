(ns edmond-ql.schema
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [integrant.core :as ig]
            [edmond-ql.db :as db]))

(def books
  (atom []))

(defn book-by-title [_ {:keys [title]} _]
  (let [r (re-pattern title)]
    (filter #(re-find r (:title %1)) @books)))

(defn register-book [_ {:keys [isbn]} _]
  (let [b (db/register-book (BigInteger/valueOf isbn))]
    (swap! books conj b)
    (println "about to save the book: " b)
    b))

(defn resolver-map []
  {:query/book-by-id       (fn [_ _ _])
   :query/book-by-title    book-by-title
   :query/books-by-user    (fn [_ _ _])
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

(defmethod ig/halt-key! :graphql/schema [_ _]
  (reset! books []))
