(ns reporter.utilities.benchmarking
  (:require [clojure.java.jdbc :as jdbc]
            [criterium.core :as crit]
            [clojure.java.io :as io]
            [reporter.utilities.hash-functions :refer [sha256-hash blake2b-hash]]
            [reporter.utilities.time :refer [current-datetime]]
            [reporter.reports.template-compilation :refer [compile-report]]
            [reporter.reports.template-memoisation :refer [get-compiled-template]])
  (:import (java.nio.file Files Paths)
           (java.util Base64)
           [java.security MessageDigest]))

(defn read-file-bytes
  "Reads a file and returns its byte array."
  [file-path]
  (Files/readAllBytes (Paths/get file-path (make-array String 0))))

(defn benchmark-hashes
  "Runs a benchmark comparing SHA-256 and Blake2b-256 performance on the given file."
  [^String file-path]
  (let [file-data (read-file-bytes file-path)]
    (println "")
    (println "==> Benchmarking SHA-256...")
    (crit/bench (sha256-hash file-data))

    (println "")
    (println "==> Benchmarking Blake2b-256...")
    (crit/bench (blake2b-hash file-data))))

(defn benchmark-compile-report-once [template-path]
  (let [start-time (System/nanoTime)
        compiled-report (compile-report template-path)
        end-time (System/nanoTime)
        elapsed-ms (/ (- end-time start-time) 1e6)]
    (println (str "Report compiled in " elapsed-ms " ms"))
    compiled-report)) ;; Return compiled report for further use

(defn benchmark-compile-report
  [db-specification ^String template-path]
  (do
    (println "")
    (println "==> Benchmarking template compiling...")
    (crit/bench (compile-report template-path))

    (println "")
    (println "==> Benchmarking get memoised template...")
    (crit/quick-bench (get-compiled-template db-specification template-path))))
