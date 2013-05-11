(ns leiningen.javacc
  "Compile Java source files."
  (:require [leiningen.classpath :as classpath]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import java.io.File
           javax.tools.ToolProvider))

(defn- stale-java-sources [dirs compile-path]
  (for [dir dirs
        ^File source (filter #(-> ^File % (.getName) (.endsWith ".java"))
                             (file-seq (io/file dir)))
        :let [rel-source (.substring (.getPath source) (inc (count dir)))
              rel-compiled (.replaceFirst rel-source "\\.java$" ".class")
              compiled (io/file compile-path rel-compiled)]
        :when (>= (.lastModified source) (.lastModified compiled))]
    (.getPath source)))

(defn- javac-options [project files args]
  (let [options-file (File/createTempFile ".leiningen-cmdline" nil)]
    (with-open [options-file (io/writer options-file)]
      (doto options-file
        (.write (format "-cp %s\n" (classpath/get-classpath-string project)))
        (.write (format "-d %s\n" (:compile-path project)))
        (.write (string/join "\n" files))))
    (into-array String
                (concat (:javac-options project)
                        args
                        [(str "@" options-file)]))))

(defn- subprocess-form [compile-path files javac-opts]
  (let [out (java.io.ByteArrayOutputStream.)
        abort (fn [& msg]
                  (.println java.lang.System/err (apply str msg))
                  (System/exit 1))]
     (if-let [compiler (javax.tools.ToolProvider/getSystemJavaCompiler)]
       (do
         (.run compiler nil out out (into-array java.lang.String javac-opts))
         (str out))
       (abort "lein-javac: system java compiler not found; "
               "Be sure to use java from a JDK\nrather than a JRE by"
               " either modifying PATH or setting JAVA_CMD."))))

(defn- run-javac-subprocess [project args files]
  (when (seq files)
    (try
      (subprocess-form (:compile-path project) files (vec (javac-options project files args)))
      (catch Exception e
        (if-let [exit-code (:exit-code (ex-data e))]
          (main/exit exit-code)
          (throw e))))))

(defn get-files [project]
  (stale-java-sources (:java-source-paths project) (:compile-path project)))

(defn get-uncompiled-generated-files [project]
  (let [compile-path (:compile-path project)]
    (stale-java-sources [compile-path] compile-path)))

(defn jcompile [project args files]
  (println "Compiling" (count files) "source files")
  (let [out (run-javac-subprocess project args files)
        new-files (get-files project)]
    (cond
     (empty? new-files) true
     (= files new-files) (println out)
     :else (jcompile project args new-files))))

(defn javacc [project & args]
  (.mkdirs (clojure.java.io/file (:compile-path project)))
  (when (jcompile project args (get-files project))
    (run-javac-subprocess project args
                          (get-uncompiled-generated-files project))))