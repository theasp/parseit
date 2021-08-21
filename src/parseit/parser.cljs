(ns parseit.parser
  (:require
   [parseit.misc :as misc]
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



(defn some-second? [d]
  (some? (second d)))

(defn build-parser [{:keys [grammar options parser] :as state}]
  (let [style-name (get-in state [:options :style])]
    (if-let [style (get styles style-name)]
      (let [parser     (or parser (insta/parser grammar :output-format style))
            parse-opts (->>  [:start :partial :total :unhide :optimize]
                             (select-keys options)
                             (filter some-second?)
                             (apply concat))
            parse      (if (:all? options)
                         insta/parses
                         insta/parse)
            parse-fn   (fn [input]
                         (let [parsed (apply parse parser input :optimize :memory parse-opts)]
                           (if (insta/failure? parsed)
                             (errors/unable-to-parse parsed)
                             parsed)))]
        (assoc state :parse-fn parse-fn))
      (errors/invalid-style style-name))))

(defn load-grammar-file [state]
  (if-let [file (-> state :arguments first)]
    (-> state
        (assoc :grammar-file file)
        (assoc :grammar (slurp/slurp-file file "utf8"))
        (update :arguments rest)
        (build-parser))
    (errors/missing-argument-grammar-file)))

(defn load-grammar [state]
  (if (or (:grammar state) (:parser state))
    state
    (load-grammar-file state)))
