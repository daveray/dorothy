(ns user
  (:require cljs.repl.node
            cemerick.piggieback))

(defn cljs-repl
  "Start a node CLJS REPL."
  []
  (cemerick.piggieback/cljs-repl (cljs.repl.node/repl-env)))
