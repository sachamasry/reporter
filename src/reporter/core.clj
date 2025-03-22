(ns reporter.core
  "Core functionality for Reporter, defining configuration, dependencies
  needed for interfacing with the Java Reports library, and prototype code
  demonstrating a working report-generating solution."

  (:require
   [reporter.db :refer [db-specification db-connection]]))
   ;; [reporter.template-memoisation :refer [get-compiled-template]]))
