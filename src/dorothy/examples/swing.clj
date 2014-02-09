; Copyright (c) Dave Ray, 2011. All rights reserved.

; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v10.html at the root of this
; distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns dorothy.examples.swing
  (:use dorothy.core)
  (:import [javax.swing JPanel JButton JLabel]))

; An example of generating a graph from a swing widget hierarchy.
; Uses the (gen-id) function to generate ids for swing objects.

(def widgets
  (doto (JPanel.)
    (.add (JLabel. "First"))
    (.add (JButton. "Second"))
    (.add (doto (JPanel. )
            (.add (JLabel. "Nested First"))
            (.add (JButton. "Nested Second"))))
    (.add (JButton. "Third"))
    (.add (JLabel. "Fourth"))))

(defn label-for [v]
  (cond
    (instance? JButton v) (.getText v)
    (instance? JLabel v)  (.getText v)
    :else (.getSimpleName (class v))))

(defn children [p]
  (seq (.getComponents p)))

(defn node-and-edges [p]
  (list
    [(gen-id p) {:label (label-for p)}]
    (for [c (.getComponents p)]
      [(gen-id c) :> (gen-id p)])))

(defn -main [& args]
  (-> (digraph :Swing
               (->> (tree-seq children children widgets)
                    (map node-and-edges)))
      dot
      show!))

(comment
  (-main))

