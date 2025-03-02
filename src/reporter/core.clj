(ns reporter.core
  (:require [clj-bean.core :as bean]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clj-bean.core :as bean])
  (:import [net.sf.jasperreports.engine
            JasperCompileManager
            JasperFillManager
            JasperExportManager
            JasperPrint]
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

(defn json->beans
  "Converts a JSON string or parsed Clojure data into a list of TimesheetEntry beans."
  [json-input]
  (let [data (if (string? json-input)
               (parse-json json-input)  ;; Parse if it's a string
               json-input)]  ;; Use directly if it's already a parsed list
    (map (fn [entry]
           (let [bean (reporter.core.TimesheetEntry.)]
             (.setBusinessPartnerName bean (or (:businessPartnerName entry) "Unknown"))
             (.setBillingDurationInHours bean (float (or (:billingDurationInHours entry) 0.0))) ;; Prevents null float
             (.setStartDate bean (or (:startDate entry) "1970-01-01"))
             (.setStartTime bean (or (:startTime entry) "00:00"))
             (.setDescription bean (or (:description entry) "No description"))
             bean))
         data)))

;; Sample data for testing purposes only
(defn sample-data []
  (let [entry1 (reporter.core.TimesheetEntry. "PartnerA" 8 "2025-02-28" "09:00" "Worked on report")
        entry2 (reporter.core.TimesheetEntry. "PartnerB" 6 "2025-02-28" "10:00" "Worked on presentation")]
    [entry1 entry2]))

(defn parse-json [json-string]
  (json/parse-string json-string true))  ;; Converts JSON into a Clojure map

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

(defn generate-report [jrxml-path output-path report-data]
  (let [jasper-report (JasperCompileManager/compileReport jrxml-path)
        data-source (JRBeanCollectionDataSource. report-data)
        parameters (HashMap.)]
    (JasperExportManager/exportReportToPdfFile
     (JasperFillManager/fillReport jasper-report parameters data-source)
     output-path)))

(def db-connection {:classname "org.sqlite.JDBC"
                    :subprotocol "sqlite"
                    :subname "/Users/sacha/bin/klepsidra/db/reporter.db"})  ;; Use the SQLite file that Elixir writes to

(defn get-next-job []
  (first (jdbc/query db-connection ["SELECT * FROM report_jobs WHERE status = 'pending' ORDER BY inserted_at LIMIT 1"])))
