(ns reporter.reports.jasper-reports

  (:require [reporter.db
             :refer [get-db-specification
                     get-db-connection
                     get-report-job
                     store-completed-report]]
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
  (let [db-spec (get-db-specification db-path)
        job (get-report-job db-spec job-id)]
    (if job
      (let [template-path (get-template-path db-spec job)
            memoised-output (get-memoised-report db-spec job-id)]
        (if memoised-output
          (println "Using memoized report for job" job-id)))
          ;; (generate-and-store-report template-path db-specification job)))
      (println "Job ID not found:" job-id))))

(defn generate-report
  [template-path db-specification primary-data-table-name]
  (-> template-path
      (get-compiled-template db-specification)
      (fill-report db-specification primary-data-table-name)
      (export-to-pdf-blob)))

(defn generate-and-store-report
  [template-path db-specification job]
  (let [job-id (:id job)
        temporary-data-tables (:temporary_tables_created job)
        primary-data-table-name
        (-> temporary-data-tables
            (parse-json)
            (:primary))]
    (let [[result execution-time]
          (time-execution
           (fn [] (generate-report template-path db-specification primary-data-table-name)))]
      (store-completed-report result execution-time db-specification job-id))))
