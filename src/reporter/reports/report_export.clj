(ns reporter.reports.report-export

  (:import [net.sf.jasperreports.engine
            JasperExportManager]))

(defn export-to-pdf-file
  "Exports a filled Jasper report to a PDF file on the filesystem."
  [filled-report output-path]
  (JasperExportManager/exportReportToPdfFile filled-report output-path)
  (println (str "==> PDF saved to: " output-path)))

(defn export-to-pdf-blob
  "Exports a filled Jasper report to a PDF byte array and stores it in the database."
  [filled-report]
  (JasperExportManager/exportReportToPdf filled-report))
