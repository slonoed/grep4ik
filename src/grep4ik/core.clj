(ns grep4ik.core
  (:require
    [clojure.java.io :as io]
    [uap-clj.core :as u]
    [clojure.core.async :as a])
  (:use [amazonica.aws.s3]
        [amazonica.core :refer [defcredential]])
  (:gen-class))

(defcredential
  {:access-key ""
   :secret-key ""
   :endpoint   "eu-west-1"})

(def acc-id 668599862092)
(def region "eu-west-1")
(def bucket "flock-logs")
(def prefix (str "AWSLogs/" acc-id "/elasticloadbalancing/" region "/2016/11/02"))
(def browsers (atom {}))

(defn get-list
  ([bucket prefix] (get-list bucket prefix nil))
  ([bucket prefix marker]
    (amazonica.aws.s3/list-objects :bucket-name bucket
                                   :marget marker
                                   :prefix prefix)))
(defn get-one
  [bucket k]
  (-> (amazonica.aws.s3/get-object :bucket-name bucket
                                   :key k)))

(defn log->ua [l] (-> (re-seq #"\"(.*?)\"" l)
                      second
                      second
                      u/useragent))


(def max-go 20)
(defn logs-list []
  (let [ch (a/chan max-go)]
    (a/go-loop [marker nil]
             (let [res (get-list bucket prefix)
                   {:keys [truncated? next-marker object-summaries]} res
                   ks (map :key object-summaries)]
               (doseq [k ks] (a/>! ch k))
               (when truncated? (recur next-marker))))
    ch))

(defn add-browser! [{b :browser}]
  (swap! browsers (fn [bs]
                    (update bs
                            (str (:family b) " " (:major b))
                            (fnil inc 0)))))



(defn slurp-link [link]
  (let [lines (-> (get-one bucket link)
                  :input-stream
                  io/reader
                  line-seq)]
      (doseq [s lines]
        (let [ua (log->ua s)]
          (add-browser! ua)))))

(def success-count (atom 0))
(def error-count (atom 0))
(defn handle-keys [ch]
  (a/go-loop [k (a/<! ch)]
           (when k
             (a/<! (a/timeout 300))
             (a/thread (try
                         (do
                           (slurp-link k)
                           (swap! success-count inc))
                         (catch Exception e
                           (swap! error-count inc)
                           )))
             (recur (a/<! ch)))))

(defn st []
  (handle-keys (logs-list)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

; {:prefix AWSLogs/668599862092/elasticloadbalancing/eu-west-1/2016/11/02, :truncated? true, :bucket-name flock-logs, :max-keys 1000, :next-marker AWSLogs/668599862092/elasticloadbalancing/eu-west-1/2016/11/02/668599862092_elasticloadbalancing_eu-west-1_main_20161102T1000Z_52.31.223.161_qnlkimju.log, :common-prefixes [], :object-summaries [{:key AWSLogs/668599862092/elasticloadbalancing/eu-west-1/2016/11/02/668599862092_elasticloadbalancing_eu-west-1_main_20161102T0000Z_52.18.164.14_10bv15x1.log, :size 39438283, :owner {:id c81fd8ca74cac685e10fd2b8435ebc17a038a17714e3e6db8ccd2f63e7637d84, :display-name elblogdelivery-eu-west-1}, :last-modified #object[org.joda.time.DateTime 0x6ad3fb98 2016-11-02T02:34:14.000+03:00], :bucket-name flock-logs, :etag 24e698b96c23f34dcfefc589a5febaee-8, :storage-class STANDARD} {:key AWSLogs/668599862092/elasticloadbalancing/eu-west-1/2016/11/02/668599862092_elasticloadbalancing_eu-west-1_main_20161102T0000Z_52.18.164.14_16w1uph5.log, :size 39526094, :owner {:id c81fd8ca74cac685e10fd2b8435ebc17a038a17714e3e6db8ccd2f63e7637d84, :display-name elblogdelive}}]}
(def link "AWSLogs/668599862092/elasticloadbalancing/eu-west-1/2016/11/02/668599862092_elasticloadbalancing_eu-west-1_main_20161102T0000Z_52.18.164.14_4592hgsf.log")
(def ua "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0_2 like Mac OS X) AppleWebKit/602.1.50 (KHTML, like Gecko) Version/10.0 Mobile/14A456 Safari/602.1")

