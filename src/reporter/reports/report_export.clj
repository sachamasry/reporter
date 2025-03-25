(ns reporter.reports.report-export

  (:require [clojure.java.jdbc :as jdbc]
            [reporter.reports.jasper-reports :refer [process-report]]
            [reporter.db :refer [get-db-specification ]]
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

(defn export-to-pdf
  "Exports a filled Jasper report to a PDF file."
  [filled-report output-path]
  (JasperExportManager/exportReportToPdfFile filled-report output-path)
  (println (str "PDF saved to: " output-path)))

;; (defn generate-pdf-report
;;   [template-path output-path db-connection data-table-name]
;;   (let [compiled-report (JasperCompileManager/compileReport template-path)
;;         parameters (java.util.HashMap.)]
;;     (.put parameters "TABLE_NAME" data-table-name)
;;     (JasperExportManager/exportReportToPdfFile
;;      (JasperFillManager/fillReport compiled-report parameters db-connection)
;;      output-path)))

(defn generate-pdf-report
  [template-path output-path db-connection data-table-name db-specification]
  (let [compiled-report (get-compiled-template db-specification template-path)
        parameters (java.util.HashMap.)]
    (.put parameters "TABLE_NAME" data-table-name)
    (JasperExportManager/exportReportToPdfFile
     (JasperFillManager/fillReport compiled-report parameters db-connection)
     output-path)))

(defn generate-and-store-report [db-specification job template-path]
  (let [output-bytes (generate-pdf-report template-path job)]
    (jdbc/insert! db-specification :report_export_memoisation
                  {:job_id (:id job)
                   :output_blob output-bytes
                   :created_at (current-datetime)})))
