(defproject revuecinema "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [io.pedestal/pedestal.jetty "0.5.0"]
                 [io.pedestal/pedestal.service "0.5.0"]
                 [org.clojure/core.async "0.2.385"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/tools.reader "1.0.0-beta3"]
                 [com.stuartsierra/component "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.7" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jcl-over-slf4j "1.7.21" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.21" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/log4j-over-slf4j "1.7.21" :exclusions [org.slf4j/slf4j-api]]
                 [enlive "1.1.6"]
                 [prismatic/schema "1.1.3"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :plugins [[info.sunng/lein-bootclasspath-deps "0.2.0"]]
  :boot-dependencies [[org.mortbay.jetty.alpn/alpn-boot "8.1.4.v20150727"]]
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "revuecinema.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.0"]]}
             :uberjar {:aot [revuecinema.server]}}
  :main ^{:skip-aot true} revuecinema.server)
