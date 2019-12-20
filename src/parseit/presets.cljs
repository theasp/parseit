(ns parseit.presets
  (:require
   [parseit.errors :as errors]
   [parseit.transforms :as transforms]
   [instaparse.core :as insta :refer-macros [defparser]]
   [clojure.string :as str]))

(defparser csv-parser "csv.ebnf")
(def csv-preset
  {:parser csv-parser})


(defparser passwd-parser "passwd.ebnf")
(def passwd-preset
  {:parser    passwd-parser
   :transform (merge transforms/standard
                     {:user   transforms/transform-map
                      :passwd transforms/transform-list})})

(def presets
  {""       nil
   nil      nil
   "csv"    csv-preset
   "passwd" passwd-preset})

(defn load-preset [state]
  (let [preset-name (-> state :options :preset)
        preset      (get presets preset-name false)]
    (if preset
      (merge state preset)
      (errors/invalid-preset preset-name))))
