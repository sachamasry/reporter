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
            JasperPrint
            JRParameter]
           [net.sf.jasperreports.engine.design JasperDesign]
           ;; [net.sf.jasperreports.engine.xml JRXPathQueryExecuterFactory]
           [net.sf.jasperreports.engine JRPropertiesUtil]
           [net.sf.jasperreports.engine.util JRXmlUtils]
           [net.sf.jasperreports.engine JRDataSource]
           [net.sf.jasperreports.engine.query JRJdbcQueryExecuterFactory]))

           ;; [net.sf.jasperreports.engine.data JRBeanCollectionDataSource]
           ;; [java.util HashMap]))

(defn parse-json
  "Converts a JSON string into a native Clojure map."
  [json-string]
  (json/parse-string json-string true))

(defn compile-report
  "Compiles a JasperReports JRXML template into an in-memory template representation."
  [template-path]
  (JasperCompileManager/compileReport template-path))

(defn compile-report-to-file
  "Compiles a JasperReports JRXML template into a '.jasper' file at `destination-path`."
  [template-path destination-path]
  (JasperCompileManager/compileReportToFile template-path destination-path))

;; (defn input-stream-to-byte-array [input-stream]
;;   (with-open [is input-stream]
;;     (let [byte-array (java.io.ByteArrayOutputStream.)]
;;       (io/copy is byte-array)
;;       (.toByteArray byte-array))))

;; (defn compile-report-to-sqlite-blob
;;   [template-file]
;;   (let [compiled-template (compile-report template-file)]
;;     (input-stream-to-byte-array compiled-template)))

(defn compile-report-to-pdf-byte-array
  "Compiles a JRXML template into a PDF byte array."
  [template-path]
  (let [compiled-report (compile-report template-path)
        jasper-print (JasperFillManager/fillReport compiled-report nil nil) ;; Fill report with data (use nil if no data)
        byte-array-output (java.io.ByteArrayOutputStream.)]
    ;; Export the compiled JasperPrint to PDF and write it to the byte array output stream
    (JasperExportManager/exportReportToPdfStream jasper-print byte-array-output)
    (.toByteArray byte-array-output))) ;; Convert to byte array for storage

;; (defn fill-report
;;   "Fills a compiled Jasper report with data."
;;   [compiled-report-path data]
;;   (let [datasource (JRBeanCollectionDataSource. data)]
;;     (JasperFillManager/fillReport compiled-report-path nil datasource)))

(defn export-to-pdf
  "Exports a filled Jasper report to a PDF file."
  [filled-report output-path]
  (JasperExportManager/exportReportToPdfFile filled-report output-path)
  (println (str "PDF saved to: " output-path)))

(defn generate-pdf-report
  [template-path output-path db-connection data-table-name]
  (let [compiled-report (JasperCompileManager/compileReport template-path)
        parameters (java.util.HashMap.)]
    (.put parameters "TABLE_NAME" data-table-name)
    (JasperExportManager/exportReportToPdfFile
     (JasperFillManager/fillReport compiled-report parameters db-connection)
     output-path)))

(def db-specification {:classname "org.sqlite.JDBC"
                       :subprotocol "sqlite"
                       :subname "/Users/sacha/Development/elixir/klepsidra/db/reporter_dev.db"})
;; :subname "/Users/sacha/bin/klepsidra/db/reporter.db"})  

;; Get a real JDBC connection
(def db-connection (jdbc/get-connection db-specification))

(defn list-defined-tables
  "Lists all tables defined in the database"
  []
  (jdbc/query db-connection ["SELECT name FROM sqlite_schema WHERE type ='table' AND name NOT LIKE 'sqlite_%';"]))

(defn get-next-job
  "Picks up next 'pending' job from the report job queue, returning the entire
  database record."
  []
  (first (jdbc/query db-specification ["SELECT * FROM report_jobs WHERE state = 'available' ORDER BY inserted_at LIMIT 1"])))

;; Proposed code, to be tested
(defn process-job [db-connection job]
  (let [report-name (:report_name job)
        system-template-name (:system_template_name job)
        template-path (:template_path job)
        temporary-data-tables (:temporary_tables_created job)
        primary-data-table-name (-> temporary-data-tables (parse-json) (:primary))
        output-path (:output_path job)
        output-filename (:output_filename job)
        output-type (:output_type job)
        output-path (str output-path "/" output-filename)]
    (if (= output-type "pdf")
      (generate-pdf-report template-path output-path db-connection primary-data-table-name))))

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

(defn benchmark-compile-report [jrxml-path]
  (let [start-time (System/nanoTime)
        compiled-report (JasperCompileManager/compileReport jrxml-path)
        end-time (System/nanoTime)
        elapsed-ms (/ (- end-time start-time) 1e6)]
    (println (str "Report compiled in " elapsed-ms " ms"))
    compiled-report)) ;; Return compiled report for further use
