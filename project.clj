(defproject grep4ik "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [uap-clj "1.2.2"]
                 [amazonica "0.3.77"]
                 [org.clojure/core.async "0.2.395"]]
  :main ^:skip-aot grep4ik.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
