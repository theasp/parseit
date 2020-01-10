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

(def output-fns
  {:edn             edn-output
   :edn-pretty      edn-pretty-output
   :json            json-output
   :json-pretty     json-pretty-output
   :transit         transit-output
   :transit-verbose transit-verbose-output
   :yaml            yaml-output})

(defn output-parsed [{:keys [output-fn parsed] :as state}]
  (output-fn parsed))

(defn build-output [{:keys [options] :as state}]
  (let [format    (get options :format)
        output-fn (get output-fns format)]
    (if output-fn
      (assoc state :output-fn output-fn)
      (errors/unknown-format format))))
