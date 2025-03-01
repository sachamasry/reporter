(ns reporter.core
  (:require [clj-bean.core :as bean]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json])
  (:import [net.sf.jasperreports.engine
            JasperCompileManager
            JasperFillManager
            JasperExportManager
            JasperPrint]
           ;; [net.sf.jasperreports.engine.data JsonDataSource]
           [net.sf.jasperreports.engine JRPropertiesUtil]
           [net.sf.jasperreports.engine.data JRBeanCollectionDataSource]
           [java.util HashMap]))

;; Correct usage of defbean with camelCase field names
(bean/defbean reporter.core.TimesheetEntry
  [[String businessPartnerName]
   [float billingDurationInHours]
   [String startDate]
   [String startTime]
   [String description]])

(defn json->bean [json-map]
  (reporter.core.TimesheetEntry.
   (:businessPartnerName json-map "")
   (float (:billingDurationInHours json-map 0.0))
   (:startDate json-map "")
   (:startTime json-map "")
   (:description json-map "")))

;; Sample data for testing purposes only
(defn sample-data []
  (let [entry1 (reporter.core.TimesheetEntry. "PartnerA" 8 "2025-02-28" "09:00" "Worked on report")
        entry2 (reporter.core.TimesheetEntry. "PartnerB" 6 "2025-02-28" "10:00" "Worked on presentation")]
    [entry1 entry2]))

;; (defn parse-json [json-str]
;;   (json/read-str json-str :key-fn keyword))
(defn parse-json [json-str]
  (json/parse-string json-str true))  ;; Converts JSON into a Clojure map

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

(defn generate-report [jrxml-path output-path]
  (let [jasper-report (JasperCompileManager/compileReport jrxml-path)
        data-source (JRBeanCollectionDataSource. (sample-data))
        parameters (HashMap.)]
    ;; Fill the report with data and then export it to a PDF file
    (JasperExportManager/exportReportToPdfFile (JasperFillManager/fillReport jasper-report parameters data-source) output-path)))
