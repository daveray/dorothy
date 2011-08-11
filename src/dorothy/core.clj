(ns dorothy.core)

(def ^:dynamic *options* {:edge-op "->"})

(defn escape-id [id]
  (cond
    (keyword? id) (escape-id (name id))
    (string? id)  (cond 
                    (.startsWith id "<") id
                    (and (.startsWith id "\"") (.endsWith id "\"")) id
                    :else (str \" id \"))
    :else (escape-id (str id))))

(defprotocol Dot 
  (dot* [this]))

(defn statements [& ss]
  (reify Dot
    (dot* [this]
      (apply str (map #(str (dot* %) ";\n") ss)))))

(defn node-id 
  ([id port compass-pt]
    (reify Dot
      (dot* [this]
        (str (escape-id id) 
            (if port (str ":" (escape-id port)))
            (if compass-pt (str ":" (name compass-pt)))))))
  ([id port]
    (node-id id port nil))
  ([id]
    (node-id id nil nil)))

(defn attr [key val]
  (reify Dot
    (dot* [this]
      (str (escape-id key) \= (escape-id val)))))

(defn attrs [options]
  (reify Dot
    (dot* [this]
      (apply str (interpose \, (for [[k v] options] (dot* (attr k v))))))))


(defn x-attrs [type options]
  (reify Dot
    (dot* [this]
      (str type " [" (dot* (attrs options)) "]"))))

(def graph-attrs (partial x-attrs "graph"))
(def node-attrs (partial x-attrs "node"))
(def edge-attrs (partial x-attrs "edge"))

(defn trailing-attrs [attr-map]
  (if attr-map (str " [" (dot* (attrs attr-map)) "]")))

(defn node 
  ([attr-map id]
    (reify Dot
      (dot* [this]
        (str (dot* id) (trailing-attrs attr-map)))))
  ([id]
    (node nil id)))

(defn edge [attr-map & nodes]
  (reify Dot
    (dot* [this] 
      (str (apply str (->> nodes
        (map dot*)
        (interpose (str " " (:edge-op *options*) " "))))
       (trailing-attrs attr-map)))))

(defn options-for-type [type]
  (condp = type
    :graph    (assoc *options* :edge-op "--")
    :digraph  (assoc *options* :edge-op "->")
    :subgraph *options*))

(defn graph [opts & stmts]
  (let [{:keys [type id strict?]} opts 
        type (or type :graph)
        id (or id (gensym "G"))]
    (reify Dot
      (dot* [this]
        (binding [*options* (options-for-type type)] 
          (str (if strict? "strict ") (name type) " " (escape-id id) " {\n" (dot* (apply statements stmts)) "} "))))))

(defn digraph [opts & stmts]
  (apply graph (assoc opts :type :digraph) stmts))

(defn subgraph [opts & stmts]
  (apply graph (assoc opts :type :subgraph) stmts))

(dot* (attrs {:style :filled :color :blue}))
(dot* (node-id "start" "p" :ne))
(dot* (node-id "start" "p"))
(dot* (statements (node-id :start)(node-id "start" "p")))
(dot* (node (node-id :start)))
(dot* (node {:style :filled :color :blue} (node-id :start) ))
(dot* (edge nil (node-id :start)(node-id :end)))
(binding [*options* {:edge-op "--"}] 
  (dot* (edge {:color :grey} (node-id :start)(node-id :middle :p :_)(node-id :end))))
(dot* (graph-attrs {:style :filled}))
(dot* (node-attrs {:style :filled, :color :red}))
(dot* (edge-attrs {:style :filled}))
(dot* (attr :color :lightgrey))

(println (dot*
  (graph
    ;{:id :G :strict? true}
    nil
    (edge nil (node-id "start") (node-id :a0))
    (edge {:color :green} (node-id :a0) 
          (subgraph nil (edge nil (node-id :a) (node-id :b)))
          (node-id :a1))
    (node {:shape :Mdiamond} (node-id :start)))))

(println (dot*
  (digraph
    ;{:id :G :strict? true}
    nil
    (edge nil (node-id "start") (node-id :a0))
    (edge {:color :green} (node-id :a0) (node-id :a1))
    (node {:shape :Mdiamond} (node-id :start)))))
