(ns reporter.reports.template-memoisation
  (:require [clojure.java.jdbc :as jdbc]
            [reporter.db :refer [get-report-job]]
            [reporter.utilities.file :refer [get-file-last-modified]]
            [reporter.utilities.hash-functions :refer [sha256-hash]]
            [reporter.utilities.time :refer [current-datetime]]
            [reporter.reports.template-compilation
             :refer [compile-report-template-to-sqlite-blob blob-to-jasper-report]]))

(defn get-template-path
  "Returns the file path of the template specified in the report job `job`"
  ^String [db-specification job]
  (:template_path
   (get-report-job db-specification job)))

(defn lookup-memoised-template
  [db-specification ^String template-file-path]
  (jdbc/query
   db-specification
   ["SELECT template_file_last_modified, template_file_hash, compiled_report_template, FROM report_templates_memoisation WHERE template_path = ?"
    template-file-path]
   {:result-set-fn first}))

(defn memoise-compiled-template
  [db-specification ^String template-file-path]
  (let [last-modified (get-file-last-modified ^String template-file-path)
        ^String file-hash (sha256-hash ^String template-file-path)
        current-timestamp (current-datetime)
        compiled-bytes (compile-report-template-to-sqlite-blob ^String template-file-path)]
    (jdbc/insert! db-specification :report_templates_memoisation
                  {:template_path template-file-path
                   :template_hash file-hash
                   :compiled_report_template compiled-bytes
                   :last_modified last-modified
                   :inserted_at current-timestamp
                   :updated_at current-timestamp})))

(defn get-compiled-template
  [db-specification ^String file-path] ;; compile-fn]
  (blob-to-jasper-report
   (if-let [{:keys [template_hash compiled_report_template last_modified]}
            (lookup-memoised-template db-specification ^String file-path)]
     (let [current-modified (get-file-last-modified ^String file-path)]
       (if (= last_modified current-modified)
         compiled_report_template ;; Use cached compiled object
         (if (= template_hash (sha256-hash ^String file-path))
           compiled_report_template ;; Use cached compiled object
           (do
             (memoise-compiled-template db-specification ^String file-path)
             (:compiled_report_template
              (lookup-memoised-template db-specification ^String file-path))))))
     (do
       (memoise-compiled-template db-specification ^String file-path)
       (:compiled_report_template
        (lookup-memoised-template db-specification ^String file-path))))))
