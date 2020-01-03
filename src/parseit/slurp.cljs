(ns parseit.slurp
  (:require
   ["fs" :as fs]
   ["process" :as process]
   ["split2" :as split2]
   [parseit.errors :as errors]))

(defn slurp-file [file encoding]
  (let [encoding (when (and encoding (not= :raw (keyword encoding)))
                   (name encoding))]
    (try
      (if (= file "-")
        (fs/readFileSync 0 encoding)
        (fs/readFileSync file encoding))
      (catch js/Object e
        (errors/unable-to-read-file e)))))

(defn read-stream-split [stream regex line-fn done-fn]
  (-> stream
      (.pipe (split2 regex nil nil))
      (.on "data" line-fn)
      (.on "end" done-fn)))

(defn read-stream [stream data-fn done-fn]
  (-> stream
      (.on "data" data-fn)
      (.on "end" done-fn)))

(defn create-read-stream [file encoding]
  (let [encoding (when (and encoding (not= :raw (keyword encoding)))
                   (name encoding))]
    (if (= file "-")
      (.setEncoding process/stdin encoding)
      (fs/createReadStream file #js {:encoding  encoding
                                     :emitClose true}))))

(defn read-file-split [file regex encoding line-fn done-fn]
  (try
    (-> (create-read-stream file encoding)
        (read-stream-split regex line-fn done-fn))
    (catch js/Object e
      (errors/unable-to-read-file e))))


(defn read-file [file encoding data-fn done-fn]
  (try
    (-> (create-read-stream file encoding)
        (read-stream data-fn done-fn))
    (catch js/Object e
      (errors/unable-to-read-file e))))
