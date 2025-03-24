(ns reporter.utilities.hash-functions
  (:import [java.security Security MessageDigest]
           [java.math BigInteger]
           [org.bouncycastle.jcajce.provider.digest Blake2b$Blake2b256]
           [org.bouncycastle.jce.provider BouncyCastleProvider]))

;; Ensure BouncyCastle provider is registered
(Security/addProvider (BouncyCastleProvider.))

(defn sha256-hash
  "Computes the SHA-256 hash of the given input (string or byte array)."
  ^String [^String input]
  (let [digest (MessageDigest/getInstance "SHA-256")
        bytes (if (string? input) (.getBytes input "UTF-8") input)]
    (format "%064x" (BigInteger. 1 (.digest digest bytes)))))

(defn blake2b-hash
  "Computes the Blake2b-256 hash of the given input (string or byte array)."
  ^String [^String input]
  (let [digest (Blake2b$Blake2b256.)
        bytes  (if (string? input)
                 (.getBytes input "UTF-8")  ; Convert only if it's a string
                 input)]  ; Assume it's already a byte array
    (.update digest bytes)
    (format "%064x" (BigInteger. 1 (.digest digest)))))
