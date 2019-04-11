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
   ^String full-text
   ^Integer stock-count
   ^PersistentVector borrowing-user])

(def raw-book->texts
  (juxt (fn [raw-book] (map :Text (get-in raw-book [:onix :CollateralDetail :TextContent])))
        (fn [raw-book] (get-in raw-book [:summary :title]))
        (fn [raw-book] (get-in raw-book [:summary :publisher]))
        (fn [raw-book] (get-in raw-book [:summary :author]))))

(defn full-text [raw-book]
  (clojure.string/replace
    (clojure.string/join "," (flatten (raw-book->texts raw-book)))
    #"\n"
    ","))

(defn default-meta
  "Book :edmond-meta の初期値を返します。"
  []
  {:stock-count 0
   :borrowing-user []})

(defn fetch-new-book
  "isbn の情報を opendb から取得して結果の body を hash-map で返します。"
  [isbn]
  (-> isbn
      book-url
      slurp
      future
      deref
      (json/read-str :key-fn keyword)))

(defn str->number [v]
  (if (empty? v) 0 (BigInteger/valueOf (Long/parseLong v))))


(defn raw-book->book
  "opendb の結果を受け取って Book を返します。"
  [raw-book]
  (-> raw-book
      (get :summary)
      (assoc :full-text (full-text raw-book))
      (update :volume str->number)
      (update :series str->number)
      (update :isbn str->number)
      (merge (default-meta))
      map->Book))

(defn isbn->book [isbn]
  (let [[raw-book] (fetch-new-book isbn)]
    (if (nil? raw-book)
      nil
      (raw-book->book raw-book))))

(defn search-req [q]
  {:url    [:test_book :_doc :_search]
   :body   {:query q}
   :_source [:isbn :title :volume :series :publisher :cover :author :stock-count]
   :method :get})

(defn fetch-book-by-isbn [isbn]
  (try
    (s/request es-client (search-req {:match {:isbn isbn}}))
    (catch Exception e
      (println (Throwable->map e)))))

(defn fetch-book-by-full-text [text]
  (try
    (s/request es-client (search-req {:match {:full-text text}}))
    (catch Exception e
      (println (Throwable->map e)))))

(defn books-by-text [text]
  "elasticsearch を full text search します。"
  (let [books (fetch-book-by-full-text text)]
    (->> (get-in books [:body :hits :hits])
         (map :_source))))

(defn books-by-isbn [isbn]
  (let [books (fetch-book-by-isbn isbn)]
    (-> (get-in books [:body :hits :hits])
        first
        :_source)))

(defn inc-stock
  "elasticsearch の book doc の在庫を1増やします。"
  [book]
  (update book :stock-count inc))

(defn save-book
  "book で elasticsearch に新規登録します"
  [client book]
  (s/request client {:url    [:test_book :_doc]
                     :method :post
                     :body   book}))

(defn book->schema-book [book]
  (-> book
      (assoc :stock_count (:stock-count book))
      (dissoc :stock-count)))

(defn register-book
  "isbn で elasticsearch を検索し (未実装)、 見つからない場合は新規登録します。見つかった場合は何もせずそれを返します。
   結果として schema Book として扱える hash map を返します。"
  [isbn]
  (println "started to fetch isbn:" isbn)
  (if-let [registered-book (books-by-isbn isbn)]
    registered-book
    (when-let [book (some-> isbn
                            isbn->book
                            inc-stock)]
      (println "got api response: " book)
      (try
        (save-book es-client book)
        (catch Exception e
          (println (Throwable->map e))))
      (book->schema-book book))))


