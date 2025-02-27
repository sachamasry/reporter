(ns reporter.core
  (:import [net.sf.jasperreports.engine
            JasperCompileManager
            JasperFillManager
            JasperExportManager
            JasperPrint]
           [net.sf.jasperreports.engine JRPropertiesUtil]
           [net.sf.jasperreports.engine.data JRBeanCollectionDataSource]))

(defn compile-report
  "Compiles a JasperReports JRXML template into a .jasper file."
  [template-path]
  (JasperCompileManager/compileReport template-path))

(defn fill-report
  "Fills a compiled Jasper report with data."
  [compiled-report-path data]
  (let [datasource (JRBeanCollectionDataSource. data)]
    (JasperFillManager/fillReport compiled-report-path nil datasource)))

(defn export-to-pdf
  "Exports a filled Jasper report to a PDF file."
  [filled-report output-path]
  (JasperExportManager/exportReportToPdfFile filled-report output-path)
  (println (str "PDF saved to: " output-path)))

;; Example usage
(defn generate-report []
  (let [jrxml-template "resources/client_timesheet.jrxml"  ;; Path to your .jrxml template
        compiled-report (compile-report jrxml-template)
        sample-data [{:name "Alice" :age 30} {:name "Bob" :age 25}]
        filled-report (fill-report compiled-report sample-data)
        pdf-output "output/report.pdf"]
    (export-to-pdf filled-report pdf-output)))

;; (defn generate-report []
;;   (println "Compiling report...")
;;   (let [jrxml-template "resources/client_timesheet.jrxml"
;;         jasper-report (JasperCompileManager/compileReport jrxml-template)]
;;     (println "Report compiled successfully!")))
