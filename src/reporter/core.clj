(ns reporter.core
  "Core functionality for Reporter, defining configuration, dependencies
  needed for interfacing with the Java Reports library, and prototype code
  demonstrating a working report-generating solution."

  (:gen-class)
  (:require [reporter.db :refer [get-db-specification get-report-job change-state-to-executing]]
            [reporter.reports.template-memoisation :refer [get-template-path]]
            [reporter.reports.jasper-reports :refer [process-report]]
            [reporter.reports.template-memoisation
             :refer [get-compiled-template get-template-path]]
            [reporter.reports.jasper-reports :refer [generate-and-store-report]]
            [clojure.tools.logging :as log]
            [reporter.utilities.logging :refer [log-error log-warn log-info log-debug log-trace with-log-step]]
            ;; [reporter.reports.report-export-memoisation :refer [get-generated-report]]
            ))

(defn safe-execute [f]
  (try
    (f)
    (catch Exception e
      (log-error "->" (str "Error processing report: " (.getMessage e)))
      (.printStackTrace e)
      (System/exit 1))))

(defn execute-job
  [db-path job-id]
  (log-info "-->" (str "Executing job" job-id " on database " db-path))
  (try
    (let [db-specification (get-db-specification db-path)
          job (get-report-job job-id db-specification)]
      (log-trace "-->" (str "DB specification:" db-specification))
      (log-trace "-->" (str "Job report:" job))
      (if job
        (with-log-step "-->" "Generating report"
          (let [template-path (get-template-path db-specification job)
                temporary-data-tables (:temporary_tables_created job)]
            (log-info "-->" (str "Updating state of job #'" job-id "' to executing"))
            (change-state-to-executing job-id db-specification)
            (generate-and-store-report template-path job-id temporary-data-tables db-specification)
            (log-info "-->" (str "Job completed: " job-id))))
        (log-error "-->" (str "Job ID not found:" job-id))))
    (catch Exception e
      (log-error "-->" (str "Report execution failed for job: " job-id " with error " e)))))

(defn -main
  [& args]
  (let [[command db-path job-id] args]
    (case command
      "execute-job" (if (and db-path job-id)
                      (do
                        (log-info "->" (str "Received job execution request: db=" db-path " job id=" job-id))
                        (safe-execute #(execute-job db-path job-id)))
                      (log-error "->" "️Usage: reporter execute-job <db-path> <job-id>"))
      (log-error "->" (str "Unknown command:" command)))))

;; (defn -main [& args]
;;   (safe-execute #(process-report (first args) (second args)))
;;   (System/exit 0))
