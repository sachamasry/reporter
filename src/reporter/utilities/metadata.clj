(ns reporter.utilities.metadata
  (:require [lein-project-reader.core :refer [read-project]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [reporter.utilities.time :as time])
  (:import [java.net InetAddress]
           [java.lang.management ManagementFactory]))

(defn get-application-metadata
  []
  (if-let [{:keys [name version]}
           (read-project)]
    {:application (str name) :version version}))

(defn get-fossil-commit-hash
  []
  (let [uuid-file-path "./manifest.uuid"]
    (if (.exists (io/file uuid-file-path))
      (str/trim (slurp uuid-file-path))
      "Unknown commit hash"))) ; Return a default message if the file doesn't exist.

(defn generate-attempted-by []
  (let [hostname (.getHostName (InetAddress/getLocalHost))
        runtime-mx-bean (ManagementFactory/getRuntimeMXBean)
        pid-host (.getName runtime-mx-bean)
        jvm-version (.getSpecVersion runtime-mx-bean)
        start-time (.getStartTime runtime-mx-bean)
        fossil-commit-hash (get-fossil-commit-hash)]
    (-> (get-application-metadata)
        (assoc :fossil-commit-hash fossil-commit-hash
               :host hostname
               :pid-host pid-host
               :jvm-version jvm-version
               :start-time start-time
               :timestamp (time/current-datetime)))))
