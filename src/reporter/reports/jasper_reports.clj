(ns reporter.reports.jasper-reports

  (:require [reporter.db
             :refer [get-db-specification
                     get-db-connection
                     get-report-job
                     complete-job-and-store-report]]
            [reporter.reports.template-memoisation
             :refer [get-template-path get-compiled-template]]
            [reporter.reports.report-export :refer [export-to-pdf-blob]]
            [reporter.reports.report-export-memoisation :refer [get-memoised-report]]
            [reporter.utilities.benchmarking :refer [time-execution]]
            [reporter.utilities.json :refer [parse-json]])

  (:import [net.sf.jasperreports.engine
            JasperFillManager]))

(defn fill-report
  "Fills a compiled Jasper report with data"
  [compiled-report db-specification data-table-name]
  (with-open [db-connection (get-db-connection db-specification)]
    (let [parameters (java.util.HashMap.)]
      (.put parameters "TABLE_NAME" data-table-name)
      (JasperFillManager/fillReport compiled-report parameters db-connection))))

(defn process-report [db-path job-id]
  (let [db-specification (get-db-specification db-path)
        job (get-report-job job-id db-specification)]
    (if job
      (let [template-path (get-template-path db-specification job)
            memoised-output (get-memoised-report db-specification job-id)]
        (if memoised-output
          (println "Using memoised report for job" job-id)))
          ;; (generate-and-store-report template-path db-specification job)))
      (println "Job ID not found:" job-id))))

(defn generate-report
  [template-path primary-data-table-name db-specification]
  (-> template-path
      (get-compiled-template db-specification)
      (fill-report db-specification primary-data-table-name)
      (export-to-pdf-blob)))

(defn generate-and-store-report
  [template-path job-id temporary-data-tables db-specification]
  (let [primary-data-table-name
        (-> temporary-data-tables
            (parse-json)
            (:primary))]
    (let [[result execution-time]
          (time-execution
           (fn [] (generate-report template-path primary-data-table-name db-specification)))]
      (complete-job-and-store-report result execution-time job-id db-specification))))
