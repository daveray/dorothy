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

(deftest test-attr
  (testing "escapes key and value"
    (is (= "_123=\"hello there\"" (dot* (attr :_123 "hello there"))))))

