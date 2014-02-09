(ns dorothy.test.core
  (:require [dorothy.core :as d]
            [clojure.test :refer [deftest testing is are]]))

(deftest test-is-ast?
  (testing "Returns true for any map with :type"
    (is (d/is-ast? {:type :foo}))
    (is (not (d/is-ast? 99)))
    (is (not (d/is-ast? {}))))
  (testing "Checks for a particular type"
    (is (d/is-ast? {:type :foo} :foo))
    (is (not (d/is-ast? {:type :foo} :bar))))
  (testing "Checks for one of several types in a set"
    (is (d/is-ast? {:type :foo} #{:foo :bar}))
    (is (d/is-ast? {:type :bar} #{:foo :bar}))
    (is (not (d/is-ast? {:type :yum} #{:foo :bar})))))

(deftest test-escape-id
  (testing "does nothing to pure ids"
    (is (= "_abc123" (#'dorothy.core/escape-id :_abc123)))
    (is (= "_abc123" (#'dorothy.core/escape-id "_abc123"))))
  (testing "quotes ids with special chars"
    (is (= "\"_abc123!\"" (#'dorothy.core/escape-id :_abc123!)))
    (is (= "\"_ab\\\"c123\"" (#'dorothy.core/escape-id "_ab\"c123"))))
  (testing "surrounds HTML with <>"
    (is (= "<<html></html>>" (#'dorothy.core/escape-id "<html></html>")))))

(deftest html?-can-detect-html-looking-stuff
  (is (#'dorothy.core/html? "<html></html>"))
  (is (#'dorothy.core/html? "  <table border=9></table>  "))
  (is (#'dorothy.core/html? "<html><foo/>  <bar></html>"))
  (is (not (#'dorothy.core/html? "<html></html")))
  (is (not (#'dorothy.core/html? "<html</html>"))))

(deftest test-node-id
  (testing "returns :type ::node-id"
    (is (= { :type ::d/node-id :id :foo :port :bar :compass-pt :n}
           (d/node-id :foo :bar :n))))
  (testing "checks compass point"
    (is (thrown? RuntimeException (d/node-id :a :b :x)))
    (are [pt] (d/node-id :a :b pt)
      :n :ne :e :se :s :sw :w :nw :c :_
      "n" "ne" "e" "se" "s" "sw" "w" "nw" "c" "_")))

(deftest test-graph-attrs
  (testing "return :type ::graph-attrs"
    (is (= {:type ::d/graph-attrs :attrs {:a 1}}
           (d/graph-attrs {:a 1})))))

(deftest test-node-attrs
  (testing "return :type ::node-attrs"
    (is (= {:type ::d/node-attrs :attrs {:a 1}}
           (d/node-attrs {:a 1})))))

(deftest test-edge-attrs
  (testing "return :type ::edge-attrs"
    (is (= {:type ::d/edge-attrs :attrs {:a 1}}
           (d/edge-attrs {:a 1})))))

(deftest test-node
  (testing "checks that :id is a node-id"
    (is (thrown? RuntimeException (d/node {} 99))))
  (testing "return :type ::node"
    (is (= {:type ::d/node :attrs {:a 1} :id (d/node-id :foo) }
           (d/node {:a 1} (d/node-id :foo))))))

(deftest test-edge
  (testing "checks that :node-ids is all node-id"
    (is (thrown? RuntimeException (d/edge {} [(d/node-id :hi) 1.2 (d/node-id :bye)]))))
  (testing "return :type ::edge"
    (is (= {:type ::d/edge :attrs {:a 1} :node-ids [(d/node-id :foo)(d/node-id :bar)] }
           (d/edge {:a 1} [(d/node-id :foo)(d/node-id :bar)])))))

(deftest test-graph*
  (testing "returns :type ::graph"
    (is (= {:type ::d/graph :id :G :strict? true :statements [] }
           (d/graph* {:id :G :strict? true} [])))))

(deftest test-digraph*
  (testing "returns :type :::digraph"
    (is (= {:type ::d/digraph :id :G :strict? false :statements [] }
           (d/digraph* {:id :G } [])))))

(deftest test-subgraph*
  (testing "returns :type ::subgraph"
    (is (= {:type ::d/subgraph :id :G :strict? false :statements [] }
           (d/subgraph* {:id :G } [])))))

(deftest test-statements-are-flattened
  (let [input [(list {:style :filled})
               :a
               [:a :> :b]
               (cons :c (list (for [i [:d :e :f]]
                                i)))]
        result (d/graph input)]
    (is (= [{:type ::d/graph-attrs}
            {:type ::d/node-id :id "a"}
            {:type ::d/edge }
            {:type ::d/node-id :id "c"}
            {:type ::d/node-id :id "d"}
            {:type ::d/node-id :id "e"}
            {:type ::d/node-id :id "f"} ]
           (->> result
                :statements
                (map #(select-keys % [:type :id])))))))
