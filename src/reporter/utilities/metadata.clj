(ns reporter.utilities.metadata
  (:require [lein-project-reader.core :refer [read-project]]
            [clojure.java.shell :as sh]
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
  (let [result (sh/sh "fossil" "info" "--hash")]
    (:out result)))  ;; This will return the commit hash

(defn generate-attempted-by []
  (let [hostname (.getHostName (InetAddress/getLocalHost))
        runtime-mx-bean (ManagementFactory/getRuntimeMXBean)
        pid-host (.getName runtime-mx-bean)
        jvm-version (.getSpecVersion runtime-mx-bean)
        start-time (.getStartTime runtime-mx-bean)]
    (-> (get-application-metadata)
        (assoc :host hostname
               :pid-host pid-host
               :jvm-version jvm-version
               :start-time start-time
               :timestamp (time/current-datetime)))))
