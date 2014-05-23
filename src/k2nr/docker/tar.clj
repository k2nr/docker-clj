(ns k2nr.docker.tar
  (:require [clojure.java.io :refer [file output-stream copy input-stream]]
            [clojure.string :refer [replace-first]])
  (:import (java.util UUID)
           (java.util.zip GZIPInputStream GZIPOutputStream)
           (org.apache.commons.compress.archivers.tar TarArchiveInputStream
                                                      TarArchiveOutputStream
                                                      TarArchiveEntry)
           (org.apache.commons.io FilenameUtils)))

(defn- tar-output-stream [path]
  (-> path
      (output-stream)
      (GZIPOutputStream.)
      (TarArchiveOutputStream.)))

(defn- entry-path
  ([file] (entry-path file ""))
  ([file root]
     (replace-first (.getPath file)
                    (re-pattern (str "^" root "/"))
                    "")))

(defn- path-exists? [p]
  (.exists (file p)))

(defn add-entry [out in-file entry-path]
  (let [entry (TarArchiveEntry. entry-path)]
    (.setSize entry (.length in-file))
    (.putArchiveEntry out entry)
    (copy (input-stream in-file) out)
    (.closeArchiveEntry out)))

(defn with-open-tar [tar-path f]
  (with-open [out (tar-output-stream tar-path)]
    (f out)
    (.finish out))
  (file tar-path))

(defn create-tar-from-dir
  ([tar-path dir] (create-tar-from-dir tar-path dir ""))
  ([tar-path dir tar-root]
     (with-open-tar tar-path (fn [tar]
                               (doseq [in (file-seq (file dir))
                                       :when (.isFile in)]
                                 (let [path (entry-path in tar-root)]
                                   (add-entry tar in path)))))))

(defn create-archive
  [dir & [tmpdir]]
  (let [archive-name (str (UUID/randomUUID) ".tar.gz")
        out-path (str (if (and tmpdir (path-exists? tmpdir))
                        tmpdir
                        (System/getProperty "java.io.tmpdir"))
                      "/" archive-name)]
    (create-tar-from-dir out-path dir)))
