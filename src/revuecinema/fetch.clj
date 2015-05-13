(ns revuecinema.fetch
  (:require
   [io.pedestal.interceptor.helpers :as interceptor
    :refer [defhandler defon-request defbefore definterceptor handler]])
  (:require
   [ca.clojurist.revuecinema :as rc]))

(defon-request movies
  [request]
  (let [movie-seq (rc/convert rc/base-url)]
    (assoc request :movies movie-seq)))
