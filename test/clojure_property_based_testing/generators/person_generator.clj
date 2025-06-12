(ns clojure-property-based-testing.generators.person-generator
  (:require
   [clojure.string :as string]
   [clojure.test.check.generators :as gen]))

(def pname
  (->>
   (gen/vector gen/char-alphanumeric 3 5)
   (gen/fmap string/join)))

