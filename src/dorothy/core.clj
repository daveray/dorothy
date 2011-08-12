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

(declare to-dottable)
(defprotocol Dot 
  (dot* [this]))

(defn statements [ss]
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
  (if-not (empty? attr-map) (str " [" (dot* (attrs attr-map)) "]")))

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
    (node {} id)))

(defn edge [attr-map node-ids]
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

(defn graph* [opts stmts]
  (let [{:keys [type id strict?]} opts 
        type (or type :graph)]
    (reify Dot
      (dot* [this]
        (binding [*options* (options-for-type type)] 
          (str (if strict? "strict ") 
               (name type) " "
               (if id (str (escape-id id) " ")) 
               "{\n" (dot* (statements stmts)) "} "))))))

(defn digraph* [opts stmts]
  (graph* (assoc opts :type :digraph) stmts))

(defn subgraph* [opts stmts]
  (graph* (assoc opts :type :subgraph) stmts))

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

(defn to-dottable [v]
  (cond
    (satisfies? Dot v) v
    (keyword? v)       (node-id v)
    (string?  v)       (node-id v)
    (map? v)           (graph-attrs v)
    (vector? v)        (vector-to-dottable v)))

(defn- canonicalize-graph-attrs [attrs]
  (cond
    (map? attrs) attrs
    (keyword? attrs) {:id attrs}
    (number? attrs)  {:id (str attrs)}
    (string? attrs)  {:id attrs}
    :else            (throw (IllegalArgumentException. (str "Invalid graph arg " attrs)))))

(defn graph 
  ([attrs stmts]
   (graph* (canonicalize-graph-attrs attrs) (map to-dottable stmts)))
  ([stmts] (graph {} stmts)))
(defn digraph 
  ([attrs stmts]
   (digraph* (canonicalize-graph-attrs attrs) (map to-dottable stmts)))
  ([stmts] (digraph {} stmts)))
(defn subgraph 
  ([attrs stmts]
   (subgraph* (canonicalize-graph-attrs attrs) (map to-dottable stmts)))
  ([stmts] (subgraph {} stmts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(println (dot* (digraph :G [
  (subgraph :cluster_0 [
    {:style :filled, :color :lightgrey, :label "process #1"}
    [:node {:style :filled, :color :white}]

    [:a0 :> :a1 :> :a2 :> :a3]])

  (subgraph :cluster_1 [
    {:style :filled, :color :blue, :label "process #2"}
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
  [:end   {:shape :Msquare}]])))
