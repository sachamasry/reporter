(ns reporter.utilities.logging
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log])
  (:import [java.lang Thread]))

;; ANSI colour codes
(def colour-codes
  {:info "\033[32m"   ;; Green
   :debug "\033[34m"  ;; Blue
   :warn "\033[33m"   ;; Yellow
   :error "\033[31m"  ;; Red
   :trace "\033[37m"  ;; Red
   :magenta "\033[35m" ;; Magenta
   :cyan "\033[36m"   ;; Cyan
   :reset "\033[0m"})

;; Function to get current thread name (helps to format logs)
(defn current-thread-name []
  (.getName (Thread/currentThread)))

;; Helper function for colouring text
(defn colourise [text colour-key]
  (str (get colour-codes colour-key (get colour-codes :reset)) text (get colour-codes :reset)))

;; Function to format the log line by including the thread, prefix, log level, and message
(defn format-log-line
  [level prefix message]
  (let [level-str (format "%-7s" (.toUpperCase (name level)))
        thread    (current-thread-name)]
    (str level-str " "
         (colourise thread :magenta)  ;; Colourise thread name in magenta
         " " (colourise prefix :cyan)  ;; Colourise prefix in cyan
         " " message)))

;; Function to colourise the log line based on the log level
(defn colourise-log-line
  [level formatted-line]
  (let [colour (get colour-codes level :reset)] ;; Fixing the get call to handle default value properly
    (str colour formatted-line (get colour-codes :reset))))

;; USAGE
;;
;; (log-info "=>" "Starting report compilation")
;; (log-info "-->" "Template loaded from cache")
;; (log-warn "==>" "Table format is deprecated")
;; (log-error "!!" "Report generation failed with nil template")
;;
;; The severity ranking of the below levels, from lowest to highest is:
;; trace < debug < info < warn < error
(defn log-error [prefix message]
  (let [formatted (format-log-line :error prefix message)]
    (log/error (colourise-log-line :error formatted))))

(defn log-warn [prefix message]
  (let [formatted (format-log-line :warn prefix message)]
    (log/warn (colourise-log-line :warn formatted))))

(defn log-info [prefix message]
  (let [formatted (format-log-line :info prefix message)]
    (log/info (colourise-log-line :info formatted))))

(defn log-debug [prefix message]
  (let [formatted (format-log-line :debug prefix message)]
    (log/debug (colourise-log-line :debug formatted))))

(defn log-trace [prefix message]
  (let [formatted (format-log-line :trace prefix message)]
    (log/trace (colourise-log-line :trace formatted))))

;; USAGE
;;
;; (with-log-step "->" "Generate Report"
;;   (compile-template ...)
;;   (run-dataset-query ...)
;;   (write-output ...))
(defmacro with-log-step [prefix label & body]
  `(do
     (log-info ~prefix (str "BEGIN: " ~label))
     (let [result# (do ~@body)]
       (log-info ~prefix (str "END: " ~label))
       result#)))

(defn log-debug-map [prefix label m]
  (log-debug prefix (str label "\n" (with-out-str (pprint m)))))
