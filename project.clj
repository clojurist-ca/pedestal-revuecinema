(defproject revuecinema "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[ca.clojurist/revuecinema "0.1.0-SNAPSHOT"]
                 [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]
                 [com.cognitect/transit-clj "0.8.271"]
                 [com.stuartsierra/component "0.2.3"]
                 [io.pedestal/pedestal.jetty "0.4.0"]
                 [io.pedestal/pedestal.service "0.4.0"]
                 [org.clojure/clojure "1.7.0-beta2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.typed "0.2.87"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/test.check "0.7.0"]
                 [org.slf4j/jcl-over-slf4j "1.7.12" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.12" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/log4j-over-slf4j "1.7.12" :exclusions [org.slf4j/slf4j-api]]
                 [prismatic/schema "0.4.2"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "revuecinema.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.4.0"]]}
             :uberjar {:aot [revuecinema.server]}}
  :main ^{:skip-aot true} revuecinema.server)
