(ns reporter.crypto
  (:import [java.security Security MessageDigest]
           [org.bouncycastle.jcajce.provider.digest Blake2b]
           [org.bouncycastle.jce.provider BouncyCastleProvider]))

;; Ensure BouncyCastle provider is registered
(Security/addProvider (BouncyCastleProvider.))

(defn sha256-hash
  "Computes the SHA-256 hash of the given input (string or byte array)."
  [input]
  (let [digest (MessageDigest/getInstance "SHA-256")
        bytes (if (string? input) (.getBytes input "UTF-8") input)]
    (format "%064x" (BigInteger. 1 (.digest digest bytes)))))

(defn blake2b-hash
  "Computes the Blake2b-256 hash of the given input (string or byte array)."
  [input]
  (let [digest (MessageDigest/getInstance "BLAKE2B-256" "BC")
        bytes (if (string? input) (.getBytes input "UTF-8") input)]
    (format "%064x" (BigInteger. 1 (.digest digest bytes)))))
