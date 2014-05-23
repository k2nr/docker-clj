# k2nr/docker

Docker remote API library for Clojure.

## Installation

Add this dependency to your `project.clj`

```
[k2nr/docker "0.0.1"]
```

## Usage

k2nr.docker is designed to communicate with [Docker Remote API](http://docs.docker.io/reference/api/docker_remote_api/).

### 1. Create client

All `k2nr/docker` functions require client as its first argument.

```clojure
(def cli (k2nr.docker.core/make-client "127.0.0.1:4243"))
```

### Examples

```clojure
;; Get running containers list
(k2nr.docker.container/list cli)

;; Get all containers list
(k2nr.docker.container/list cli :all true)
```

Get image list

```clojure
;; Get images list
(k2nr.docker.image/list cli)

;; Get all images list
(k2nr.docker.image/list cli :all true)
```

Run container.
This acts like [this](http://docs.docker.io/reference/api/docker_remote_api_v1.11/#31-inside-docker-run).

```clojure
(k2nr.docker.core/run cli "ubuntu")
```

Build image.

```clojure
;; build image named "test" from Dockerfile
(k2nr.docker.image/build-from-file cli "./Dockerfile" :name "test")

;;build from directory
(k2nr.docker.image/build-from-dir cli ".")
```

### stream APIs

Some Docker Remote API returns response as stream.

The below example run the image `ubunt:14.04` and each build logs will be passed as json object to `:stream-fn` (in this example, `println`).

```clojure
(k2nr.docker.core/run cli "ubuntu"
                          :tag "14.04"
                          :cmd ["ls"]
                          :stream true
                          :stream-fn println)
```

Or, build

```clojure
(k2nr.docker.image/build-from-file cli "./Dockerfile"
                                       :stream true
                                       :stream-fn println)
;stdout => {"stream":"Step 0 : FROM ubuntu:14.04\n"}
;stdout => {"stream":" ---\u003e 99ec81b80c55\n"}
;stdout => {"stream":"Step 1 : ENV TEST test\n"}
;stdout => {"stream":" ---\u003e Using cache\n"}
;stdout => {"stream":" ---\u003e edf64f655ac3\n"}
;stdout => {"stream":"Successfully built edf64f655ac3\n"}
;=> nil
```

## License

Copyright © 2014 Kazunori Kajihiro

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
