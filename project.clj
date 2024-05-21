(defproject cassidy "0.2.7"
  :description "A SHON read and write library."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [hiccup "1.0.5"]
                 [clojure-saxon "0.9.4"]]
  :repositories [["snapshots" {:url "https://gitlab.standyck.com/api/v4/projects/51/packages/maven"
                               :username "Private-Token"
                               :password :env/GITLAB_TOKEN
                               :sign-releases false}]
                 ["releases" {:url "https://gitlab.standyck.com/api/v4/projects/51/packages/maven"
                              :username "Private-Token"
                              :password :env/GITLAB_TOKEN
                              :sign-releases false}]]
  :main ^:skip-aot cassidy.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
