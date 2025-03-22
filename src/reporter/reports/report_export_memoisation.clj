(ns reporter.reports.report-export-memoisation
  (:require [clojure.java.jdbc :as jdbc]
            [reporter.db :refer [get-db-specification]]
            [reporter.utilities.time :refer [current-datetime]]))

(defn get-memoised-report [db-spec job-id]
  (first (jdbc/query get-db-specification
    ["SELECT output_blob FROM report_export_memoisation WHERE job_id = ?" job-id])))
