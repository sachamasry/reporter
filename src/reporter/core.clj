(ns reporter.core
  "Core functionality for Reporter, defining configuration, dependencies
  needed for interfacing with the Java Reports library, and prototype code
  demonstrating a working report-generating solution."
  (:require
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

(defn parse-json [json-string]
  "Converts a JSON string into a native Clojure map."
  (json/parse-string json-string true))

(defn compile-report
  "Compiles a JasperReports JRXML template into an in-memory template representation."
  [template-path]
  (JasperCompileManager/compileReport template-path))

(defn compile-report-to-file
  "Compiles a JasperReports JRXML template into a '.jasper' file at `destination-path`."
  [template-path destination-path]
  (JasperCompileManager/compileReportToFile template-path destination-path))

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
  "Picks up next 'pending' job from the report job queue, returning the entire
  database record."
  (first (jdbc/query db-connection ["SELECT * FROM report_jobs WHERE state = 'pending' ORDER BY inserted_at LIMIT 1"])))

;; Proposed code, to be tested
(defn process-job [job]
  (let [params [] ;; (json/read-str (:parameters job) :key-fn keyword)
        output-path (str "output/report-" (:id job) ".pdf")]
    (generate-report "resources/template.jrxml" output-path params)))
    ;; (jdbc/update! db-spec :report_jobs
    ;;               {:status "completed" :result_path output-path :updated_at (java.time.LocalDateTime/now)}
    ;;               ["id = ?" (:id job)])))


;; The following is prototype code that would task itself at looking for further
;; queued report jobs with a status of "processing", take them off the queue and
;; perform work on them.
;;
;; It is not known at this stage whether this is the best way forward.
;;
;; (defn run-loop []
;;   (while true
;;     (when-let [job (get-next-job)]
;;       (jdbc/update! db-spec :report_jobs {:status "processing"} ["id = ?" (:id job)])
;;       (try
;;         (process-job job)
;;         (catch Exception e
;;           (jdbc/update! db-spec :report_jobs {:status "failed"} ["id = ?" (:id job)])
;;           (println "Error processing job:" e))))
;;     (Thread/sleep 5000)))  ;; Poll every 5 seconds
