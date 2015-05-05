(ns revuecinema.fetch
  (:require
   [io.pedestal.interceptor.helpers :as interceptor])
  (:require
   [ca.clojurist.revuecinema :as rc]))

;; NB: uses the new (as of Pedestal 0.4.0) Interceptor API for better
;; AOT compilation support.
;;
;; Cf. https://github.com/pedestal/pedestal/pull/301

(def movies
  (interceptor/on-request
   ::movies
   (fn [request]
     (let [movie-seq (rc/convert rc/base-url)]
       (assoc request :movies movie-seq)))))
