(ns leiningen.javaauto
  "Compile Java source files when they change."
  (:use leiningen.javacc
        util.watchtower))

(defn javaauto [project & args]
  (watcher ["src/"]
    (rate 50)
    (file-filter ignore-dotfiles)
    (file-filter (extensions :java))
    (on-change (fn [_] (run-javac-subprocess project args true))))
  (while true
    (Thread/sleep 1000)))