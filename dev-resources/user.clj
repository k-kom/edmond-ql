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

(comment "elasticsearch experiment"
         (http/get "http://localhost:9200/bank/_search" {:headers {"Content-Type" "application/json"}
                                                         :body (clojure.data.json/write-str {:query {:match_all {}}
                                                                                             :sort [{:account_number :asc}]})}
                   (fn [{:keys [status headers body error]}] ;; asynchronous response handling
                     (if error
                       (println "Failed, exception is " error)
                       (do
                         (println "Async HTTP GET: " status)
                         (println
                           (map #(-> %1 :_source (select-keys [:firstname :lastname]))
                                (get-in (clojure.data.json/read-str body :key-fn keyword) [:hits :hits]))))))))

;;(comment "localhost:9200/bank/_search?pretty" -H 'Content-Type: application/json' -d'
;;         {
;;          "query": { "match_all": {} },
;;                 "sort": [
;;                          { "account_number": "asc" }
;;                          ]
;;          })