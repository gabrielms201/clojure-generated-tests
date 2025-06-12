(ns clojure-property-based-testing.models
  (:require
   [schema.core :as s])

  (:import
   [clojure.lang PersistentQueue]))

(s/set-fn-validation! true)

(def Empty-Queue PersistentQueue/EMPTY)

(s/defschema Departments (s/enum :cardiology :neurology :pediatrics))
(s/defschema Department
  {Departments (s/maybe (s/queue s/Str))})

(s/defschema Hospital {(s/required-key :departments) Department})

