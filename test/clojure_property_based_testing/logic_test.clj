(ns clojure-property-based-testing.logic-test
  (:require
   [clojure-property-based-testing.generators.person-generator :as pg]
   [clojure-property-based-testing.logic :refer [arrives-at fits-queue?]]
   [clojure-property-based-testing.models :as m]
   [clojure.set :as set]
   [clojure.test :refer [are deftest is testing]]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as properties]
   [schema-generators.complete :as c]
   [schema-generators.generators :as g]))

(deftest fits-queue-test
  (testing "It should be able to tell that that an empty queue fits a new person"
    (doseq
     [people (gen/sample (gen/vector pg/pname 0 4))]
      (let [hospital   {:departments {:cardiology (into m/Empty-Queue people)}}
            department :cardiology]
        (is (= true (fits-queue? hospital department))))))

  (testing "It should be able to tell that that an random queue having less than 5 elements fits a new person")
  (let [hospital   {:departments {:cardiology m/Empty-Queue}}
        department :cardiology]
    (is (= true (fits-queue? hospital department))))

  (testing "It should be able to tell that that an filled queue doesn't fit a new person"
    (let [hospital   {:departments {:cardiology (into m/Empty-Queue ["1" "2" "3" "4" "5" "6"])}}
          department :cardiology]
      (is (= false (fits-queue? hospital department)))))

  (testing "It should be able to tell that that an filled with 5 PEOPLE queue doesn't fit a new person"
    (let [hospital   {:departments {:cardiology (into m/Empty-Queue ["1" "2" "3" "4" "5"])}}
          department :cardiology]
      (is (= false (fits-queue? hospital department)))))

  (testing "It should throw schema error when provided invalid hospital"
    (let [hospital   {:invalid-keyword {:cardiology (into m/Empty-Queue ["1" "2" "3" "4" "5"])}}
          department :cardiology]
      (is (thrown-with-msg? Exception #"does not match schema*" (fits-queue? hospital department)))))

  (testing "It should throw schema error when provided invalid department"
    (let [hospital   {:departments {:invalid-department (into m/Empty-Queue ["1" "2" "3" "4" "5"])}}
          department :cardiology]
      (is (thrown-with-msg? Exception #"does not match schema*" (fits-queue? hospital department)))))

  (testing "It should be able to tell that that an nil queue doesn't fit a new person"
    (let [hospital   {:departments {:cardiology nil}}
          department :cardiology]
      (is (= false (fits-queue? hospital department))))))

(defspec arrives-at-success-test 10
  (properties/for-all
   [queue-as-vector (gen/vector pg/pname 0 4)
    person-to-arrive pg/pname]
   (let [queue (into m/Empty-Queue queue-as-vector)
         hospital {:departments {:cardiology queue}}
         expected-output {:departments {:cardiology (conj queue person-to-arrive)}}]
     (is (= expected-output
            (arrives-at hospital :cardiology person-to-arrive))))))

(defspec arrives-at-generated-schema-success-test 30
  (properties/for-all
   [queue (gen/fmap (partial into m/Empty-Queue) (gen/vector pg/pname 0 4))
    department (g/generator m/Departments)
    person-to-arrive pg/pname]
   (let [hospital (c/complete {:departments {department queue}} m/Hospital)
         output (arrives-at hospital department person-to-arrive)
         output-queue (department (:departments output))]
     (is (= person-to-arrive (last output-queue)))
     (is (= (inc (count queue)) (count output-queue)))
     (is (= (->  queue vec seq) (-> output-queue vec butlast))))))

(deftest arrives-at-test
  (testing "It should be able to insert a new person in the queue if is not empty"
    (let [hospital        {:departments {:cardiology (into m/Empty-Queue ["1" "2"])}}
          expected-output {:departments {:cardiology (into m/Empty-Queue ["1" "2" "5"])}}]
      (is (= expected-output (arrives-at hospital :cardiology "5")))))

  (testing "It should be able to insert a new person in the queue if is not empty"
    (are [hospital person-to-add expected-output]
         (= expected-output (arrives-at hospital :cardiology person-to-add))
      {:departments {:cardiology (into m/Empty-Queue ["1" "2"])}} "5" {:departments {:cardiology (into m/Empty-Queue ["1" "2" "5"])}}
      {:departments {:cardiology (into m/Empty-Queue ["1"])}} "2" {:departments {:cardiology (into m/Empty-Queue ["1" "2"])}}))

  (testing "It should NOT be able to insert a new person in the queue if it's full"
    (doseq [{:keys [description input-hospital expected-output person-to-add]}
            [{:description     "Can't add when reach five people"
              :input-hospital  {:departments {:cardiology (into m/Empty-Queue ["1" "2" "3" "4" "5"])}}
              :expected-output {:departments {:cardiology (into m/Empty-Queue ["1" "2" "3" "4" "5"])}}
              :person-to-add   "6"}

             {:description     "Can't add when reach MORE than five people"
              :input-hospital  {:departments {:cardiology (into m/Empty-Queue ["1" "2" "3" "4" "5" "6"])}}
              :expected-output {:departments {:cardiology (into m/Empty-Queue ["1" "2" "3" "4" "5" "6"])}}
              :person-to-add   "7"}]]
      (testing description
        (is (= expected-output (arrives-at input-hospital :cardiology person-to-add))))))

  (testing "It should NOT be able to insert a new person in the queue if is null"
    (let [hospital        {:departments {:cardiology nil}}
          expected-output {:departments {:cardiology nil}}]
      (is (= expected-output (arrives-at hospital :cardiology "6"))))))
