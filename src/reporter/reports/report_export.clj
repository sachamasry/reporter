(ns reporter.reports.report-export

  (:require [clojure.java.jdbc :as jdbc]
            [reporter.db :refer [get-db-specification]]
            [reporter.reports.jasper-reports :refer [process-report]]
            [reporter.reports.template-memoisation :refer [get-compiled-template]]
            [reporter.reports.template-compilation :refer [blob-to-jasper-report]]
            [reporter.utilities.time :refer [current-datetime]])
  (:import [net.sf.jasperreports.engine
            JasperCompileManager
            JasperFillManager
            JasperExportManager
            JasperPrint
            JRParameter]
           [net.sf.jasperreports.engine.design JasperDesign]
           [net.sf.jasperreports.engine JRPropertiesUtil]
           [net.sf.jasperreports.engine.util JRXmlUtils]
           [net.sf.jasperreports.engine JRDataSource]
           [net.sf.jasperreports.engine.query JRJdbcQueryExecuterFactory]))

(defn export-to-pdf-file
  "Exports a filled Jasper report to a PDF file on the filesystem."
  [filled-report output-path]
  (JasperExportManager/exportReportToPdfFile filled-report output-path)
  (println (str "==> PDF saved to: " output-path)))

(defn export-to-pdf-blob
  "Exports a filled Jasper report to a PDF byte array and stores it in the database."
  [db-specification job-id filled-report]
  (let [pdf-bytes (JasperExportManager/exportReportToPdf filled-report)]
    (jdbc/update! db-specification
                  :report_jobs
                  {:generated_report pdf-bytes}
                  ["id = ?" job-id])
    (println (str "==> PDF stored in database for job ID: " job-id))))

;; Deprecated function
;; (defn generate-pdf-report
;;   [template-path output-path db-specification data-table-name]
;;   (with-open [db-connection (get-db-connection db-specification)]
;;     (let [compiled-report (get-compiled-template db-specification template-path)
;;           parameters (java.util.HashMap.)]
;;       (.put parameters "TABLE_NAME" data-table-name)
;;       (JasperExportManager/exportReportToPdfFile
;;        (JasperFillManager/fillReport compiled-report parameters db-connection)
;;        output-path))))

;; (defn generate-and-store-report [db-specification job template-path]
;;   (let [output-bytes (generate-pdf-report template-path job)]
;;     (jdbc/insert! db-specification :report_export_memoisation
;;                   {:job_id (:id job)
;;                    :output_blob output-bytes
;;                    :created_at (current-datetime)})))
