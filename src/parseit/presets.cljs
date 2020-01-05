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

(defparser group-parser "group.ebnf")
(def group-preset
  {:parser    group-parser
   :transform (merge transforms/standard
                     {:group  transforms/transform-map
                      :groups transforms/transform-list
                      :user   str})})

(def presets
  {:csv    csv-preset
   :passwd passwd-preset
   :group  group-preset})

(defn load-preset [{:keys [options] :as state}]
  (if-let [preset-name (-> state :options :preset)]
    (if-let [preset (get presets preset-name false)]
      (if preset
        (merge state preset)
        (errors/invalid-preset preset-name)))
    (merge state {:transform (when-not (:no-standard-tx? options) transforms/standard)})))
