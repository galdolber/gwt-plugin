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

(defn- javac-options
  "Compile all sources of possible options and add important defaults.
  Result is a String java array of options."
  [project files args]
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

(def subprocess-profile
  {:dependencies [^:displace ['org.clojure/clojure (clojure-version)]]
   :eval-in :subprocess})

(defn- subprocess-form
  "Creates a form for running javac in a subprocess."
  [compile-path files javac-opts show-out]
  (let [out (if show-out nil (java.io.ByteArrayOutputStream.))
        abort (fn [& msg]
                  (.println java.lang.System/err (apply str msg))
                  (System/exit 1))]
     (if-let [compiler (javax.tools.ToolProvider/getSystemJavaCompiler)]
       (do
         (println "Compiling" (count files) "source files")
         (.mkdirs (clojure.java.io/file compile-path))
         (when-not
           (zero? (.run compiler nil out out
                        (into-array java.lang.String javac-opts)))))
       (abort "lein-javac: system java compiler not found; "
               "Be sure to use java from a JDK\nrather than a JRE by"
               " either modifying PATH or setting JAVA_CMD."))))

(defn- run-javac-subprocess
  "Run javac to compile all source files in the project. The compilation is run
  in a subprocess to avoid it from adding the leiningen standalone to the
  classpath, as leiningen adds itself to the classpath through the
  bootclasspath."
  [project args files show-out]
  (let [javac-opts (vec (javac-options project files args))]
    (when (seq files)
      (try
        (binding [eval/*pump-in* false]
          (eval/eval-in
           (project/merge-profiles project [subprocess-profile])
           (subprocess-form (:compile-path project) files javac-opts show-out)))
        (catch Exception e
          (if-let [exit-code (:exit-code (ex-data e))]
            (main/exit exit-code)
            (throw e)))))))

(defn get-files [project]
  (stale-java-sources (:java-source-paths project) (:compile-path project)))

(defn get-uncompiled-generated-files [project]
  (let [compile-path (:compile-path project)]
    (stale-java-sources [compile-path] compile-path)))

(defn javacc
  "Compile Java source files.

  Add a :java-source-paths key to project.clj to specify where to find them.
  Options passed in on the command line as well as options from the :javac-opts
  vector in project.clj will be given to the compiler; e.g. `lein javac -verbose`.

  Like the compile and deps tasks, this should be invoked automatically when
  needed and shouldn't ever need to be run by hand. By default it is called before
  compilation of Clojure source; change :prep-tasks to alter this."
  [project & args]
  (run-javac-subprocess project args (get-files project) false)
  (run-javac-subprocess project args (get-files project) false)
  (run-javac-subprocess project args (get-files project) true)
  (run-javac-subprocess project args (get-uncompiled-generated-files project) true))