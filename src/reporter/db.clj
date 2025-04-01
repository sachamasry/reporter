(ns reporter.db
  (:require [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json]
            [reporter.utilities.json :refer [parse-json]]
            [reporter.utilities.time :refer [current-datetime]]
            [reporter.utilities.metadata :refer [generate-attempted-by]]))

(defn get-db-specification [db-path]
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     db-path})

;; Get a real JDBC connection
(defn get-db-connection
  [db-specification]
  (jdbc/get-connection db-specification))

(defn list-defined-tables
  "Lists all tables defined in the database"
  [db-specification]
  (jdbc/query
   db-specification
   ["SELECT name FROM sqlite_schema WHERE type ='table' AND name NOT LIKE 'sqlite_%';"]))

(defn get-next-job
  "Picks up next job with a state of 'available' from the report job queue,
  returning the entire record."
  [db-specification]
  (first
   (jdbc/query
    db-specification
    ["SELECT * FROM report_jobs WHERE state = 'available' ORDER BY inserted_at LIMIT 1"])))

(defn get-report-job
  "Returns the report job record mathing the provided `ID`, from the database"
  [^String job-id db-specification]
  (first
   (jdbc/query
    db-specification
    ["SELECT * FROM report_jobs WHERE id = ?" job-id])))

(defn report-partially-generated?
  [^String job-id db-specification]
  (let [job (get-report-job job-id db-specification)]
    (not (nil? (:generated_report job)))))  ;; If generated_report has any data, return true

(defn vacuum-database
  "Runs the VACUUM operation on the SQLite database to reclaim free space and defragment it.
   Since SQLite does not allow VACUUM inside a transaction, this function explicitly disables transactional execution."
  [db-specification]
  (try
    (jdbc/db-do-commands db-specification false ["VACUUM"])
    (println "✅ VACUUM completed: Database has been defragmented and optimized.")
    (catch Exception e
      (println (str "⚠️ VACUUM failed: " (.getMessage e))))))

(defn analyse-database
  "Runs the ANALYSE command to update query planner statistics."
  [db-specification]
  (try
    (jdbc/execute! db-specification ["ANALYZE"])
    (println "✅ ANALYSE completed: Query planner statistics updated.")
    (catch Exception e
      (println (str "⚠️ ANALYSE failed: " (.getMessage e))))))

(defn reindex-database
  "Runs REINDEX on the SQLite database to rebuild indexes and improve lookup efficiency."
  [db-specification]
  (try
    (jdbc/execute! db-specification ["REINDEX"])
    (println "✅ REINDEX completed: All indexes rebuilt for efficiency.")
    (catch Exception e
      (println (str "⚠️ REINDEX failed: " (.getMessage e))))))

(defn run-database-maintenance
  "Performs SQLite database maintenance operations: VACUUM, ANALYZE, and REINDEX."
  [db-specification]
  (vacuum-database db-specification)
  (analyse-database db-specification)
  (reindex-database db-specification)
  (println "✅ Database maintenance completed."))

(defn drop-temporary-tables
  "Drops all temporary tables associated with a report job and clears the `temporary_tables_created` field."
  [^String job-id db-specification]
  (let [job (first
             (jdbc/query db-specification
                         ["SELECT temporary_tables_created FROM report_jobs WHERE id = ?" job-id]))
        temporary-tables (json/parse-string (:temporary_tables_created job) true)]  ;; Parse JSON to Clojure map
    (when (seq temporary-tables)
      (doseq [[dataset-name table-name] temporary-tables]
        (try
          (jdbc/execute! db-specification [(str "DROP TABLE IF EXISTS " table-name)])
          (println (str "==> Dropped temporary table for dataset '" dataset-name "': " table-name))
          (catch Exception e
            (println (str "⚠️ Warning: Failed to drop table " table-name ": " (.getMessage e)))))))

    ;; ✅ If all tables were successfully dropped, update the field to an empty JSON object
    (jdbc/update! db-specification
                  :report_jobs
                  {:temporary_tables_created (json/generate-string {})}
                  ["id = ?" job-id])
    (println (str "✅ Cleared `temporary_tables_created` field for job #" job-id))

    ;; ✅ Run database maintenance
    (run-database-maintenance db-specification)))

(defn change-state-to-available
  [^String job-id db-specification]
  (let [timestamp (current-datetime)]
    (jdbc/update! db-specification
                  :report_jobs
                  {:state "available"
                   ;; :available_metadata ""
                   :updated_at timestamp}
                  ["id = ?" job-id])
    (println (str "==> Updated job #'" job-id "' state to 'available'"))))

(defn change-state-to-discarded
  [^String job-id ^String reason db-specification]
  (let [timestamp (current-datetime)]
        ;; previous-attempts (try
        ;;                     (json/parse-string (:attempted_by job) true)
        ;;                     (catch Exception _ {})) ;; Default to an empty map if parsing fails
        ;; new-attempt-metadata (generate-attempted-by)
        ;; updated-attempts (assoc previous-attempts (str attempt) new-attempt-metadata)
        ;; attempt-metadata (json/generate-string updated-attempts)]
        ;; system-state (get-system-state)
        ;; job (get-report-job job-id db-specification)
        ;; metadata {:reason reason
        ;;           :discarded_at timestamp
        ;;           :system_state system-state
        ;;           :failure_type (get-failure-type job)
        ;;           :last_attempt_duration_ms (get-last-attempt-duration job)
        ;;           :previous_attempts_count (:attempt job)}]
    (jdbc/update! db-specification
                  :report_jobs
                  {:state "discarded"
                   :discarded_at timestamp
                   ;; :discarding_metadata ""
                   :updated_at timestamp}
                  ["id = ?" job-id])
    (println (str "==> Updated job #'" job-id "' state to 'discarded'"))))

(defn discard-job
  [^String job-id ^String reason db-specification]
  (change-state-to-discarded job-id reason db-specification)
  (drop-temporary-tables job-id db-specification))  ;; ✅ Drop temp tables here!

;; Discarding metadata example
;; {
;;   "reason": "Max attempts exceeded",
;;   "discarded_at": "2025-04-04T16:45:12",
;;   "system_state": {
;;     "memory_free_mb": 850,
;;     "cpu_load": 45.2
;;   },
;;   "failure_type": "Timeout",
;;   "last_attempt_duration_ms": 8200,
;;   "previous_attempts_count": 3
;; }

(defn change-state-to-executing
  [^String job-id db-specification]
  (let [job (get-report-job job-id db-specification)
        timestamp (current-datetime)
        attempt (inc (:attempt job))
        max-attempts (:max_attempts job)
        previous-attempts (try
                            (json/parse-string (:attempted_by job) true)
                            (catch Exception _ {})) ;; Default to an empty map if parsing fails
        new-attempt-metadata (generate-attempted-by)
        updated-attempts (assoc previous-attempts (str attempt) new-attempt-metadata)
        attempt-metadata (json/generate-string updated-attempts)]
    (if (> attempt max-attempts)
      (do
        (change-state-to-discarded job-id "Max attempts exceeded" db-specification)
        (println (str "==> Job #'" job-id "' has exceeded max attempts, marking it as 'discarded'.")))
      (do
        (jdbc/update! db-specification
                      :report_jobs
                      {:state "executing"
                       :attempt attempt
                       :attempted_at timestamp
                       :attempt_metadata attempt-metadata
                       :updated_at timestamp}
                      ["id = ?" job-id])
        (println (str "==> Updated job #'" job-id "' state to 'executing'."))))))

(defn change-state-to-retryable
  [^String job-id db-specification]
  (let [job (get-report-job job-id db-specification)
        attempt (:attempt job)
        max_attempts (:max_attempts job)
        timestamp (current-datetime)]
    (jdbc/update! db-specification
                  :report_jobs
                  {:state "retryable"
                   :updated_at timestamp}
                  ["id = ?" job-id])
    (println (str "==> Updated job #'" job-id "' state to 'retryable'"))))

(defn change-state-to-completed
  [generated-report report-generation-time ^String job-id db-specification]
  (let [timestamp (current-datetime)]
    (jdbc/update! db-specification
                  :report_jobs
                  {:generated_report generated-report
                   :generation_time_ms report-generation-time
                   :state "completed"
                   :completed_at timestamp
                   ;; :completed_metadata ""
                   :updated_at timestamp}
                  ["id = ?" job-id])
    (println (str "==> Job #'" job-id "' marked as completed."))))

(defn complete-job-and-store-report
  [generated-report report-generation-time job-id db-specification]
  (change-state-to-completed generated-report report-generation-time job-id db-specification)
  (drop-temporary-tables job-id db-specification)
  (println (str "==> Report stored in database for job ID: " job-id "; job state updated to 'completed'")))

(defn change-state-to-cancelled
  [^String job-id ^String reason db-specification]
  (let [timestamp (current-datetime)
        ;; system-state (get-system-state)
        metadata {:reason reason
                  :canceled_at timestamp
                  :canceled_by ""
                  :system_state ""
                  ;; :running_time_ms (get-job-runtime job-id db-specification)
                  ;; :partial_progress (report-partially-generated? job-id db-specification)
                  }]
    (jdbc/update! db-specification
                  :report_jobs
                  {:state "canceled"
                   :cancellation_metadata (json/generate-string metadata)
                   :updated_at timestamp}
                  ["id = ?" job-id])
    (println (str "==> Job #'" job-id "' has been canceled."))))
;; Cancellation metadata example
;; {"reason" : "User requested",
;;  "canceled_at" ​ "2025-04-04T15:12:30",
;;  "canceled_by" ​ "admin_user",
;;  "system_state" ​ {"memory_free_mb" ​ 1024,
;;                    "cpu_load" ​ 23.5},
;;  "running_time_ms" ​ 15230,
;;  "partial_progress" ​ false}

(defn cancel-job
  [^String job-id ^String reason db-specification]
  (change-state-to-cancelled job-id reason db-specification)
  (drop-temporary-tables job-id db-specification))  ;; ✅ Drop temp tables here!

(defn process-job [job db-specification]
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
