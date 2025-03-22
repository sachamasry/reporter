(ns reporter.utilities.json

  (:require
   [cheshire.core :as json]))

(defn parse-json
  "Converts a JSON string into a native Clojure map."
  [json-string]
  (json/parse-string json-string true))

(defn encode-json
  "Encodes a Clojure map into a JSON string."
  [map]
  (json/generate-string map))
