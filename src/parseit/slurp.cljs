(ns parseit.slurp
  (:require
   ["fs" :as fs]
   [parseit.errors :as errors]))

(defn slurp-file [file]
  (try
    (if (= file "-")
      (fs/readFileSync 0 "utf8")
      (fs/readFileSync file "utf8"))
    (catch js/Object e
      (errors/unable-to-read-file e))))
