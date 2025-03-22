(ns reporter.reports.template-memoisation
  (:require [clojure.java.jdbc :as jdbc]
            [reporter.utilities.file :refer [get-file-last-modified]]
            [reporter.utilities.hash-functions :refer [sha256-hash]]
            [reporter.utilities.time :refer [current-datetime]]
            [reporter.reports.template-compilation
             :refer [compile-report-template-to-sqlite-blob]]))

(defn get-template-path
  [db-specification job]
  (let [template-id (:template_id job)]
    (:template_path
     (first
      (jdbc/query
       db-specification
       ["SELECT template_path FROM report_templates WHERE template_id = ?" template-id])))))

(defn lookup-memoised-template
  [db-specification file-path]
  (jdbc/query
   db-specification
   ["SELECT template_hash, compiled_report_template, last_modified FROM report_templates_memoisation WHERE template_path = ?"
    file-path]
   {:result-set-fn first}))

(defn memoise-compiled-template
  [db-specification file-path] ;; compiled-object]
  (let [file-hash (sha256-hash file-path)
        last-modified (get-file-last-modified file-path)
        timestamp (current-datetime)
        compiled-bytes (compile-report-template-to-sqlite-blob file-path)]
    (jdbc/insert! db-specification :report_templates_memoisation
                  {:template_path file-path
                   :template_hash file-hash
                   :compiled_report_template compiled-bytes
                   :last_modified last-modified
                   :inserted_at timestamp
                   :updated_at timestamp})))

(defn get-compiled-template
  [db-specification file-path] ;; compile-fn]
  (if-let [{:keys [template_hash compiled_report_template last_modified]}
           (lookup-memoised-template db-specification file-path)]
    (let [current-modified (get-file-last-modified file-path)]
      (if (= last_modified current-modified)
        compiled_report_template ;; Use cached compiled object
        (if (= template_hash (sha256-hash file-path))
          compiled_report_template ;; Use cached compiled object
          (memoise-compiled-template db-specification file-path))))
    (memoise-compiled-template db-specification file-path)))
