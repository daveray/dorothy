; Copyright (c) Dave Ray, 2011. All rights reserved.

; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v10.html at the root of this
; distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

; http://www.graphviz.org/content/ER
(ns dorothy.examples.er
  (:use dorothy.core))

(defn -main [] 
  (-> 
    (graph :ER [
      {:rankdir :LR}

      (node-attrs {:shape :box})

      :course :institute :student

      (node-attrs {:shape :ellipse})

      (subgraph [
        [:node {:label "name"}] 
        :name0 :name1 :name2])

      :code :grade :number

      (node-attrs {:shape :diamond :style :filled :color :lightgrey}) 
      "C-I" "S-C" "S-I"

      ; Edges
      [:name0     :> :course]
      [:code      :> :course]
      [:course    :> "C-I"      {:label "n" :len 1.00}]
      ["C-I"      :> :institute {:label "1" :len 1.00}]
      [:institute :> :name1]
      [:institute :> "S-I"      {:label "1" :len 1.00}]
      ["S-I"      :> :student   {:label "n" :len 1.00}]
      [:student   :> :grade]
      [:student   :> :name2]
      [:student   :> :number]
      [:student   :> "S-C"      {:label "m" :len 1.00}]
      ["S-C"      :> :course    {:label "n" :len 1.00}]

      {:label  "\n\nEntity Relation Diagram\ndrawn by NEATO"
       :fontsize 20}
      ])
    dot
    (show! {:layout :neato}))) 

