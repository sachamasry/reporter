(ns reporter.benchmark
  (:require [criterium.core :as crit])
  (:import (org.bouncycastle.jcajce.provider.digest Blake2b256 SHA256Digest)
           (java.nio.file Files Paths)
           (java.util Base64)))

;; Utility: Read File Contents
(defn read-file-bytes [file-path]
  (Files/readAllBytes (Paths/get file-path)))

;; BLAKE2b-256 Hashing
(defn blake2b-hash [file-bytes]
  (let [digest (Blake2b256.)]
    (-> (.digest digest file-bytes)
        (Base64/getEncoder)
        (.encodeToString))))

;; SHA-256 Hashing
(defn sha256-hash [file-bytes]
  (let [digest (SHA256Digest.)]
    (.update digest file-bytes 0 (count file-bytes))
    (let [output (byte-array 32)]
      (.doFinal digest output 0)
      (-> (Base64/getEncoder) (.encodeToString output)))))

;; Benchmark Function
(defn benchmark-hashes [file-path]
  (let [file-bytes (read-file-bytes file-path)]
    (println "Benchmarking BLAKE2b-256...")
    (crit/quick-bench (blake2b-hash file-bytes))

    (println "\nBenchmarking SHA-256...")
    (crit/quick-bench (sha256-hash file-bytes))))
