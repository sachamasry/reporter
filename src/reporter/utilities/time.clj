(ns reporter.utilities.time
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]))

(defn current-datetime
  "Gets the current date and time stamp, returning it in the 'yyyy-MM-ddTHH:mm:ss'
  format, without millisencond precision.

  This is the format commonly used in database record datetime stamps
  including `inserted_at` and `undated_at`, as well as other lower-precision uses."
  []
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss")]
    (.format (LocalDateTime/now) formatter)))
