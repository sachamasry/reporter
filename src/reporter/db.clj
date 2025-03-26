(ns reporter.db
  (:require [clojure.java.jdbc :as jdbc]
            [reporter.utilities.json :refer [parse-json]]
            [reporter.utilities.time :refer [current-datetime]]))

(defn get-db-specification [db-path]
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     db-path})

(def db-specification
  (get-db-specification "/Users/sacha/Development/elixir/klepsidra/db/reporter_dev.db"))
;; :subname "/Users/sacha/bin/klepsidra/db/reporter.db"})  

;; Get a real JDBC connection
(defn get-db-connection
  [db-specification]
  (jdbc/get-connection db-specification))

(defn list-defined-tables
  "Lists all tables defined in the database"
  []
  (jdbc/query
   db-specification
   ["SELECT name FROM sqlite_schema WHERE type ='table' AND name NOT LIKE 'sqlite_%';"]))

(defn get-next-job
  "Picks up next job with a state of 'available' from the report job queue,
  returning the entire record."
  []
  (first
   (jdbc/query
    db-specification
    ["SELECT * FROM report_jobs WHERE state = 'available' ORDER BY inserted_at LIMIT 1"])))

(defn get-report-job
  "Returns the report job record mathing the provided `ID`, from the database"
  [db-specification ^String id]
  (first
   (jdbc/query
    db-specification
    ["SELECT * FROM report_jobs WHERE id = ?" id])))

(defn process-job [db-specification job]
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
      (println "PDF format selected. Mock stand-in for complete function."))))
      ;; (generate-pdf-report template-path output-path db-connection primary-data-table-name db-specification))))

;; Proposed code, to be tested
;; (defn process-job [job]
;;   (let [params (json/read-str (:parameters job) :key-fn keyword)
;;         fingerprint (:fingerprint job)
;;         existing-report (first (jdbc/query db-connection ["SELECT result_path FROM report_jobs WHERE fingerprint = ? AND status = 'completed'" fingerprint]))]

;;     (if existing-report
;;       (do
;;         (jdbc/update! db-connection :report_jobs
;;                       {:status "completed" :result_path (:result_path existing-report) :updated_at (java.time.LocalDateTime/now)}
;;                       ["id = ?" (:id job)])
;;         (println "Skipping report generation, using cached version"))

;;       (let [output-path (str "output/report-" fingerprint ".pdf")]
;;         (generate-report "resources/template.jrxml" output-path params)
;;         (jdbc/update! db-connection :report_jobs
;;                       {:status "completed" :result_path output-path :updated_at (java.time.LocalDateTime/now)}
;;                       ["id = ?" (:id job)])))))
;;
;; (defn process-job [job]
;;   (let [job-id (:id job)
;;         report-data (parse-json (:report_data job))
;;         output-path (str "output/" (:fingerprint job) ".pdf")]

;;     (try
;;       ;; Convert JSON data to Java Beans here
;;       (let [data-beans (map ->TimesheetEntry report-data)]  ;; Assuming TimesheetEntry is the bean
;;         (generate-report "resources/client_timesheet.jrxml" output-path data-beans))

;;       ;; Mark job as completed in DB
;;       (update-job-status job-id "completed" output-path)

;;       (catch Exception e
;;         (update-job-status job-id "failed" nil)
;;         (println "Error processing job:" (.getMessage e))))))

;; (defn process-job [job]
;;   (let [job-id (:id job)
;;         report-data (parse-json (:report_data job)) ;; Convert JSON string to Clojure map
;;         data-beans (map json->bean report-data) ;; Convert each map to a Java Bean
;;         output-path (str "output/" (:fingerprint job) ".pdf")]

;;     (try
;;       ;; Generate the report with Java Beans
;;       (generate-report "resources/client_timesheet.jrxml" output-path data-beans)

;;       ;; Mark job as completed in DB
;;       (update-job-status job-id "completed" output-path)

;;       (catch Exception e
;;         (update-job-status job-id "failed" nil)
;;         (println "Error processing job:" (.getMessage e))))))

;; (defn process-job [job]
;;   (let [job-id (:id job)
;;         report-data (parse-json (:report_data job)) ;; Convert JSON string to Clojure map
;;         data-beans (map json->bean report-data) ;; Convert each map to a Java Bean
;;         output-path (str "output/" (:fingerprint job) ".pdf")]

;;     (try
;;       ;; Generate the report using the correct Java Beans
;;       (generate-report "resources/client_timesheet.jrxml" output-path data-beans)

;;       ;; Mark job as completed in the database
;;       (update-job-status job-id "completed" output-path)

;;       (catch Exception e
;;         (update-job-status job-id "failed" nil)
;;         (println "Error processing job:" (.getMessage e))))))

;; (defn run-loop []
;;   (while true
;;     (when-let [job (get-next-job)]
;;       (jdbc/update! db-connection :report_jobs {:status "processing"} ["id = ?" (:id job)])
;;       (try
;;         (process-job job)
;;         (catch Exception e
;;           (jdbc/update! db-connection :report_jobs {:status "failed"} ["id = ?" (:id job)])
;;           (println "Error processing job:" e))))
;;     (Thread/sleep 5000)))
