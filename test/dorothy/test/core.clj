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
  (testing "checks compass point"
    (is (thrown? RuntimeException (node-id :a :b :x)))
    (are [pt] (node-id :a :b pt) 
      :n :ne :e :se :s :sw :w :nw :c :_
      "n" "ne" "e" "se" "s" "sw" "w" "nw" "c" "_")))
