(ns parseit.errors
  (:require
   [parseit.misc :as misc :refer [printf printf-stderr print-stderr]]
   ["process" :as process :refer [exit]]))

(defn invalid-preset [preset]
  (printf-stderr "ERROR: Unknown preset '%s'" preset)
  (exit 1))

(defn unknown-format [output]
  (printf-stderr "ERRROR: Unknown format '%s'" output)
  (exit 1))

(defn missing-argument-grammar-file []
  (printf-stderr "ERROR: You must specify a grammar file or use --preset")
  (exit 1))

(defn invalid-style [style-name]
  (printf-stderr "ERROR: Unknown style '%s'" style-name)
  (exit 1))

(defn unable-to-parse [err]
  (print-stderr (pr-str err))
  (exit 1))

(defn unknown-transform-type [type]
  (printf-stderr "Unknown transform type '%s'" type)
  (exit 1))

(defn parsing-options [errors summary]
  (print-stderr "ERROR: Problems parsing command line options:")
  (doseq [error errors]
    (printf-stderr "  %s" error))
  (exit 1))

(defn unable-to-read-file [err]
  (print-stderr (.-message err))
  (exit 1))
