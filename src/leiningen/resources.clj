(ns leiningen.resources
  "Copy all resources to classes output."
  (:require [clojure.java.io :as io]))

(defn relativize [base file]
  (.getPath
   (.relativize (.toURI base) (.toURI file))))

(defn copy-recursive-into
  "Recursively copy the files in src to dest."
  [src dest]
  (let [f (io/file src)]
    (when (.exists f)
      (doseq [file (remove #(.isDirectory %) (file-seq f))]
        (let [dest-file (io/file dest (relativize f file))]
          (.mkdirs (.getParentFile dest-file))
          (io/copy file dest-file))))))

(defn- copy-resources [project args]
  (if-let [resources (:resource-paths project)]
    (doseq [r resources]
      (copy-recursive-into r (:compile-path project))))
  (if-let [resources (:web-resource-paths project)]
    (doseq [r resources]
      (copy-recursive-into r (:web-path project)))))

(defn resources
  [project & args]
  (copy-resources project args))