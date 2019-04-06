(defproject edmond-ql "0.1.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/data.json "0.2.6"]
                 [com.walmartlabs/lacinia-pedestal "0.11.0"]
                 [integrant "0.7.0"]
                 [cc.qbits/spandex "0.5.2"]
                 [environ "1.1.0"]]
  :main edmond-ql.core
  :profiles {:uberjar {:aot :all}})
