(ns edmond-ql.db
  (:require [integrant.core :as ig]
            [clojure.data.json :as json]
            [qbits.spandex :as s]))

(comment This namespace implements infrastructure layer like postgresql.
         schema namespace depends on this namespace to retrieve data from physical database.)

;; config の面倒をみる関数
(defonce book-api-url nil)

(defonce elasticsearch-url nil)

(defn book-url [isbn]
  (str book-api-url isbn))

(defmethod ig/init-key :data/db [_ {:keys [book-api-url elasticsearch-url]}]
  (alter-var-root #'book-api-url (constantly book-api-url))
  (alter-var-root #'elasticsearch-url (constantly elasticsearch-url)))

(defmethod ig/halt-key! :data/db [_ _]
  (alter-var-root #'book-api-url (constantly nil))
  (alter-var-root #'elasticsearch-url (constantly nil)))

;; config の詳細は知らないロジック
(defn fetch-book [isbn]
  (future (slurp (book-url isbn))))

(defn str->number [v]
  (if (empty? v) 0 (BigInteger/valueOf (Float/parseFloat v))))

(defn extract-book [book]
  (-> book
      first
      (get :summary)
      (update :volume str->number)
      (update :series str->number)
      (update :isbn str->number)))

;; TODO: こいつは永続化の役割を果たさないといけないけど今その機能がここにないので何もしてません
(defn register-book [isbn]
  (println "started to fetch isbn:" isbn)
  (let [book-future (fetch-book isbn)]
    (-> @book-future
        (json/read-str :key-fn keyword)
        extract-book)))
