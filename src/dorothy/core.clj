; Copyright (c) Dave Ray, 2014. All rights reserved.

; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v10.html at the root of this
; distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

;; # dorothy
;;
;; [Hiccup-style](https://github.com/weavejester/hiccup) generation of [Graphviz](http://www.graphviz.org/) graphs in Clojure.
;;
;; *Dorothy is extremely alpha and subject to radical change. [Release Notes Here](https://github.com/daveray/dorothy/wiki)*
;;
;; ## Usage
;;
;; *Dorothy assumes you have an understanding of Graphviz and DOT. The text below describes the mechanics of Dorothy's DSL, but you'll need to refer to the Graphviz documentation for specifics on node shapes, valid attributes, etc.*
;;
;; *The Graphviz `dot` tool executable must be on the system path*
;;
;; Dorothy is on Clojars. In Leiningen:
;;
;;     [dorothy "x.y.z"]
;;
;; A graph consists of a vector of *statements*. The following sections describe the format for all the types of statements. If you're bored, skip ahead to the "Defining Graphs" section below.
;;
;; ### Node Statement
;; A *node statement* defines a node in the graph. It can take two forms:
;;
;;     node-id
;;
;;     [node-id]
;;
;;     [node-id { attr map }]
;;
;; where `node-id` is a string, number or keyword with optional trailing *port* and *compass-point*. Here are some node statement examples:
;;
;;     :node0          ; Define a node called "node0"
;;
;;     :node0:port0    ; Define a node called "node0" with port "port0"
;;
;;     :node0:port0:sw ; Similarly a node with southwest compass point
;;
;; the node's attr map is a map of attributes for the node. For example,
;;
;;     [:start {:shape :Mdiamond}]
;;     ; => start [shape=Mdiamond];
;;
;; Dorothy will correctly escape and quote node-ids as required by dot.
;;
;; A node id can also be auto-generated with `(gen-id object)`.
;;
;;     [(gen-id some-object) {:label (.getText some-object)}]
;;
;; It allows you to use arbitrary objects as nodes.
;;
;; ### Edge Statement
;; An *edge statement* defines an edge in the graph. It is expressed as a vector with two or more node-ids followed optional attribute map:
;;
;;     [node-id0 node-id1 ... node-idN { attr map }]
;;     ; => "node-id0" -> "node-id1" -> ... -> "node-idN" [attrs ...];
;;
;; In addition to node ids, an edge statement may also contain subgraphs:
;;
;;     [:start (subgraph [... subgraph statements ...])]
;;
;; For readability, `:>` delimiters may be optionally included in an edge statement:
;;
;;     [:start :> :middle :> :end]
;;
;; ### Graph Attribute Statement
;;
;; A *graph attribute* statement sets graph-wide attributes. It is expressed as a single map:
;;
;;     {:label "process #1", :style :filled, :color :lightgrey}
;;     ; => graph [label="process #1",style=filled,color=lightgrey];
;;
;; alternatively, this can be expressed with the `(graph-attrs)` function like this:
;;
;;     (graph-attrs {:label "process #1", :style :filled, :color :lightgrey})
;;     ; => graph [label="process #1",style=filled,color=lightgrey];
;;
;; ### Node and Edge Attribute Statement
;; A *node attribute* or *edge attribute* statement sets node or edge attributes respectively for all nodes and edge statements that follow. It is expressed with `(node-attrs)` and `(edge-attrs)` statements:
;;
;;     (node-attrs {:style :filled, :color :white})
;;     ; => node [style=filled,color=white];
;;
;; or:
;;
;;     (edge-attrs {:color :black})
;;     ; => edge [color=black];
;;
;;
;; ## Defining Graphs
;; As mentioned above, a graph consists of a series of statements. These statements are passed to the `graph`, `digraph`, or `subgraph` functions. Each takes an optional set of attributes followed by a vector of statements:
;;
;;
;;     ; From http://www.graphviz.org/content/cluster
;;     (digraph [
;;       (subgraph :cluster_0 [
;;         {:style :filled, :color :lightgrey, :label "process #1"}
;;         (node-attrs {:style :filled, :color :white})
;;
;;         [:a0 :> :a1 :> :a2 :> :a3]])
;;
;;       (subgraph :cluster_1 [
;;         {:color :blue, :label "process #2"}
;;         (node-attrs {:style :filled})
;;
;;         [:b0 :> :b1 :> :b2 :> :b3]])
;;
;;       [:start :a0]
;;       [:start :b0]
;;       [:a1    :b3]
;;       [:b2    :a3]
;;       [:a3    :a0]
;;       [:a3    :end]
;;       [:b3    :end]
;;
;;       [:start {:shape :Mdiamond}]
;;       [:end   {:shape :Msquare}]])
;;
;; ![Sample](https://github.com/downloads/daveray/dorothy/dorothy-show2.png)
;;
;; Similarly for `(graph)` (undirected graph) and `(subgraph)`. A second form of these functions takes an initial option map, or a string or keyword id for the graph:
;;
;;     (graph :graph-id ...)
;;     ; => graph "graph-id" { ... }
;;
;;     (digraph { :id :G :strict? true } ...)
;;     ; => strict graph G { ... }
;;
;; ## Generate Graphviz dot format and rendering images
;;
;; Given a graph built with the functions described above, use the `(dot)` function to generate Graphviz DOT output.
;;
;;     (use 'dorothy.core)
;;     (def g (graph [ ... ]))
;;     (dot g)
;;     "graph { ... }"
;;
;; Once you have DOT language output, you can render it as an image using the `(render)` function:
;;
;;     ; This produces a png as an array of bytes
;;     (render graph {:format :png})
;;
;;     ; This produces an SVG string
;;     (render graph {:format :svg})
;;
;;     ; A one-liner with a very simple 4 node digraph.
;;     (-> (digraph [ [:a :b :c] [:b :d] ]) dot (render {:format :svg}))
;;
;; *The dot tool executable must be on the system path*
;;
;; other formats include `:pdf`, `:gif`, etc. The result will be either a java byte array, or String depending on whether the format is binary or not. `(render)` returns a string or a byte array depending on whether the output format is binary or not.
;;
;; Alternatively, use the `(save!)` function to write to a file or output stream.
;;
;;     ; A one-liner with a very simple 4 node digraph
;;     (-> (digraph [ [:a :b :c] [:b :d] ]) dot (save! "out.png" {:format :png}))
;;
;; Finally, for simple tests, use the `(show!)` function to view the result in a simple Swing viewer:
;;
;;     ; This opens a simple Swing viewer with the graph
;;     (show! graph)
;;
;;     ; A one-liner with a very simple 4 node digraph
;;     (-> (digraph [ [:a :b :c] [:b :d] ]) dot show!)
;;
;; which shows:
;;
;; ![Sample](https://github.com/downloads/daveray/dorothy/dorothy-show.png)
;;
;;
;; ## License
;;
;; Copyright (C) 2014 Dave Ray
;;
;; Distributed under the Eclipse Public License, the same as Clojure.

(ns dorothy.core
  {:doc "A Hiccup-style library for generating graphs with Graphviz.
           The functions you want are (graph), (digraph), (subgraph), (dot),
           (render), (save!) and (show!). See https://github.com/daveray/dorothy."
      :author "Dave Ray"}
  (:require [clojure.string :as cs]
            [clojure.java.io :as jio]))

(set! *warn-on-reflection* true)

;; ----------------------------------------------------------------------
;; # Utilities
;;
;; You know.

(defn ^:private error
  [fmt & args]
  (throw (RuntimeException. ^String (apply format fmt args))))

;; ----------------------------------------------------------------------
;; # Id Generation

(defn gen-id
  "Node ids are expected to be keywords or strings. Sometimes you have an object
  graph where the nodes don't have obvious keyword or string ids. Pass the object
  to (gen-id) and a consisten unique id will be generated for the object when the
  graph is generated.

  Notes:
    Assume the return value of this function is opaque. The impl will change.

  See:
    (dorothy.core/gen-id?)
  "
  [target]
  (constantly target))

(defn gen-id?
  "Returns true if the target was created with (dorothy.core/gen-id)"
  [target] (fn? target)) ; hrmmm.

(defn ^:private id-generator []
  (let [id-map (atom {})]
    (fn [target]
      (if-let [id (get @id-map target)]
        id
        (let [id (str (gensym))]
          (swap! id-map assoc target id)
          id)))))

;; ----------------------------------------------------------------------
;; # Graphviz DOT AST
;;
;; Dorothy represents the unrendered graph with an Abstract Syntax Tree (AST).
;; Each node in the tree is a map with a `:type` key and other keys that vary
;; based on the node type.

(declare to-ast)

(defn is-ast?
  "Returns true if v is an AST node, i.e. has :type. The second form
  checks for a particular type.

  Examples:

    (is-ast? {:type ::node})
    ;=> true

    (is-ast? {:type ::node} ::node)
    ;=> true
  "
  ([v] (and (map? v) (contains? v :type)))
  ([v type]
   (and (is-ast? v)
        (if (set? type)
          (type (:type v))
          (= type (:type v))))))

(defn ^:private check-ast [v type]
  (if-not (is-ast? v type)
    (error "Expected AST node of type %s" type)))


(def ^:private compass-pts #{"n" "ne" "e" "se" "s" "sw" "w" "nw" "c" "_"})
(defn ^:private check-compass-pt [pt]
  (if (or (nil? pt) (compass-pts (name pt)))
    pt
    (error "Invalid compass point %s" pt)))

(defn node-id
  "Create a node-id. Creates an AST node with :type ::node-id

  Examples:

    (node-id :foo)
    ;=> {:dorothy.core/type :dorothy.core/node-id :id :foo}
  "
  ([id port compass-pt]
    { :type ::node-id :id id :port port :compass-pt (check-compass-pt compass-pt) })
  ([id port]
    (node-id id port nil))
  ([id]
    (node-id id nil nil)))

(defn ^:private x-attrs [type attrs] { :type type :attrs attrs})

(defn graph-attrs
  "Create a graph attribute statement. attrs is the attribute map.

  Examples:

    (graph-attrs {:label \"hi\"})
    ;=> {:dorothy.core/type :dorothy.core/graph-attrs :attrs {:label \"hi\"}
  "
  [attrs]
  { :type ::graph-attrs :attrs attrs })

(defn node-attrs
  "Create a node attribute statement. attrs is the attribute map.

  Examples:

    (node-attrs {:label \"hi\"})
    ;=> {:dorothy.core/type :dorothy.core/node-attrs :attrs {:label \"hi\"}
  "
  [attrs]
  { :type ::node-attrs :attrs attrs })

(defn edge-attrs
  "Create a edge attribute statement. attrs is the attribute map.

  Examples:

    (edge-attrs {:label \"hi\"})
    ;=> {:dorothy.core/type :dorothy.core/edge-attrs :attrs {:label \"hi\"}
  "
  [attrs]
  { :type ::edge-attrs :attrs attrs })

(defn node
  "Create a node in a graph. This is a more structured version of the
  :node-id or [:node-id { attrs }] sugar for specifying nodes in a graph. Its
  result may be used in place of that sugar within a graph specification.

  attrs is a possibly empty map of attributes for the edge
  id is the result of (dorothy.core/node-id)"
  [attrs id]
  (check-ast id ::node-id)
  { :type ::node :attrs attrs :id id })

(defn edge
  "Create an edge. This is a more structured version of the
  [:source :target] sugar for specifying edges. Its result may be used in place
  of that sugar within a graph specification.

  attrs is a possibly empty map of attributes for the edge.
  node-ids is a seq of 2 or more node identifiers.

  See:
    (dorothy.core/node-id)
  "
  [attrs node-ids]
  (doseq [n node-ids] (check-ast n #{::node-id ::subgraph}))
  { :type ::edge :attrs attrs :node-ids node-ids })

(defn graph*
  "Create a graph AST node with type `:dorothy.core/graph`.

  opts is an option map with keys `:id` and `:strict?`
  statements is a list of statement AST nodes."
  [opts statements]
  (let [{:keys [id strict?]} opts]
    {:type      ::graph
     :id         id
     :strict?    (boolean strict?)
     :statements statements }))

(derive ::digraph ::graph)
(derive ::subgraph ::graph)

(defn digraph*
  "Same as `(dorothy.core/graph*)` but has type `:dorothy.core/digraph`"
  [opts statements]  (assoc (graph* opts statements) :type ::digraph))

(defn subgraph*
  "Same as `(dorothy.core/graph*)` but has type `:dorothy.core/subgraph`"
  [opts statements]
  (assoc (graph* opts statements) :type ::subgraph))

;; ----------------------------------------------------------------------
;; # Dorothy Graph DSL Processing
;;
;; Implements the graph DSL described above.

(defn ^:private vector-to-ast-edge [v]
  (let [end    (last v)
        attrs? (map? end)
        attrs  (if attrs? end {})
        parts  (if attrs? (butlast v) v)
        parts  (remove #{:>} parts)]
    (edge attrs (map to-ast parts))))

(defn ^:private vector-to-ast [[v0 v1 & more :as v]]
  (cond
    more         (vector-to-ast-edge v)
    (map? v1)    (node v1 (to-ast v0))
    v1           (vector-to-ast-edge v)
    (is-ast? v0) v0
    (map? v0)    (graph-attrs v0)
    (gen-id? v0) (node {} (node-id v0))
    v0           (node {} (to-ast v0))))

(defn ^:private parse-node-id [v]
  (apply node-id (cs/split v #":")))

(defn ^:private to-ast [v]
  (cond
    (is-ast? v)  v
    (keyword? v) (parse-node-id (name v))
    (string?  v) (parse-node-id v)
    (number?  v) (parse-node-id (str v))
    (gen-id?  v) (node-id v)
    (map? v)     (graph-attrs v)
    (vector? v)  (vector-to-ast v)
    :else        (error "Don't know what to do with %s" v)))

(defn ^:private desugar-graph-options
  "Turn first arg of (graph) into something usable"
  [options]
  (cond
    (map? options)     options
    (keyword? options) {:id options}
    (number? options)  {:id (str options)}
    (string? options)  {:id options}
    :else            (error "Invalid graph arg %s" options)))

(defn ^:private flatten-statements
  [ss]
  (let [helper (fn [statement]
                 (cond
                   (seq? statement)
                   (flatten-statements statement)
                   :else
                   [statement]))]
    (mapcat helper ss)))

(defn graph
  "Construct an undirected graph from the given statements which must be a vector.
  See https://github.com/daveray/dorothy or README.md for details of the DSL.

  The returned value may be converted to dot language with (dorothy.core/dot)."
  ([handler options statements]
   (handler (desugar-graph-options options)
            (map to-ast (flatten-statements statements))))
  ([options statements]
   (graph graph* options statements))
  ([statements]
   (graph {} statements)))

(defn digraph
  "Construct a directed graph from the given statements which must be a vector.
  See https://github.com/daveray/dorothy or README.md for details of the DSL.

  The returned value may be converted to dot language with (dorothy.core/dot)."
  ([attrs statements] (graph digraph* attrs statements))
  ([statements]       (digraph {} statements)))

(defn subgraph
  "Construct a sub-graph from the given statements which must be a vector.
  See https://github.com/daveray/dorothy or README.md for details of the DSL.
  A subgraph may be used as a statement in a graph, or as a node entry in
  an edge statement.

  The returned value may be converted to dot language with (dorothy.core/dot)."
  ([attrs statements] (graph subgraph* attrs statements))
  ([statements]       (subgraph {} statements)))

;; ----------------------------------------------------------------------
;; # DOT generation
;;
;; Generate DOT language from a graph AST.

(def ^:dynamic ^:private *options*
  {:edge-op      "->"
   :id-generator #(-> % hash str)})

; id's that don't need quotes
(def ^:private safe-id-pattern #"^[_a-zA-Z\0200-\0377][_a-zA-Z0-9\0200-\0377]*$")
(def ^:private html-pattern    #"^\s*<([a-zA-Z1-9_-]+)(\s|>).*</\1>\s*$")

(defn ^:private safe-id? [s] (re-find safe-id-pattern s))
(defn ^:private html? [s] (re-find html-pattern s))
(defn ^:private escape-quotes [s] (cs/replace s "\"" "\\\""))
(defn ^:private escape-id [id]
  (cond
    (string? id)  (cond
                    (safe-id? id) id
                    (html? id)    (str \< id \>)
                    :else         (str \" (escape-quotes id) \"))
    (number? id)  (str id)
    (keyword? id) (escape-id (name id))
    (gen-id? id)  (escape-id ((:id-generator *options*) (id)))
    :else         (error "Invalid id: %s - %s" (class id) id)))

(defmulti dot* :type)

(defn ^:private dot*-statements [statements]
  (apply str (interleave (map dot* statements) (repeat ";\n"))))

(defmethod dot* ::node-id [{:keys [id port compass-pt]}]
  (str
    (escape-id id)
    (if port (str ":" (escape-id port)))
    (if compass-pt (str ":" (name compass-pt)))))

(defn dot*-attrs [attrs]
  (cs/join
    \,
    (for [[k v] attrs]
      (str (escape-id k) \= (escape-id v)))))

(defn ^:private dot*-trailing-attrs [attrs]
  (if-not (empty? attrs)
    (str " [" (dot*-attrs attrs) "]")))

(defn dot*-x-attrs [type {:keys [attrs]}]
  (str type " [" (dot*-attrs attrs) "]"))

(defmethod dot* ::graph-attrs [this] (dot*-x-attrs "graph" this))
(defmethod dot* ::node-attrs  [this] (dot*-x-attrs "node" this))
(defmethod dot* ::edge-attrs  [this] (dot*-x-attrs "edge" this))

(defmethod dot* ::node [{:keys [attrs id]}]
  (str (dot* id) (dot*-trailing-attrs attrs)))

(defmethod dot* ::edge [{:keys [attrs node-ids]}]
  (str
    (cs/join (str " " (:edge-op *options*) " ") (map dot* node-ids))
    (dot*-trailing-attrs attrs)))

(defn ^:private options-for-type [type]
  (condp = type
    ::graph    (assoc *options* :edge-op "--")
    ::digraph  (assoc *options* :edge-op "->")
    ::subgraph *options*))

(defmethod dot* ::graph [{:keys [id strict? statements] :as this}]
  (binding [*options* (merge
                        (options-for-type (:type this))
                        {:id-generator (id-generator)})]
    (str (if strict? "strict ")
         (name (:type this)) " "
         (if id (str (escape-id id) " "))
         "{\n" (dot*-statements statements) "} ")))

(defn dot
  "Convert the given Dorothy graph AST to a string suitable for input to
  the Graphviz dot tool.

  input can either be the result of (graph) or (digraph), or it can be a vector of
  statements (see README.md) in which case (graph) is implied.

  Examples:

    user=> (dot (digraph [[:a :b :c]]))
    \"digraph { a -> b -> c; }\"

  See:
  * `(dorothy.core/render)`
  * `(dorothy.core/show!)`
  * `(dorothy.core/save!)`
  "
  [input]
  (cond
    (is-ast? input) (dot* input)
    (vector? input)             (dot* (graph input))
    (seq?    input)             (dot* (graph input))
    :else                       (error "Invalid (dot) input: %s" input)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ----------------------------------------------------------------------
;; # Graph Rendering
;; Functions responsible for taking a DOT language graph and rendering it
;; to an image.

(defn ^:private build-render-command [{:keys [format layout scale invert-y?]}]
  (->>
    ["dot"
     (if format    (str "-T" (name format)))
     (if layout    (str "-K" (name layout)))
     (if scale     (str "-s" scale))
     (if invert-y? "-y")]
    (remove nil?)))

(defn ^:private ^java.lang.ProcessBuilder init-process-builder
  [{:keys [dir] :as options}]
  (let [pb (java.lang.ProcessBuilder. ^java.util.List (build-render-command options))]
    (when dir (.directory pb (if (instance? java.io.File dir)
                               dir
                               (java.io.File. (str dir)))))
    pb))

(def ^:private binary-formats
  #{:bmp :eps :gif :ico :jpg :jpeg :pdf :png :ps :ps2 :svgz :tif :tiff :vmlz :wbmp})

(defn ^:private read-dot-result [input-stream {:keys [format binary?]}]
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

  * (dorothy.core/dot)
  * http://www.graphviz.org/content/command-line-invocation
  * http://www.graphviz.org/content/output-formats
  "
  [graph options]
  (let [p        (.start (init-process-builder options))
        from-dot (future (with-open [from-dot (.getInputStream p)]
                           (read-dot-result from-dot options)))]
    (with-open [to-dot (.getOutputStream p)]
      (spit to-dot graph))
    @from-dot))

(defn save!
  "Render and save the given graph (string result of (dorothy.core/dot)) to an
  output stream. f is any argument acceptable to (clojure.java.io/ouput-stream).

  Examples:

    ; Write a graph to a png file
    (-> (digraph [[:a :b :c]])
        dot
        (save! \"out.png\" {:format :png}))

  See:

  * (dorothy.core/render)
  * (dorothy.core/dot)
  * http://clojure.github.com/clojure/clojure.java.io-api.html#clojure.java.io/make-output-stream
"
  [graph f & [options]]
  (let [bytes (render graph (merge options {:binary? true}))]
    (with-open [output (jio/output-stream f)]
      (jio/copy bytes output)))
  graph)

(defonce ^:private frames (atom {}))

(defn- get-frame [id options]
  ((swap! frames (fn [fs]
                   (if (contains? fs id)
                     fs
                     (let [f (javax.swing.JFrame. "Dorothy")]
                       (.setLocationByPlatform f true)
                       (assoc fs id f)))))
   id))

(defn show!
  "Show the given graph (must be the string result of (dorothy.core/dot)) in a
  new Swing window with scrollbars. Supports same options as
  (dorothy.core/render) except that :format is ignored.

  Examples:

    ; Simple 3 node graph, converted to dot and displayed.
    (-> (digraph [[:a :b :c]]) dot show!)

  Additional options:

  * :frame supply to reuse frames.
  * :frame-width specify maximum frame width.
  * :frame-height specify maximum frame width.

  Notes:

  * Closing the resulting frame will not cause the JVM to exit.

  See:

  * `(dorothy.core/render)`
  * `(dorothy.core/dot)`
  "
  [graph & [options]]
  (let [id (:frame options (gensym))
        ^javax.swing.JFrame frame (get-frame id options)
        shortcut-mask (int (.. java.awt.Toolkit getDefaultToolkit getMenuShortcutKeyMask))
        close-key (javax.swing.KeyStroke/getKeyStroke java.awt.event.KeyEvent/VK_W shortcut-mask)
        ^bytes bytes (render graph (merge options {:format :png}))
        icon  (javax.swing.ImageIcon. bytes)
        max-w (:frame-width options 640)
        max-h (:frame-height options 480)
        w     (.getIconWidth icon)
        h     (.getIconHeight icon)
        lbl   (javax.swing.JLabel. icon)
        sp    (javax.swing.JScrollPane. lbl)]
    (.. sp getInputMap (put close-key "closeWindow"))
    (.. sp getActionMap (put "closeWindow" (proxy [javax.swing.AbstractAction] []
                                             (actionPerformed [e]
                                               (.setVisible frame false)
                                               (.dispose frame)
                                               (swap! frames dissoc id)))))
    (doto frame
      (.setTitle (format "Dorothy %s (%dx%d)" id w h))
      (.setContentPane sp)
      (.setSize (min max-w (+ w 50)) (min max-h (+ h 50)))
      (.setVisible true))
    nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ----------------------------------------------------------------------
;; # Random tests

(comment

  (println (dot* (node-id "start" "p" :ne)))
  (println (dot* (node-id "start" "p")))
  (println (dot* (node {} (node-id :start))))
  (println (dot* (node {:style :filled :color :blue} (node-id :start) )))
  (println (dot* (edge {} [(node-id :start)(node-id :end)])))
  (println (binding [*options* {:edge-op "--"}]
             (dot* (edge {:color :grey} [(node-id :start)(node-id :middle :p :_)(node-id :end)]))))
  (println (dot* (graph-attrs {:style :filled})))
  (println (dot* (node-attrs {:style :filled, :color :red})))
  (println (dot* (edge-attrs {:style :filled})))

  (println (dot
             (graph
               {:id :G :strict? true}
               [(edge nil [(node-id "start") (node-id :a0)])
                (edge {:color :green}
                      [(node-id :a0)
                       (subgraph [{:style :filled :color :lightgrey :label "Hello"}
                                  (edge {} [(node-id :a) (node-id :b)])
                                  (edge {} [(node-id :b) (node-id :c)]) ])
                       (node-id :a1)])
                (node {:shape :Mdiamond} (node-id :start))])))

  (println (dot*
             (digraph*
               ;{:id :G :strict? true}
               {}
               [(edge nil [(node-id "start") (node-id :a0)])
                (edge {:color :gre_en :text "hello\"there"} [(node-id :a0) (node-id :a1)])
                (node {:shape :Mdiamond} (node-id :start))])))

  (-> (digraph :G [(for [i (range 5)]
                     [i :> (inc i)])
                   [5 0]])
      dot
      show!)

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
      show!))
