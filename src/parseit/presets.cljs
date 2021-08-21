(ns parseit.presets
  (:require
   [parseit.errors :as errors]
   [parseit.transforms
    :as transforms
    :refer [wrap-type
            transform-map
            transform-list
            transform-merge
            transform-map-kv
            transform-first
            transform-integer
            transform-hex-to-int
            transform-int-to-hex]]
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
                     {:user   transform-map
                      :passwd transform-list})})

(defparser group-parser "group.ebnf")
(def group-preset
  {:desc      "NSS group(5), i.e. /etc/group"
   :parser    group-parser
   :transform (merge transforms/standard
                     {:group  transform-map
                      :groups transform-list
                      :user   str})})

(defparser hosts-parser "hosts.ebnf")
(def hosts-preset
  {:desc      "NSS hosts(5), i.e. /etc/hosts"
   :parser    hosts-parser
   :transform (merge transforms/standard
                     {:hosts            transform-list
                      :host             transform-map
                      :hostname_segment str
                      :hostname         str
                      :ipv4             str
                      :ipv6             str})})

(defparser hpl-parser "hpl.ebnf")
(def hpl-preset
  {:desc      "High Performance Linpack benchmark results"
   :parser    hpl-parser
   :transform (merge transforms/standard
                     {:hpl            transform-map
                      :authors        (wrap-type transform-list :authors)
                      :header         (wrap-type transform-map :header)
                      :result_timings (wrap-type transform-merge :result_timings)
                      :results        (wrap-type transform-map :results)
                      :result         transform-map
                      :WORD           str
                      :settings_body  transform-merge
                      :kv_item        transform-map-kv
                      :kv_key         transform-first
                      :kv_value       transform-first})})

(defparser ibnetdiscover-parser "ibnetdiscover.ebnf")
(def ibnetdiscover-preset
  {:desc      "Infiniband ibnetdiscover output"
   :parser    ibnetdiscover-parser
   :transform (merge transforms/standard
                     {:ibnetdiscover transform-map
                      :header        (wrap-type transform-merge :header)
                      :initiated     transform-map
                      :generated     transform-map
                      :node          transform-map
                      :port          transform-map})})

(defparser ibdiagnet-lst-parser "ibdiagnet-lst.ebnf")

(def ibdiagnet-lst-preset
  {:desc      "Infiniband ibdiagnet.lst"
   :parser    ibdiagnet-lst-parser
   :transform (merge transforms/standard
                     {:port          (wrap-type transform-hex-to-int :port)
                      :lid           (wrap-type transform-hex-to-int :lid)
                      :ports         (wrap-type transform-hex-to-int :ports)
                      :versions      transform-merge
                      :version_item  transform-map-kv
                      :version_name  str
                      :version_value str
                      :header        (wrap-type transform-map :header)
                      :conn          (wrap-type transform-map :conn)
                      :links         (wrap-type transform-list :links)
                      :link          transform-map
                      :node          transform-map
                      :ibdiagnet_lst transform-map})})

(def presets
  {:csv           csv-preset
   :passwd        passwd-preset
   :group         group-preset
   :hosts         hosts-preset
   :hpl           hpl-preset
   :ibnetdiscover ibnetdiscover-preset
   :ibdiagnet-lst ibdiagnet-lst-preset})

(defn load-preset [{:keys [options] :as state}]
  (if-let [preset-name (-> state :options :preset)]
    (let [preset (get presets preset-name false)]
      (if preset
        (merge state preset)
        (errors/invalid-preset (name preset-name))))
    (merge state {:transform (when-not (:no-standard-tx? options) transforms/standard)})))

