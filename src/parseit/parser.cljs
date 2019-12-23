(ns parseit.parser
  (:require
   [parseit.misc :as misc]
   [parseit.presets :as presets]
   [parseit.slurp :as slurp]
   [parseit.errors :as errors]
   [instaparse.core :as insta :refer-macros [defparser]]))

(def styles
  {nil      :hiccup
   ""       :hiccup
   "hiccup" :hiccup
   "tree"   :hiccup
   "enlive" :enlive
   "list"   :enlive})

(defn build-parser [{:keys [grammar] :as state}]
  (let [style-name (get-in state [:options :style])
        style      (get styles style-name)]
    (if style
      (-> state
          (assoc :parser (insta/parser grammar :output-format style))
          (dissoc :grammar))
      (errors/invalid-style style-name))))

(defn load-grammar-file [state]
  (if-let [file (-> state :arguments first)]
    (-> state
        (assoc :grammar-file file)
        (assoc :grammar (slurp/slurp-file file))
        (update :arguments rest)
        (build-parser))
    (errors/missing-argument-grammar-file)))

(defn load-grammar [state]
  (if (-> state :options :preset)
    (presets/load-preset state)
    (load-grammar-file state)))

(defn load-input [{:keys [arguments] :as state}]
  (if-let [file (first arguments)]
    (assoc state :input (slurp/slurp-file file))
    (assoc state :input (slurp/slurp-file 0))))

(defn some-second? [d]
  (some? (second d)))

(defn parse-input [{:keys [parser input options] :as state}]
  (let [parse-opts (->>  [:start :partial :total :unhide :optimize]
                         (select-keys options)
                         (filter some-second?)
                         (apply concat))
        parsed     (apply insta/parse parser input parse-opts)]
    (if (insta/failure? parsed)
      (errors/unable-to-parse parsed)
      (-> state
          (dissoc :input)
          (assoc :parsed parsed)))))


