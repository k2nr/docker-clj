(ns k2nr.docker.client
  (:require [clj-http.client :as http]
            [slingshot.slingshot :refer [throw+ try+]]))

(defprotocol DockerClient
  (url [this path]))

(defprotocol RESTClient
  (get    [this path opts])
  (delete [this path opts])
  (post   [this path opts]))

(defn- make-opts [opts]
  (merge {:throw-entire-message? true}
         opts))

(def methods {:get    http/get
              :post   http/post
              :delete http/delete})

(def handlable-errors {400 {:type ::bad-parameter}
                       404 {:type ::not-found}
                       406 {:type ::not-acceptable}
                       409 {:type ::conflict}
                       500 {:type ::server-error}})

(defn error-info [resp]
  (merge (-> resp (:status) (handlable-errors))
         {:response resp}))

(defn- request [url method opts]
  (try+
   (let [method-fn (method methods)
         response (method-fn url (make-opts opts))]
     (:body response))
   (catch #(-> % (:status) (handlable-errors)) response
     (throw+ (error-info response)))))

(defrecord Client [host]
  DockerClient
  (url [this path] (str "http://" host path))
  RESTClient
  (get    [this path opts] (request (url this path) :get    opts))
  (delete [this path opts] (request (url this path) :delete opts))
  (post   [this path opts] (request (url this path) :post   opts)))

(defn make-client
  ([] (make-client "localhost:4243"))
  ([host] (Client. host)))
