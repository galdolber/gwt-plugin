(ns leiningen.gwt
  "Runs and compiles GWT applications"
  (:import [java.io BufferedReader])
  (:require [clojure.string :as string]))

(def defaults
  {:module nil ;Required
   :war nil    ;Required
   :deploy "target/extra"
   :gen "target/.generated"
   :logLevel "INFO"
   :port "8080"
   :codeServerPort 9997
   :startupUrl "index.html"
   :noserver false
   :extraJvmArgs nil
   :localWorkers 1})

(defn print-stream [out]
  (let [b (clojure.java.io/reader out)]
    (loop [line (.readLine b)]
      (when line
        (do
          (println line)
          (recur (.readLine b)))))))

(defn sh [& args]
  (let [proc (.exec (Runtime/getRuntime)
              ^"[Ljava.lang.String;" (into-array args))]
    (with-open [stdout (.getInputStream proc)
                stderr (.getErrorStream proc)]
      (let [out (future (print-stream stdout))
            err (future (print-stream stderr))
            exit-code (.waitFor proc)]
        {:exit exit-code :out @out :err @err}))))

(defn- classpath [project]
  (string/join
    java.io.File/pathSeparatorChar
    (leiningen.core.classpath/get-classpath project)))

; TODO
;com.google.gwt.dev.codeserver.CodeServer

(defn- addJvmArgs [args extraJvmArgs]
  (if extraJvmArgs
    (let [extra (vec (.split extraJvmArgs " "))]
      (flatten [(first args) extra (next args)]))
    args))

(defn- path [p]
  (.getCanonicalPath (java.io.File. p)))

(defmulti task (fn [_ args] (first args)))

(defmethod task :default [project args]
  (println "options: run, compile. Running...")
  (task project ["run"]))

(defmethod task "run" [project args]
  (let [{:keys [extraJvmArgs deploy gen war logLevel port
                codeServerPort startupUrl noserver module localWorkers]}
         (merge defaults (:gwt project))
        args ["java"
              "-cp" (classpath project)
              "com.google.gwt.dev.DevMode"
              (if noserver "-noserver" nil)
              "-deploy" (path deploy)
              "-gen" (path gen)
              "-war" (path war)
              "-logLevel" logLevel
              "-port" (str port)
              "-codeServerPort" (str codeServerPort)
              "-startupUrl" startupUrl
              module]]
    (apply sh (filter (comp not nil?) (addJvmArgs args extraJvmArgs)))))

(defmethod task "compile" [project args]
  (let [{:keys [extraJvmArgs deploy gen war logLevel port
                codeServerPort startupUrl noserver module localWorkers]}
         (merge defaults (:gwt project))
        args ["java"
              "-cp" (classpath project)
              "com.google.gwt.dev.Compiler"
              "-style" "OBF"
              "-localWorkers" "2"
              "-XfragmentCount" "-1"
              "-deploy" (path deploy)
              "-gen" (path gen)
              "-war" (path war)
              "-logLevel" logLevel
              module]]
    (apply sh (filter (comp not nil?) (addJvmArgs args extraJvmArgs)))))

(defn gwt [project & args]
  (task project args))