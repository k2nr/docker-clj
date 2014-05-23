(defproject k2nr.docker "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.3.1"]
                 [clj-http "0.9.1"]
                 [slingshot "0.10.3"]
                 [org.apache.commons/commons-compress "1.8.1"]
                 [org.apache.commons/commons-io "1.3.2"]
                 [camel-snake-kebab "0.1.5"]
                 ]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}})
