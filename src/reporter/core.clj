(ns reporter.core
  "Core functionality for Reporter, defining configuration, dependencies
  needed for interfacing with the Java Reports library, and prototype code
  demonstrating a working report-generating solution."

  (:require [reporter.db :refer [get-db-specification get-report-job change-state-to-executing]]
            [reporter.reports.template-memoisation :refer [get-template-path]]
            [reporter.reports.jasper-reports :refer [process-report]]
            [reporter.reports.template-memoisation
             :refer [get-compiled-template get-template-path]]
            [reporter.reports.jasper-reports :refer [generate-and-store-report]]
            ;; [reporter.reports.report-export-memoisation :refer [get-generated-report]]
            ))

(defn safe-execute [f]
  (try
    (f)
    (catch Exception e
      (println "Error processing report:" (.getMessage e))
      (System/exit 1))))

(defn execute-job [db-path job-id]
  (let [db-specification (get-db-specification db-path)
        job (get-report-job job-id db-specification)]
    (if job
      (let [template-path (get-template-path db-specification job)
            temporary-data-tables (:temporary_tables_created job)]
        (println (str "=> Working on job #'" job-id "'..."))
        (change-state-to-executing job-id db-specification)
        (generate-and-store-report template-path job-id temporary-data-tables db-specification))
      (println "=> Job ID not found:" job-id))))

(defn -main
  [& args]
  (let [[db-path job-id] args]
    (if (and db-path job-id)
      (safe-execute #(execute-job db-path job-id))
      (println "Usage: reporter <db-path> <job-id>"))))

;; (defn -main [& args]
;;   (safe-execute #(process-report (first args) (second args)))
;;   (System/exit 0))
