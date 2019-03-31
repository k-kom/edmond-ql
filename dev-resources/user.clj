(ns user
  (:require [com.walmartlabs.lacinia :as lacinia]
            [clojure.walk :as walk]
            [integrant.core :as ig]
            [edmond-ql.core :as core]
            [qbits.spandex :as s])
  (:import (clojure.lang IPersistentMap)))

(defn simplify
  "Converts all ordered maps nested within the map into standard hash maps, and
   sequences into vectors, which makes ofr easier constants in the tests, and eliminate ordering problems."
  [m]
  (walk/postwalk
    (fn [node]
      (cond
        (instance? IPersistentMap node) (into {} node)
        (seq? node) (vec node)
        :else node))
    m))

(defonce system nil)

(defn q
  [query-string]
  (-> system
      :graphql/schema
      (lacinia/execute query-string nil nil)
      simplify))

(defn start
  []
  (alter-var-root #'system (fn [_] (edmond-ql.core/-main)))
  :started)

(defn stop
  []
  (when (some? system)
    (ig/halt! system)
    (alter-var-root #'system (constantly nil)))
  :stopped)

(comment
  (start)
  (stop))
