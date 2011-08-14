; Copyright (c) Dave Ray, 2011. All rights reserved.

; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v10.html at the root of this
; distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc "A Hiccup-style library for generating graphs with Graphviz.
           The functions you want are (graph), (digraph), (subgraph), (dot),
           (render), and (show). See https://github.com/daveray/dorothy."
      :author "Dave Ray"} 
  dorothy.core
  (:require [clojure.string :as cs]
            [clojure.java.io :as jio]))

(def ^:dynamic *options* {:edge-op "->"})

; id's that don't need quotes
(def ^:private safe-id-pattern #"^[_a-zA-Z\0200-\0377][_a-zA-Z0-9\0200-\0377]*$")

(defn- safe-id? [s] (re-find safe-id-pattern s))
(defn- html? [s] (and (.startsWith s "<") (.endsWith s ">")))
(defn- quote-quotes [s] (cs/replace s "\"" "\\\""))
(defn- escape-id [id]
  (cond
    (keyword? id) (escape-id (name id))
    (string? id)  (cond 
                    (safe-id? id) id
                    (html? id)    id
                    :else         (str \" (quote-quotes id) \"))
    :else         (escape-id (str id))))

(declare to-dottable)

(defprotocol ^{:private true} Dottable 
  (dot* [this]))

(defn statements 
  "Construct a Dottable for a list of statements."
  [ss]
  (reify Dottable
    (dot* [this]
      (apply str (map #(str (dot* %) ";\n") ss)))))

(defn node-id 
  ([id port compass-pt]
    (reify Dottable
      (dot* [this]
        (str (escape-id id) 
            (if port (str ":" (escape-id port)))
            (if compass-pt (str ":" (name compass-pt)))))))
  ([id port]
    (node-id id port nil))
  ([id]
    (node-id id nil nil)))

(defn attr [key val]
  (reify Dottable
    (dot* [this]
      (str (escape-id key) \= (escape-id val)))))

(defn attrs [options]
  (reify Dottable
    (dot* [this]
      (cs/join \, (for [[k v] options] (dot* (attr k v)))))))

(defn- trailing-attrs [attr-map]
  (if-not (empty? attr-map) (str " [" (dot* (attrs attr-map)) "]")))

(defn- x-attrs [type options]
  (reify Dottable
    (dot* [this]
      (str type " [" (dot* (attrs options)) "]"))))

(def graph-attrs (partial x-attrs "graph"))
(def node-attrs  (partial x-attrs "node"))
(def edge-attrs  (partial x-attrs "edge"))

(defn node 
  ([attr-map id]
    (reify Dottable
      (dot* [this]
        (str (dot* id) (trailing-attrs attr-map)))))
  ([id]
    (node {} id)))

(defn edge [attr-map node-ids]
  (reify Dottable
    (dot* [this] 
      (str 
        (cs/join (str " " (:edge-op *options*) " ") (map dot* node-ids)) 
        (trailing-attrs attr-map)))))

(defn- options-for-type [type]
  (condp = type
    ::graph    (assoc *options* :edge-op "--")
    ::digraph  (assoc *options* :edge-op "->")
    ::subgraph *options*))

(defn graph* [opts stmts]
  (let [{:keys [type id strict?] 
         :or   {type ::graph}}     opts]
    (reify Dottable
      (dot* [this]
        (binding [*options* (options-for-type type)] 
          (str (if strict? "strict ") 
               (name type) " "
               (if id (str (escape-id id) " ")) 
               "{\n" (dot* (statements stmts)) "} "))))))

(defn digraph* [opts stmts]  (graph* (assoc opts :type ::digraph) stmts))

(defn subgraph* [opts stmts] (graph* (assoc opts :type ::subgraph) stmts))

(defn- vector-to-dottable-edge [v]
  (let [end (last v)
        attrs? (map? end)
        attrs (if attrs? end {})
        parts (if attrs? (butlast v) v)
        parts (remove #{:>} parts)]
    (edge attrs (map to-dottable parts))))

(defn- vector-to-dottable [[v0 v1 & more :as v]]
  (cond
    (= v0 :graph) (graph-attrs v1)
    (= v0 :node)  (node-attrs  v1)
    (= v0 :edge)  (edge-attrs  v1)
    more          (vector-to-dottable-edge v)
    (map? v1)     (node v1 (to-dottable v0))
    v1            (vector-to-dottable-edge v)
    (map? v0)     (graph-attrs v0)
    v0            (node (to-dottable v0))))

(defn- parse-node-id [v]
  (apply node-id (cs/split v #":")))

(defn- to-dottable [v]
  (cond
    (satisfies? Dottable v) v
    (keyword? v)       (parse-node-id (name v))
    (string?  v)       (parse-node-id v)
    (number?  v)       (parse-node-id (str v))
    (map? v)           (graph-attrs v)
    (vector? v)        (vector-to-dottable v)
    :else              (throw (IllegalArgumentException. (str "Don't know what to do with " v)))))

(defn- desugar-graph-attrs 
  "Turn first arg of (graph) into something usable"
  [attrs]
  (cond
    (map? attrs)     attrs
    (keyword? attrs) {:id attrs}
    (number? attrs)  {:id (str attrs)}
    (string? attrs)  {:id attrs}
    :else            (throw (IllegalArgumentException. (str "Invalid graph arg " attrs)))))

(defn graph 
  "Construct an undirected graph from the given statements which must be a vector.
  See https://github.com/daveray/dorothy or README.md for details of the DSL.
  
  The returned value may be converted to dot language with (dorothy.core/dot)."
  ([handler attrs stmts] (handler (desugar-graph-attrs attrs) (map to-dottable stmts)))
  ([attrs stmts]         (graph graph* attrs stmts))
  ([stmts]               (graph {} stmts)))

(defn digraph 
  "Construct a directed graph from the given statements which must be a vector.
  See https://github.com/daveray/dorothy or README.md for details of the DSL.
  
  The returned value may be converted to dot language with (dorothy.core/dot)."
  ([attrs stmts] (graph digraph* attrs stmts))
  ([stmts]       (digraph {} stmts)))

(defn subgraph 
  "Construct a sub-graph from the given statements which must be a vector.
  See https://github.com/daveray/dorothy or README.md for details of the DSL.
  A subgraph may be used as a statement in a graph, or as a node entry in
  an edge statement.
  
  The returned value may be converted to dot language with (dorothy.core/dot)."
  ([attrs stmts] (graph subgraph* attrs stmts))
  ([stmts]       (subgraph {} stmts)))

(defn dot 
  "Convert the given dorothy graph representation to a string suitable for input to
  the Graphviz dot tool.

  input can either be the result of (graph) or (digraph), or it can be a vector of
  statements (see README.md) in which case (graph) is implied.

  Examples:

    user=> (dot (digraph [[:a :b :c]]))
    \"digraph { a -> b -> c; }\"
 
  See:
    (dorothy.core/render)
    (dorothy.core/show)
  "
  [input]
  (cond
    (satisfies? Dottable input) (dot* input)
    (vector? input)             (dot* (graph input))
    (list?   input)             (dot* (graph input))
    (seq?    input)             (dot* (graph input))
    :else                       (throw (IllegalArgumentException. (str "Invalid (dot) input: " input)))))

(defn- build-render-command [{:keys [format layout scale invert-y?]}]
  (->>
    ["dot"
     (if format    (str "-T" (name format)))
     (if layout    (str "-K" (name layout)))
     (if scale     (str "-s" scale))
     (if invert-y? "-y")]
    (remove nil?)))

(defn- ^java.lang.ProcessBuilder init-process-builder 
  [{:keys [dir] :as options}]
  (let [pb (java.lang.ProcessBuilder. (build-render-command options))]
    (when dir (.directory pb (if (instance? java.io.File dir) dir (java.io.File. (str dir)))))
    pb))

(def ^{:private true} binary-formats
  #{:bmp :eps :gif :ico :jpg :jpeg :pdf :png :ps :ps2 :svgz :tif :tiff :vmlz :wbmp})

;(-> (digraph :G [[:a :b] [:b :c]]) dot (render {}))
(defn- read-dot-result [input-stream {:keys [format binary?]}]
  (if (or binary? (binary-formats format))
    (let [result (java.io.ByteArrayOutputStream.)] 
      (jio/copy input-stream result)
      (.toByteArray result))
    (slurp input-stream)))

(defn render 
  "Render the given graph (must be the string result of (dorothy.core/dot))
  using the Graphviz 'dot' tool. The 'dot' executable must be on the system
  path.

  Depending on the requested format (see options below), returns either a string
  or a Java byte array.
  
  options is a map with the following options:
 
    :dir       The working directory in which dot is executed. Defaults to '.'
    :format    The desired output format, e.g. :png, :svg. If the output format
               is known to be binary, a byte array is returned.
    :layout    Dot layout algorithm to use. (-K command-line option)
    :scale     Input scale, defaults to 72.0. (-s command-line option)
    :invert-y? If true, y coordinates in output are inverted. (-y command-line option)

  Examples:

    ; Simple 3 node graph, converted to dot and rendered as SVG. 
    (-> (digraph [[:a :b :c]]) dot (render {:format :svg))

  See:
    (dorothy.core/dot)
    http://www.graphviz.org/content/command-line-invocation
    http://www.graphviz.org/content/output-formats
  "
  [graph options]
  (let [p        (.start (init-process-builder options))
        from-dot (future (read-dot-result (.getInputStream p) options))]
    (with-open [to-dot (.getOutputStream p)] 
      (spit to-dot graph))
    @from-dot))

(defn save
  "Render and save the given graph (string result of (dorothy.core/dot)) to an
  output stream. f is any argument acceptable to (clojure.java.io/ouput-stream).
  
  Examples:
    
    ; Write a graph to a png file
    (-> (digraph [[:a :b :c]]) 
        dot 
        (save \"out.png\" {:format :png}))

  See:
    (dorothy.core/render)
    (dorothy.core/dot)
    http://clojure.github.com/clojure/clojure.java.io-api.html#clojure.java.io/make-output-stream
"
  [graph f & [options]]
  (let [bytes (render graph (merge options {:binary? true}))]
    (with-open [output (jio/output-stream f)] 
      (jio/copy bytes output)))
  graph)

(defn show 
  "Show the given graph (must be the string result of (dorothy.core/dot)) in a
  new Swing window with scrollbars. Supports same options as 
  (dorothy.core/render) except that :format is ignored.
  
  Examples:
  
    ; Simple 3 node graph, converted to dot and displayed.
    (-> (digraph [[:a :b :c]]) dot show)
 
  Notes:
    
    Closing the resulting frame will not cause the JVM to exit.

  See:
    (dorothy.core/render)
    (dorothy.core/dot)
  "
  [graph & [options]]
  (let [bytes (render graph (merge options {:format :png}))
        icon  (javax.swing.ImageIcon. bytes)
        w     (.getIconWidth icon)
        h     (.getIconHeight icon)
        lbl   (javax.swing.JLabel. icon)
        sp    (javax.swing.JScrollPane. lbl)]
    (doto (javax.swing.JFrame. (format "Dorothy (%dx%d)" w h))
      (.setLocationByPlatform true)
      (.setContentPane sp)
      (.setSize (min 640 (+ w 50)) (min 480 (+ h 50)))
      (.setVisible true))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment (do
  (println (dot* (attrs {:style :filled :color :blue :text "foo\"bar"})))
  (println (dot* (node-id "start" "p" :ne)))
  (println (dot* (node-id "start" "p")))
  (println (dot* (statements [(node-id :start)(node-id "start" "p")])))
  (println (dot* (node (node-id :start))))
  (println (dot* (node {:style :filled :color :blue} (node-id :start) )))
  (println (dot* (edge {} [(node-id :start)(node-id :end)])))
  (println (binding [*options* {:edge-op "--"}] 
    (dot* (edge {:color :grey} [(node-id :start)(node-id :middle :p :_)(node-id :end)]))))
  (println (dot* (graph-attrs {:style :filled})))
  (println (dot* (node-attrs {:style :filled, :color :red})))
  (println (dot* (edge-attrs {:style :filled})))
  (println (dot* (attr :color :lightgrey)))

  (println (dot*
    (graph*
      {:id :G :strict? true}
      [(edge nil [(node-id "start") (node-id :a0)])
      (edge {:color :green} 
            [(node-id :a0) 
            (subgraph* {} [(edge {} [(node-id :a) (node-id :b)])])
            (node-id :a1)])
      (node {:shape :Mdiamond} (node-id :start))])))

  (println (dot*
    (digraph*
      ;{:id :G :strict? true}
      {} 
      [(edge nil [(node-id "start") (node-id :a0)])
      (edge {:color :gre_en :text "hello\"there"} [(node-id :a0) (node-id :a1)])
      (node {:shape :Mdiamond} (node-id :start))])))

  (-> (digraph :G [
    (subgraph :cluster_0 [
      {:style :filled, :color :lightgrey, :label "process #1"}
      [:node {:style :filled, :color :white}]

      [:a0 :> :a1 :> :a2 :> :a3]])

    (subgraph :cluster_1 [
      {:color :blue, :label "process #2"}
      [:node {:style :filled}]

      [:b0 :> :b1 :> :b2 :> :b3]])

    [:start :a0]
    [:start :b0]
    [:a1    :b3]
    [:b2    :a3]
    [:a3    :a0]
    [:a3    :end]
    [:b3    :end]

    [:start {:shape :Mdiamond}]
    [:end   {:shape :Msquare}]])

    dot
    println)
))
