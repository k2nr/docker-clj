(ns k2nr.docker.image
  (:require [k2nr.docker.client :as client]
            [cheshire.core :as json]
            [k2nr.docker.tar :refer [create-archive with-open-tar add-entry]]
            [clojure.java.io :refer [file input-stream delete-file]])
  (:refer-clojure :exclude (list remove)))

(defn- path [& strs]
  (apply str "/images/" strs))

(defn build-from-stream [cli body-stream & {:keys [name quiet no-cache stream]}]
  (client/post cli "/build"
               {:headers {"Content-Type" "application/tar"}
                :query-params {:t name
                               :q quiet
                               :nocache no-cache}
                :body body-stream
                :as (if stream :stream :json)}))

(defn build-from-dir [cli path & opts]
  (let [tar (create-archive path)
        response (apply build-from-stream cli (input-stream tar) opts)]
    (delete-file tar)
    response))

(defn build-from-file [cli path & opts]
  (let [docker-file (file path)
        tar (with-open-tar
              "dockerfile.tar.gz"
              (fn [tar]
                (add-entry tar docker-file "Dockerfile")))
        response (apply build-from-stream cli (input-stream tar) opts)]
    (delete-file tar)
    response))

(defn list [cli & {:keys [all]}]
  (client/get cli (path "json")
              {:query-params {:all all}
               :as :json}))

(defn create [cli name & {:keys [repo tag registry stream]}]
  (client/post cli (path "create")
               {:query-params {:fromImage name
                               :repo      repo
                               :tag       tag
                               :registry  registry}
                :as (if stream :stream :json)}))

(defn push [cli name & {:keys [registry stream]}]
  (client/post cli (path name "/push")
               {:query-params {:registry registry}
                :as (if stream :stream :json)}))

(defn tag [cli name & {:keys [repo force]}]
  (client/post cli (path name "/tag")
               {:query-params {:repo repo
                               :force force}}))

(defn remove [cli name & {:keys [force noprune]}]
  (client/delete cli (path name)
                 {:query-params {:force force
                                 :noprune noprune}
                  :as :json}))

(defn search [cli term]
  (client/get cli (path "search")
              {:query-params {:term term}
               :as :json}))

(defn history [cli name]
  (client/get cli (path name "/history")
              {:as :json}))
