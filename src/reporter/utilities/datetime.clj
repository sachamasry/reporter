(ns reporter.utilities.datetime
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]))

(defn current-datetime
  []
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss")]
    (.format (LocalDateTime/now) formatter)))
