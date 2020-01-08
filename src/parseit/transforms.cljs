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

(defn transform-nil [& s]
  nil)

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
   :float   {:aliases [:decimal :number :double]
             :desc    "Convert to floating point"
             :fn      transform-float}
   :string  {:aliases [:text :str]
             :desc    "Convert to string"
             :fn      str}
   :map     {:aliases [:dict :object]
             :desc    "Create map from children"
             :fn      transform-map}
   :list    {:aliases [:array :vec]
             :desc    "Create list from children"
             :fn      transform-list}
   :nil     {:aliases [:null]
             :desc    "Convert to nil"
             :fn      transform-nil}})

(def transform-fns
  (-> (fn [acc [type value]]
        (let [types (conj (:aliases value) type)
              f     (:fn value)]
          (-> (fn [acc type]
                (let [type+ (-> (name type) (str "+") (keyword))]
                  (-> acc
                      (assoc type f)
                      (assoc type+ f))))
              (reduce acc types))))
      (reduce {} transforms)))

(def standard
  {:INTEGER transform-integer
   :NUMBER  transform-float
   :FLOAT   transform-float
   :STRING  str})

(defn type-valid? [type]
  (contains? transform-fns (keyword type)))

(defn type-wrap? [type]
  (-> (name type)
      (last)
      (= "+")))

(defn wrap-type [f name]
  (fn [& s]
    [name (apply f s)]))

(defn add-transform [transforms [rule type]]
  (if-let [transform-fn (get transform-fns (keyword type))]
    (if (type-wrap? type)
      (assoc transforms (keyword rule) (wrap-type transform-fn rule))
      (assoc transforms (keyword rule) transform-fn))
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
