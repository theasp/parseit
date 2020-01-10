(ns parseit.presets
  (:require
   [parseit.errors :as errors]
   [parseit.transforms :as transforms]
   [instaparse.core :as insta :refer-macros [defparser]]
   [clojure.string :as str]))

(defparser csv-parser "csv.ebnf")
(def csv-preset
  {:parser    csv-parser
   :transform transforms/standard
   :desc      "Comma Seperated Value"})


(defparser passwd-parser "passwd.ebnf")
(def passwd-preset
  {:desc      "NSS passwd(5), i.e. /etc/passwd"
   :parser    passwd-parser
   :transform (merge transforms/standard
                     {:user   transforms/transform-map
                      :passwd transforms/transform-list})})

(defparser group-parser "group.ebnf")
(def group-preset
  {:desc      "NSS group(5), i.e. /etc/group"
   :parser    group-parser
   :transform (merge transforms/standard
                     {:group  transforms/transform-map
                      :groups transforms/transform-list
                      :user   str})})

(defparser hosts-parser "hosts.ebnf")
(def hosts-preset
  {:desc      "NSS hosts(5), i.e. /etc/hosts"
   :parser    hosts-parser
   :transform (merge transforms/standard
                     {:hosts            transforms/transform-list
                      :host             transforms/transform-map
                      :hostname_segment str
                      :hostname         str
                      :ipv4             str
                      :ipv6             str})})

(defparser hpl-parser "hpl.ebnf")
(def hpl-preset
  {:desc      "High Performance Linpack benchmark results"
   :parser    hpl-parser
   :transform (merge transforms/standard
                     {:hpl            transforms/transform-map
                      :authors        (transforms/wrap-type transforms/transform-list :authors)
                      :header         (transforms/wrap-type transforms/transform-map :header)
                      :result_timings (transforms/wrap-type transforms/transform-merge :result_timings)
                      :results        (transforms/wrap-type transforms/transform-map :results)
                      :result         transforms/transform-map
                      :WORD           str
                      :settings_body  transforms/transform-merge
                      :kv_item        transforms/transform-map-kv
                      :kv_key         transforms/transform-first
                      :kv_value       transforms/transform-first})})

(def presets
  {:csv    csv-preset
   :passwd passwd-preset
   :group  group-preset
   :hosts  hosts-preset
   :hpl    hpl-preset})

(defn load-preset [{:keys [options] :as state}]
  (if-let [preset-name (-> state :options :preset)]
    (if-let [preset (get presets preset-name false)]
      (if preset
        (merge state preset)
        (errors/invalid-preset preset-name)))
    (merge state {:transform (when-not (:no-standard-tx? options) transforms/standard)})))
