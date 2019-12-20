(ns parseit.outputs
  (:require
   ["js-yaml" :as yaml]
   [parseit.errors :as errors]
   [cognitect.transit :as transit]))

(defn edn-output [data]
  (pr data))

(defn json-output [data]
  (-> (clj->js data)
      (js/JSON.stringify)
      (print)))

(defn transit-output [data]
  (-> (transit/writer :json)
      (transit/write data)
      (print)))

(defn yaml-output [data]
  (print "---")
  (-> (clj->js data)
      (yaml/safeDump)
      (print)))

(def output-fns
  {"edn"     edn-output
   "json"    json-output
   "transit" transit-output
   "yaml"    yaml-output})

(defn output-parsed [{:keys [parsed options] :as state}]
  (let [format    (get options :format)
        output-fn (get output-fns format)]
    (if output-fn
      (output-fn parsed)
      (errors/unknown-format format))))
