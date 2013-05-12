(ns leiningen.javaauto
  "Compile Java source files when they change."
  (:require [leiningen.javacc :as javacc]))

(defn javaauto [project & args]
  (while true
    (javacc/jcompile project args)
    (Thread/sleep 1000)))