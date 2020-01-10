(ns parseit.outputs
  (:require
   ["js-yaml" :as yaml]
   [cljs.pprint :as pprint]
   [parseit.errors :as errors]
   [cognitect.transit :as transit]))

(defn edn-output [data]
  (pr data))

(defn edn-pretty-output [data]
  (pprint/pprint data))

(defn json-output [data]
  (-> (clj->js data)
      (js/JSON.stringify)
      (print)))

(defn json-pretty-output [data]
  (-> (clj->js data)
      (js/JSON.stringify nil 2)
      (print)))

(defn transit-output [data]
  (-> (transit/writer :json)
      (transit/write data)
      (print)))

(defn transit-verbose-output [data]
  (-> (transit/writer :json-verbose)
      (transit/write data)
      (print)))

(defn yaml-output [data]
  (print "---")
  (-> (clj->js data)
      (yaml/safeDump)
      (print)))

(def outputs
  {:edn             {:desc "Extensible Data Format"
                     :fn   edn-output}
   :edn-pretty      {:desc "Extensible Data Format (pretty)"
                     :fn   edn-pretty-output}
   :json            {:desc "JavaScript Object Notation"
                     :fn   json-output}
   :json-pretty     {:desc "JavaScript Object Notation (pretty)"
                     :fn   json-pretty-output}
   :transit         {:desc "Transit JSON"
                     :fn   transit-output}
   :transit-verbose {:desc "Transit JSON Verbose"
                     :fn   transit-verbose-output}
   :yaml            {:desc "YAML Ain't Markup Language"
                     :fn   yaml-output}})

(def output-fns
  (-> (fn [acc [type value]]
        (let [types (conj (:aliases value) type)
              f     (:fn value)]
          (-> (fn [acc type]
                (assoc acc type f))
              (reduce acc types))))
      (reduce {} outputs)))

(defn output-parsed [{:keys [output-fn parsed] :as state}]
  (output-fn parsed))

(defn build-output [{:keys [options] :as state}]
  (let [format    (get options :format)
        output-fn (get output-fns format)]
    (if output-fn
      (assoc state :output-fn output-fn)
      (errors/unknown-format format))))
