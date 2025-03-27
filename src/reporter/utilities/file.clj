(ns reporter.utilities.file

  (:import [java.nio.file Files Paths LinkOption]))

(defn get-file-last-modified
  [file-path]
  (-> (Paths/get file-path (into-array String []))
      (Files/getLastModifiedTime (into-array LinkOption []))
      (.to java.util.concurrent.TimeUnit/SECONDS))) ; Get time directly in seconds
