(ns reporter.reports.template-compilation

  (:import [net.sf.jasperreports.engine
            JasperCompileManager]))

(defn compile-report
  "Compiles a JasperReports JRXML template into an in-memory template representation."
  [template-path]
  (JasperCompileManager/compileReport template-path))

(defn compile-report-template-to-file
  "Compiles a JasperReports JRXML template into a '.jasper' file at `destination-path`."
  [template-path destination-path]
  (JasperCompileManager/compileReportToFile template-path destination-path))

(defn compile-report-template-to-sqlite-blob
  [template-path]
  (let [compiled-template (compile-report template-path)
        byte-stream (java.io.ByteArrayOutputStream.)]
    (with-open [obj-out (java.io.ObjectOutputStream. byte-stream)]
      (.writeObject obj-out compiled-template))
    (.toByteArray byte-stream)))

(defn blob-to-jasper-report
  "Converts a BLOB (byte array) from the database into a JasperReport object."
  [compiled-report-blob]
  (with-open [input-stream (java.io.ByteArrayInputStream. compiled-report-blob)]
    (net.sf.jasperreports.engine.util.JRLoader/loadObject input-stream)))
