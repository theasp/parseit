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
                     {:hosts transforms/transform-list
                      :host  transforms/transform-map
                      :ipv4  str
                      :ipv6  str
                      :WORD  str})})

(def presets
  {:csv    csv-preset
   :passwd passwd-preset
   :group  group-preset
   :hosts  hosts-preset})

(defn load-preset [{:keys [options] :as state}]
  (if-let [preset-name (-> state :options :preset)]
    (if-let [preset (get presets preset-name false)]
      (if preset
        (merge state preset)
        (errors/invalid-preset preset-name)))
    (merge state {:transform (when-not (:no-standard-tx? options) transforms/standard)})))
