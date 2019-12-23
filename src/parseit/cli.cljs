(ns parseit.cli
  (:require
   [parseit.misc :as misc :refer [printf printf-stderr print-stderr fmt]]
   [parseit.slurp :as slurp]
   [parseit.parser :as parser]
   [parseit.errors :as errors]
   [parseit.transforms :as transforms]
   [parseit.presets :as presets]
   [parseit.outputs :as outputs]
   ["process" :as process]
   [clojure.string :as str]
   [clojure.tools.cli :refer [parse-opts]]))


(def cli-options
  ;; An option with a required argument
  [["-p" "--preset PRESET" "Preset grammar to use"
    :id :preset
    :default nil
    :parse-fn #(when-not (or (str/blank? %)
                             (= "none" %))
                 (keyword %))
    :default-desc "none"]
   ["-f" "--format FORMAT" "Select the output format"
    :id :format
    :default :json
    :default-desc "json"
    :parse-fn #(when-not (str/blank? %)
                 (keyword %))]
   ["-s" "--start RULE" "Start processing at this rule"
    :parse-fn #(when-not (str/blank? %)
                 (keyword %))]
   ["-t" "--transform RULE:TYPE" "Transform a rule into a type"
    :id :tx
    :default []
    :parse-fn #(str/split % #":" 2)
    :validate [#(not (str/blank? (first %)))
               "Transform rule missing"

               #(not (str/blank? (second %)))
               "Transform target missing"

               #(->> (second %)
                     (keyword)
                     (get transforms/transform-fns)
                     (some?))
               "Unknown transform type"]
    :default-desc ""
    :assoc-fn (fn [options id value]
                (-> options
                    (update :tx conj value)))]
   ["-T" "--no-standard-tx" "Do use the standard transformations"
    :id :no-standard-tx?]
   ["-S" "--style TYPE" "Build the parsed tree in the style of hiccup or enlive"
    :default "hiccup"
    :parse-fn #(when-not (str/blank? %) %)]
   ["-h" "--help" "Help"]])

(defn transform-types-help []
  (print "Transformation Types")
  (doseq [type (sort (keys transforms/transforms))]
    (let [{:keys [desc aliases]} (get transforms/transforms type)
          names                  (->> (conj aliases type)
                                      (map name)
                                      (sort)
                                      (str/join ", "))]
      (printf "  %s\n    %s" names desc))))

(defn output-formats-help []
  (print "Output Formats")
  (doseq [format (sort (keys outputs/output-fns))]
    (printf "  %s" (name format))))

(defn presets-help []
  (print "Presets")
  (doseq [preset (sort (keys presets/presets))]
    (when (get presets/presets preset)
      (printf "  %s" (name preset)))))

(defn handle-opts-help [summary]
  (printf "Usage: %s [options] <grammar> [input]" "parseit")
  (printf "       %s [options] --preset <preset> [input]" "parseit")
  (printf "       %s --help" "parseit")
  (print "")
  (print "Options")
  (print summary)
  (print "")
  (output-formats-help)
  (print "")
  (transform-types-help)
  (print "")
  (presets-help))

(defn handle-opts-normal [options arguments]
  (-> {:options options :arguments arguments}
      (transforms/build-transform)
      (parser/load-grammar)
      (parser/load-input)
      (parser/parse-input)
      (transforms/transform-parsed)
      (outputs/output-parsed)))

(defn handle-opts [{:keys [options arguments summary errors]}]
  (cond
    errors          (errors/parsing-options errors summary)
    (:help options) (handle-opts-help summary)
    :else           (handle-opts-normal options arguments)))

(defn main [& args]
  (-> (parse-opts args cli-options)
      (handle-opts))
  (process/exit 0))
