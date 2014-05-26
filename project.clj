(defproject k2nr/docker "0.0.2-SNAPSHOT"
  :description "Docker Remote API client library for Clojure"
  :url "https://github.com/k2nr/docker-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :exclusions [org.clojure/clojure]
  :dependencies [[cheshire "5.3.1"]
                 [clj-http "0.9.1"]
                 [slingshot "0.10.3"]
                 [org.apache.commons/commons-compress "1.8.1"]
                 [org.apache.commons/commons-io "1.3.2"]
                 [camel-snake-kebab "0.1.5"]
                 ]
  :plugins [[codox "0.8.7"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [midje "1.6.3"]]}})
