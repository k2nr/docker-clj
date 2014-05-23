(ns k2nr.docker.core
  (:require [k2nr.docker.client :as client]
            [k2nr.docker.container :as container]
            [k2nr.docker.utils :as utils]
            [k2nr.docker.image :as image]
            [slingshot.slingshot :refer [try+]]
            [camel-snake-kebab :refer[->kebab-case ->CamelCase]]))

(def make-client client/make-client)

(defn ping [cli]
  (client/get cli "/_ping" {}))

(defn version [cli]
  (client/get cli "/version" {:as :json}))

(defn info [cli]
  (client/get cli "/info" {:as :json}))

(defn run [cli image & {:keys [; create host config
                               hostname
                               domainname
                               exposed-ports
                               user
                               tty
                               open-stdin
                               stdin-once
                               memory
                               attach-stdin
                               attach-stdout
                               attach-stderr
                               env
                               cmd
                               dns
                               volumes
                               volumes-from
                               network-disabled
                               entrypoint
                               cpu-shares
                               working-dir
                               memory-swap
                               ; start host config
                               binds
                               lxc-conf
                               port-bindings
                               publish-all-ports
                               privileged
                               ; query parameters
                               name
                               tag
                               repo
                               registry
                               ; function options
                               stream
                               stream-fn
                               ] :as config}]
  (let [host-config (-> config
                        (assoc :image (str image (when tag (str ":" tag))))
                        (dissoc :name :tag :repo :registry :stream :stream-fn))
        container-id (:id (try+
                           (container/create-from-config cli host-config :name name)
                           (catch #(= (:type %) ::k2nr.docker.client/not-found) _
                             (let [resp (image/create cli image
                                                      :tag      tag
                                                      :repo     repo
                                                      :registry registry
                                                      :stream   stream)]
                               (when stream
                                 (utils/json-stream-fetcher resp stream-fn)))
                             (container/create-from-config cli host-config :name name))))]
    (container/start cli container-id
                     :binds             binds
                     :lxc-conf          lxc-conf
                     :port-bindigs      port-bindings
                     :publish-all-ports publish-all-ports
                     :privileged        privileged)
    container-id))

(defn commit [cli container & {:keys [repo tag message author]}]
  (utils/map-keys ->kebab-case
                  (client/post cli "/commit"
                               {:query-params {:container container
                                               :repo      repo
                                               :tag       tag
                                               :m         message
                                               :author    author}
                                :as :json})))

(defn events [cli & {:keys [since until stream stream-fn]}]
  (let [resp (client/get cli "/events"
                         {:query-params {:since since
                                         :until until}
                          :as :stream})]
    (if stream
      (utils/json-stream-fetcher resp stream-fn)
      resp)))
