(ns clojure-property-based-testing.logic
  (:require
   [clojure-property-based-testing.models :as m]
   [schema.core :as s]))

(s/set-fn-validation! true)

(s/defn fits-queue?
  [{:keys [departments]} :- m/Hospital
   department :- m/Departments]
  (boolean
   (some-> (department departments)
           count
           (< 5))))

(s/defn arrives-at :- m/Hospital
  [hospital :- m/Hospital
   department :- m/Departments
   person :- s/Str]
  (if (fits-queue? hospital department)
    (update-in hospital [:departments department] conj person)
    hospital))