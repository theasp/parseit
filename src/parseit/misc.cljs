(ns parseit.misc
  (:require
   ["console" :as console]
   [goog.string]
   [goog.string.format]))

(defn print-stderr [s]
  (console/error s))

(def fmt js/goog.string.format)

(defn printf [f & args]
  (-> (apply fmt f args)
      (print)))

(defn printf-stderr [f & args]
  (-> (apply fmt f args)
      (print-stderr)))

