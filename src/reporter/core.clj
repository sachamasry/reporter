(ns reporter.core
  "Core functionality for Reporter, defining configuration, dependencies
  needed for interfacing with the Java Reports library, and prototype code
  demonstrating a working report-generating solution."

  (:require [reporter.db :refer [get-db-specification get-report-job]]
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
        job (get-report-job db-specification job-id)]
    (if job
      (let [template-path (get-template-path db-specification job)]
        (generate-and-store-report db-specification job template-path))
      (println "Job ID not found:" job-id))))

(defn -main
  [& args]
  (let [[db-path job-id] args]
    (if (and db-path job-id)
      (safe-execute #(execute-job db-path job-id))
      (println "Usage: reporter <db-path> <job-id>"))))

;; (defn -main [& args]
;;   (safe-execute #(process-report (first args) (second args)))
;;   (System/exit 0))
