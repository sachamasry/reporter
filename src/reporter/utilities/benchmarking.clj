(ns reporter.benchmark
  (:require [criterium.core :as crit]
            [clojure.java.io :as io]
            [reporter.crypto :as crypto])
  (:import (java.nio.file Files Paths)
           (java.util Base64)
           [java.security MessageDigest]))

;; Utility: Read File Contents
(defn read-file-bytes
  "Reads a file and returns its byte array."
  [file-path]
  (Files/readAllBytes (Paths/get file-path (make-array String 0))))

(defn benchmark-hashes
  "Runs a benchmark comparing SHA-256 and Blake2b-256 performance on the given file."
  [file-path]
  (let [file-data (read-file-bytes file-path)]
    (println "Benchmarking SHA-256...")
    (crit/quick-bench (crypto/sha256-hash file-data))

    (println "Benchmarking Blake2b-256...")
    (crit/quick-bench (crypto/blake2b-hash file-data))))

;; (defn benchmark-compile-report [jrxml-path]
;;   (let [start-time (System/nanoTime)
;;         compiled-report (JasperCompileManager/compileReport jrxml-path)
;;         end-time (System/nanoTime)
;;         elapsed-ms (/ (- end-time start-time) 1e6)]
;;     (println (str "Report compiled in " elapsed-ms " ms"))
;;     compiled-report)) ;; Return compiled report for further use
