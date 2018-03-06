(defproject dorothy "0.0.7-SNAPSHOT"
  :description "Hiccup-style generation of Graphviz graphs"
  :url "https://github.com/daveray/dorothy"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles
  {:dev {:dependencies [[org.clojure/clojurescript "1.9.908"]
                        [com.cemerick/piggieback "0.2.2"]]

         :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

         :plugins [[lein-cljsbuild "1.1.7" :exclusions [org.clojure/clojure]]
                   [lein-doo "0.1.7"]]

         :aliases {"cljs-test" ["doo" "node" "node-test"]
                   "test-all" ["do" "clean," "test" ":all," "cljs-test" "once"]}

         :doo {:build "node-test"}

         :cljsbuild
         {:builds
          {"node-test" {:source-paths ["src" "test"]
                        :compiler {:output-to "target/test.js"
                                   :output-dir "target/out-node-test"
                                   :main dorothy.test.doo-runner
                                   :optimizations :advanced
                                   :pretty-print false
                                   :target :nodejs
                                   :language-in :ecmascript5}}}}}})
