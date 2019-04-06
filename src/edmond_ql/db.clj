(ns edmond-ql.db
  (:require [integrant.core :as ig]
            [clojure.data.json :as json]
            [qbits.spandex :as s])
  (:import (clojure.lang PersistentVector)))

(comment This namespace implements infrastructure layer like postgresql.
         schema namespace depends on this namespace to retrieve data from physical database.)

;; config の面倒をみる関数
(defonce book-api-url nil)

(defonce es-client nil)

(defn book-url [isbn]
  (str book-api-url isbn))

(defmethod ig/init-key :data/db [_ {:keys [book-api-url elasticsearch-url]}]
  (alter-var-root #'book-api-url (constantly book-api-url))
  (alter-var-root #'es-client (constantly (s/client {:hosts [elasticsearch-url]}))))

(defmethod ig/halt-key! :data/db [_ _]
  (alter-var-root #'book-api-url (constantly nil))
  (alter-var-root #'es-client (constantly nil)))

;; db に保存する時のスキーマ
(defrecord Book
  [^String isbn
   ^String title
   ^Integer volume
   ^Integer series
   ^String publisher
   ^String pubdate
   ^String cover
   ^String author
   ^Integer stock-count
   ^PersistentVector borrowing-user])

(defn default-meta
  "Book :edmond-meta の初期値を返します。"
  []
  {:stock-count 0
   :borrowing-user []})

(defn fetch-new-book
  "isbn の情報を opendb から取得して結果を future で返します。"
  [isbn]
  (future (slurp (book-url isbn))))

(defn str->number [v]
  (if (empty? v) 0 (BigInteger/valueOf (Float/parseFloat v))))

(defn raw-book->book
  "api の結果を受け取って Book を返します。"
  [raw-book]
  (-> raw-book
      (get :summary)
      (assoc :freeword (clojure.string/join "," (map :Text (get-in raw-book [:onix :CollateralDetail :TextContent]))))
      (update :volume str->number)
      (update :series str->number)
      (update :isbn str->number)
      (merge (default-meta))
      map->Book))

(defn isbn->book [isbn]
  (let [[raw-book] (-> isbn
                       fetch-new-book
                       deref
                       (json/read-str :key-fn keyword))]
    (raw-book->book raw-book)))

;; TODO: ...
(defn fetch-book
  "elasticsearch から isbn を元に本を取得します。みつからない場合は nil を返します。"
  [isbn])

(defn inc-stock
  "elasticsearch の book doc の在庫を1増やします。"
  [book]
  (update book :stock-count inc))

(defn update-book
  "book で elasticsearch を update します"
  [client book]
  (s/request client {:url    [:test_book :_doc]
                     :method :post
                     :body   book}))

(defn book->schema-book [book]
  (-> book
      (assoc :stock_count (:stock-count book))
      (dissoc :stock-count)))

(defn register-book
  "isbn で elasticsearch を検索し (未実装)、 見つからない場合は新規登録します。見つかった場合は在庫を1増やします。
   結果として schema Book として扱える hash map を返します。"
  [isbn]
  (println "started to fetch isbn:" isbn)
  (let [book (-> isbn
                 isbn->book
                 inc-stock)]
    (update-book es-client book)
    (book->schema-book book)))

(defn books-by-title [title]
  (s/request es-client {:url [:test_book :_doc]
                        :method :get}))
