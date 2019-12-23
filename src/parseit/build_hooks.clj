(ns parseit.build-hooks
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(defn exec [cmd]
  (let [{:keys [exit out err]} (apply sh cmd)]
    (if (zero? exit)
      (when-not (str/blank? out)
        (println out))
      (println err))))

(defn nexe
  {:shadow.build/stage :flush}
  [state src dest]
  (case (:shadow.build/mode state)
    :release
    (let [cmd ["nexe" "--output" dest src]]
      (print (fmt "Executing: %s" cmd))
      (exec cmd))

    true)
  state)
