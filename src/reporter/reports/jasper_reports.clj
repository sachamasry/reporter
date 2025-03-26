(ns reporter.reports.jasper-reports

  (:require [reporter.db :refer [get-db-specification get-db-connection get-report-job]]
            [reporter.reports.template-memoisation :refer [get-template-path]]
              ;; [reporter.reports.report-export :refer [generate-and-store-report]]
            [reporter.reports.report-export-memoisation :refer [get-memoised-report]])

  (:import [net.sf.jasperreports.engine
            JasperCompileManager
            JasperFillManager
            JasperExportManager]))

(defn fill-report
  "Fills a compiled Jasper report with data"
  [db-specification compiled-report data-table-name]
  (with-open [db-connection (get-db-connection db-specification)]
    (let [parameters (java.util.HashMap.)]
      (.put parameters "TABLE_NAME" data-table-name)
      (JasperFillManager/fillReport compiled-report parameters db-connection))))

(defn process-report [db-path job-id]
  (let [db-spec (get-db-specification db-path)
        job (get-report-job db-spec job-id)]
    (if job
      (let [template-path (get-template-path db-spec job)
            memoized-output (get-memoised-report db-spec job-id)]
        (if memoized-output
          (println "Using memoized report for job" job-id)
          ))
          ;; (generate-and-store-report db-spec job template-path)))
      (println "Job ID not found:" job-id))))
