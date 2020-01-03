(ns parseit.transforms
  (:require
   [parseit.errors :as errors]
   [goog.string]
   [goog.string.format]
   [clojure.string :as str]
   [instaparse.core :as insta :refer-macros [defparser]]))

(defn transform-integer [& s]
  (-> (apply str s)
      (js/parseInt)))

(defn transform-float [& s]
  (-> (apply str s)
      (js/parseFloat)))

(defn transform-uuid [& s]
  (-> (apply str s)
      (uuid)))

(defn add-child-to-map [m child]
  (if (vector? child)
    (case (count child)
      0 m
      1 (assoc m (first child) nil)
      2 (assoc m (first child) (second child))
      (assoc m (first child) (rest child)))
    m))

(defn transform-map [& children]
  (reduce add-child-to-map {} children))

(defn transform-list [& children]
  (reduce conj [] children))

(def transforms
  {:integer {:aliases [:int]
             :desc    "Convert to integer"
             :fn      transform-integer}
   :float   {:aliases [:decimal :number]
             :desc    "Convert to floating point"
             :fn      transform-float}
   :string  {:aliases [:text]
             :desc    "Convert to string"
             :fn      str}
   :map     {:aliases [:dict :object]
             :desc    "Create map from children"
             :fn      transform-map}
   :list    {:aliases [:array :vec]
             :desc    "Create list from children"
             :fn      transform-list}})

(def transform-fns
  (-> (fn [acc [name value]]
        (let [names (conj (:aliases value) name)]
          (-> (fn [acc name]
                (assoc acc name (:fn value)))
              (reduce acc names))))
      (reduce {} transforms)))

(def standard
  {:INTEGER transform-integer
   :STRING  str})

(defn add-transform [transforms [terminal type]]
  (if-let [transform-fn (get transform-fns (keyword type))]
    (assoc transforms (keyword terminal) transform-fn)
    (errors/unknown-transform-type type)))

(defn build-transform [{:keys [options transform] :as state}]
  (let [standard?    (not (:no-standard-tx? options))
        transform    (reduce add-transform transform (:tx options))
        transform-fn (if (empty? transform)
                       identity
                       #(insta/transform transform %))]
    (assoc state :transform-fn transform-fn)))

(defn transform-parsed [{:keys [transform-fn parsed] :as state}]
  (assoc state :parsed (transform-fn parsed)))
