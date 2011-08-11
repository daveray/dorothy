(ns dorothy.core
  (:require [clojure.string :as cs]))

(def ^:dynamic *options* {:edge-op "->"})

; id's that don't need quotes
(def ^:private safe-id-pattern #"^[_a-zA-Z\0200-\0377][_a-zA-Z0-9\0200-\0377]*$")

(defn- safe-id? [s] (re-find safe-id-pattern s))

(defn- escape-id [id]
  (cond
    (keyword? id) (escape-id (name id))
    (string? id)  (if (safe-id? id) 
                    id
                    (str \" (cs/replace (str id) "\"" "\\\"") \"))
    :else         (escape-id (str id))))

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
      (cs/join \, (for [[k v] options] (dot* (attr k v)))))))

(defn- trailing-attrs [attr-map]
  (if attr-map (str " [" (dot* (attrs attr-map)) "]")))

(defn- x-attrs [type options]
  (reify Dot
    (dot* [this]
      (str type " [" (dot* (attrs options)) "]"))))

(def graph-attrs (partial x-attrs "graph"))
(def node-attrs  (partial x-attrs "node"))
(def edge-attrs  (partial x-attrs "edge"))

(defn node 
  ([attr-map id]
    (reify Dot
      (dot* [this]
        (str (dot* id) (trailing-attrs attr-map)))))
  ([id]
    (node nil id)))

(defn edge [attr-map & node-ids]
  (reify Dot
    (dot* [this] 
      (str 
        (cs/join (str " " (:edge-op *options*) " ") (map dot* node-ids)) 
        (trailing-attrs attr-map)))))

(defn- options-for-type [type]
  (condp = type
    :graph    (assoc *options* :edge-op "--")
    :digraph  (assoc *options* :edge-op "->")
    :subgraph *options*))

(defn graph [opts & stmts]
  (let [{:keys [type id strict?]} opts 
        type (or type :graph)]
    (reify Dot
      (dot* [this]
        (binding [*options* (options-for-type type)] 
          (str (if strict? "strict ") 
               (name type) " "
               (if id (str (escape-id id) " ")) 
               "{\n" (dot* (apply statements stmts)) "} "))))))

(defn digraph [opts & stmts]
  (apply graph (assoc opts :type :digraph) stmts))

(defn subgraph [opts & stmts]
  (apply graph (assoc opts :type :subgraph) stmts))

(println (dot* (attrs {:style :filled :color :blue :text "foo\"bar"})))
(println (dot* (node-id "start" "p" :ne)))
(println (dot* (node-id "start" "p")))
(println (dot* (statements (node-id :start)(node-id "start" "p"))))
(println (dot* (node (node-id :start))))
(println (dot* (node {:style :filled :color :blue} (node-id :start) )))
(println (dot* (edge nil (node-id :start)(node-id :end))))
(println (binding [*options* {:edge-op "--"}] 
  (dot* (edge {:color :grey} (node-id :start)(node-id :middle :p :_)(node-id :end)))))
(println (dot* (graph-attrs {:style :filled})))
(println (dot* (node-attrs {:style :filled, :color :red})))
(println (dot* (edge-attrs {:style :filled})))
(println (dot* (attr :color :lightgrey)))

(println (dot*
  (graph
    {:id :G :strict? true}
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
    (edge {:color :gre_en :text "hello\"there"} (node-id :a0) (node-id :a1))
    (node {:shape :Mdiamond} (node-id :start)))))

