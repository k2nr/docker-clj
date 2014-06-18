(ns k2nr.docker.container
  (:require [clojure.string :as str]
            [k2nr.docker.client :as client]
            [k2nr.docker.utils :refer :all]
            [cheshire.core :as json]
            [camel-snake-kebab :refer[->kebab-case ->CamelCase]])
  (:refer-clojure :exclude (list remove)))

(defn- path [& strs]
  (apply str "/containers/" strs))

(defn- ->port-bindings [bindings]
  (reduce (fn [m [host container]]
            (update-in m [(str container "/tcp")]
                       (fnil #(conj % {"HostPort" (str host)}) [])))
          {} bindings))

(defn- ->env [envs]
  (mapv (fn [[k v]] (str k "=" v)) envs))

(defn- ->exposed-ports [ports]
  (into {} (map #(vector (str % "/tcp") {}) ports)))

(defn- ->volumes [vs]
  (into {} (map #(vector % {}) vs)))

(defn- update-if [m k f & args]
  (if (k m)
    (apply update-in m [k] f args)
    m))

(defn ->host-config [config]
  (-> config
      (update-if :port-bindings ->port-bindings)
      (update-if :env ->env)
      (update-if :exposed-ports ->exposed-ports)
      (update-if :volumes ->volumes)
      (#(map-keys ->CamelCase %))))

(defn create-from-config [cli host-config & {:keys [name]}]
  (map-keys ->kebab-case
            (client/post cli (path "create")
                         {:content-type :json
                          :query-params {:name name}
                          :as :json
                          :body (json/generate-string
                                 (map-keys ->CamelCase host-config))})))

(defn create [cli image & {:keys [; host config
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
                                  ; query parameters
                                  name] :as config}]
  (let [host-config (-> config
                        (assoc :image image)
                        (dissoc :name)
                        (->host-config))]
    (create-from-config cli host-config :name name)))

(defn start
  ([cli container & {:keys [; host config
                            binds
                            lxc-conf
                            port-bindings
                            publish-all-ports
                            privileged] :as config}]
     (let [host-config (->host-config config)]
       (client/post cli (path container "/start")
                    {:content-type :json
                     :as :json
                     :body (json/generate-string host-config)}))))

(defn attach [cli container & {:keys [logs stream stdin stdout stderr stream-fn]}]
  (let [resp (client/post cli (path container "/attach")
                     {:query-params {:logs logs
                                     :stream stream
                                     :stdin  stdin
                                     :stdout stdout
                                     :stderr stderr}
                      :as :stream})]
    (if stream
      (raw-stream-fetcher resp stream-fn)
      (raw-stream->seq resp))))

(defn logs [cli container & {:keys [follow stdout stderr timestamps stream-fn]}]
  (let [resp (client/get cli (path container "/logs")
                         {:query-params {:follow follow
                                         :stdout stdout
                                         :stderr stderr
                                         :timestamps timestamps}
                          :as :stream})]
    (if follow
      (raw-stream-fetcher resp stream-fn)
      (raw-stream->seq resp))))

(defn list [cli & {:keys [all limit since before size]}]
  (client/get cli (path "json")
              {:query-params {:all all
                              :limit limit
                              :since since
                              :before before
                              :size size}
               :as :json}))

(defn inspect [cli container]
  (client/get cli (path container "/json")
              {:as :json}))

(defn stop [cli container & {:keys [time]}]
  (client/post cli (path container "/stop")
               {:query-params {:t time}}))

(defn kill [cli container & {:keys [signal]}]
  (client/post cli (path container "/kill")
               {:query-params {:signal signal}}))

(defn remove [cli container & {:keys [remove-volumes force]}]
  (client/delete cli (path container)
                 {:query-params {:v remove-volumes
                                 :force force}}))

(defn top [cli container & {:keys [ps-args]}]
  (client/get cli (path container "/top")
              {:query-params {:ps_args ps-args}
               :as :json}))

(defn changes [cli container]
  (client/get cli (path container "/changes")
               {:as :json}))

(defn wait [cli container]
  (client/post cli (path container "/wait")
               {:as :json}))

(defn ports [cli container-id]
  (let [container (inspect cli container-id)
        bindings (-> container :HostConfig :PortBindings)]
    (reduce (fn [m [k v]]
              (let [private-port (->> (str/replace (str k) #"^:" "")
                                      (re-seq #"([0-9]+)/tcp")
                                      first
                                      second
                                      Integer.)
                    hosts (map (fn [h] {:ip (:HostIp h)
                                        :port (Integer. (:HostPort h))})
                               v)]
                (merge m {private-port hosts})))
            {}
            bindings)))

(defn port [cli container-id private-port]
  (let [ps (ports cli container-id)]
    (first (get ps private-port))))
