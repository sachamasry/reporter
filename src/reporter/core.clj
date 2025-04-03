(ns reporter.core
  "Core functionality for Reporter, defining configuration, dependencies
  needed for interfacing with the Java Reports library, and prototype code
  demonstrating a working report-generating solution."

  (:gen-class)
  (:require [reporter.db :refer [get-db-specification get-report-job change-state-to-executing]]
            [reporter.reports.template-memoisation :refer [get-template-path]]
            [reporter.reports.jasper-reports :refer [process-report]]
            [reporter.reports.template-memoisation
             :refer [get-compiled-template get-template-path]]
            [reporter.reports.jasper-reports :refer [generate-and-store-report]]
            [clojure.tools.logging :as log]
            ;; [reporter.reports.report-export-memoisation :refer [get-generated-report]]
            ))

(defn safe-execute [f]
  (try
    (f)
    (catch Exception e
      (println "Error processing report:" (.getMessage e))
      (.printStackTrace e)
      (System/exit 1))))

(defn execute-job
  [db-path job-id]
  (log/info "🚀 Executing job" job-id " on database " db-path)
  (try
    (let [db-specification (get-db-specification db-path)
          job (get-report-job job-id db-specification)]
      (log/debug "DB specification:" db-specification)
      (log/debug "Job repord:" job)
      (if job
        (let [template-path (get-template-path db-specification job)
              temporary-data-tables (:temporary_tables_created job)]
          (println (str "=> Working on job #'" job-id "'..."))
          (change-state-to-executing job-id db-specification)
          (generate-and-store-report template-path job-id temporary-data-tables db-specification)
          (log/info "✅ Job completed:" job-id)))
        (println "=> Job ID not found:" job-id))
    (catch Exception e
      (log/error e "❌ Report execution failed for job:" job-id))))


(defn -main
  [& args]
  (let [[command db-path job-id] args]
    (case command
      "execute-job" (if (and db-path job-id)
                      (do
                        (log/infof "🧩 Received job execution request: db=%s, job-id=%s" db-path job-id)
                        (safe-execute #(execute-job db-path job-id)))
                      (log/warn "⚠️ Usage: reporter execute-job <db-path> <job-id>"))
      (log/error "❌ Unknown command:" command))))


;; (defn -main [& args]
;;   (safe-execute #(process-report (first args) (second args)))
;;   (System/exit 0))
