(ns dorothy.test.core
  (:use [dorothy.core])
  (:use [clojure.test]))

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
  (testing "returns ::type ::node-id"
    (is (= { :dorothy.core/type :dorothy.core/node-id :id :foo :port :bar :compass-pt :n}
           (node-id :foo :bar :n))))
  (testing "checks compass point"
    (is (thrown? RuntimeException (node-id :a :b :x)))
    (are [pt] (node-id :a :b pt) 
      :n :ne :e :se :s :sw :w :nw :c :_
      "n" "ne" "e" "se" "s" "sw" "w" "nw" "c" "_")))

(deftest test-graph-attrs
  (testing "return ::type ::graph-attrs"
    (is (= {:dorothy.core/type :dorothy.core/graph-attrs :attrs {:a 1}}
           (graph-attrs {:a 1})))))

(deftest test-node-attrs
  (testing "return ::type ::node-attrs"
    (is (= {:dorothy.core/type :dorothy.core/node-attrs :attrs {:a 1}}
           (node-attrs {:a 1})))))

(deftest test-edge-attrs
  (testing "return ::type ::edge-attrs"
    (is (= {:dorothy.core/type :dorothy.core/edge-attrs :attrs {:a 1}}
           (edge-attrs {:a 1})))))

(deftest test-node
  (testing "checks that :id is a node-id"
    (is (thrown? RuntimeException (node {} 99))))
  (testing "return ::type ::node"
    (is (= {:dorothy.core/type :dorothy.core/node :attrs {:a 1} :id (node-id :foo) }
           (node {:a 1} (node-id :foo))))))

(deftest test-edge
  (testing "checks that :node-ids is all node-id"
    (is (thrown? RuntimeException (edge {} [(node-id :hi) 1.2 (node-id :bye)]))))
  (testing "return ::type ::edge"
    (is (= {:dorothy.core/type :dorothy.core/edge :attrs {:a 1} :node-ids [(node-id :foo)(node-id :bar)] }
           (edge {:a 1} [(node-id :foo)(node-id :bar)])))))

(deftest test-graph*
  (testing "returns ::type ::graph"
    (is (= {:dorothy.core/type :dorothy.core/graph :id :G :strict? true :statements [] }
           (graph* {:id :G :strict? true} [])))))

(deftest test-digraph*
  (testing "returns ::type ::digraph"
    (is (= {:dorothy.core/type :dorothy.core/digraph :id :G :strict? false :statements [] }
           (digraph* {:id :G } [])))))

(deftest test-subgraph*
  (testing "returns ::type ::subgraph"
    (is (= {:dorothy.core/type :dorothy.core/subgraph :id :G :strict? false :statements [] }
           (subgraph* {:id :G } [])))))

