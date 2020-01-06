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
   ["-S" "--start RULE" "Start processing at this rule"
    :parse-fn #(when-not (str/blank? %)
                 (keyword %))]
   ["-t" "--transform RULE:TYPE" "Transform a rule into a type"
    :id :tx
    :default []
    :parse-fn #(str/split % #":" 2)
    :validate [#(not (str/blank? (first %)))
               "Transform rule is empty"

               #(not (str/blank? (second %)))
               "Transform target empty"

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
   ["-a" "--all" "Return all parses rather than the best match"
    :id :all?]
   ["-s" "--split REGEX" "Process input as a stream, parsing each chunk seperated by REGEX"
    :id :split]
   ["-l" "--split-lines" "Split on newlines, same as --split '(?<=\\r?\\n)'"
    :id :split-lines?]
   ["-e" "--encoding ENCODING" "Use the specified encoding when reading the input, or raw"
    :id :encoding
    :default "utf8"
    :validate [#(not (str/blank? %))
               "Encoding is empty"]]
   ["-X" "--style TYPE" "Build the parsed tree in the style of hiccup or enlive"
    :default "hiccup"
    :parse-fn #(when-not (str/blank? %) (keyword %))
    :validate [#(not (str/blank? %))
               "Style is empty"

               #(contains? parser/styles %)
               "Not a valid style"]]
   ["-h" "--help" "Help"]])


(defn item->help [item-name item]
  (let [names     (concat [item-name] (:aliases item))
        item-name (->> names
                       (map name)
                       (sort)
                       (str/join ", "))]
    [item-name (:desc item)]))


(defn print-help-table [items]
  (let [items   (map #(apply item->help %) items)
        longest (->> (map #(-> % first count) items)
                     (apply max))
        fmt     (str "  %-" longest "s  %s")]
    (doseq [item (sort items)]
      (printf fmt (first item) (second item)))))


(defn transform-types-help []
  (print "Transformation Types")
  (print-help-table transforms/transforms))

(defn output-formats-help []
  (print "Output Formats")
  (doseq [format (sort (keys outputs/output-fns))]
    (printf "  %s" (name format))))

(defn presets-help []
  (print "Presets")
  (print-help-table presets/presets))

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
  (presets-help)
  (process/exit 0))

(defn handle-opts-normal [options arguments]
  (let [{:keys [parse-fn transform-fn output-fn options arguments] :as state}
        (-> {:options options :arguments arguments}
            (presets/load-preset)
            (parser/load-grammar)
            (parser/build-parser)
            (transforms/build-transform)
            (outputs/build-output))

        process-fn #(-> % parse-fn transform-fn output-fn)
        file       (or (first arguments) "-")
        encoding   (:encoding options)
        done-fn    #(process/exit 0)
        split      (some-> (if (:split-lines? options)
                             "(?<=\r?\n)"
                             (:split options))
                           (re-pattern))]
    (if split
      (slurp/read-file-split file split encoding process-fn done-fn)
      (slurp/read-file file encoding process-fn done-fn))))

(defn handle-opts [{:keys [options arguments summary errors]}]
  (cond
    errors          (errors/parsing-options errors summary)
    (:help options) (handle-opts-help summary)
    :else           (handle-opts-normal options arguments)))

(defn main [& args]
  (-> (parse-opts args cli-options)
      (handle-opts)))
