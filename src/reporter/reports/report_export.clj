(ns reporter.reports.report-export

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
  (let [compiled-report-blob (get-compiled-template db-specification template-path)
        compiled-report (blob-to-jasper-report compiled-report-blob)
        parameters (java.util.HashMap.)]
    (.put parameters "TABLE_NAME" data-table-name)
    (JasperExportManager/exportReportToPdfFile
     (JasperFillManager/fillReport compiled-report parameters db-connection)
     output-path)))
