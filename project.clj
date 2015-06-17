(defproject cassidy "0.2.2"
  :description "A SHON read and write library."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [clojure-saxon "0.9.4"]]
  :main ^:skip-aot cassidy.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
