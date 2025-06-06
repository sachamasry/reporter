(defproject reporter "0.1.6"
  :description "Clojure report generator built on Jasper Reports report-generation library"
  :url ""
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/java.classpath "1.0.0"]
                 [org.tcrawley/dynapath "1.1.0"]
                 [com.wjoel/clj-bean "0.2.1"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.xerial/sqlite-jdbc "3.36.0.3"]
                 [cheshire "5.11.0"]
                 [com.fasterxml.jackson.core/jackson-core "2.17.1"]
                 [com.fasterxml.jackson.core/jackson-databind "2.17.1"]
                 [com.fasterxml.jackson.core/jackson-annotations "2.17.1"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-xml "2.17.1"]
                 [net.sf.jasperreports/jasperreports "7.0.1"]
                 [net.sf.jasperreports/jasperreports-fonts "7.0.1"]
                 [net.sf.jasperreports/jasperreports-functions "7.0.1"]
                 [net.sf.jasperreports/jasperreports-groovy "7.0.1"]
                 [net.sf.jasperreports/jasperreports-json "7.0.1"]
                 [net.sf.jasperreports/jasperreports-pdf "7.0.1"]
                 [criterium "0.4.6"]
                 [org.bouncycastle/bcprov-jdk18on "1.80"]
                 [net.clojars.aneilm/lein-project-reader "0.0.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.slf4j/slf4j-simple "2.0.17"]
                 ]
                 ;; [org.slf4j/slf4j-nop "2.0.17"]]

  :resource-paths ["resources" "resources/fonts/DRReportFontSet.jar"]
  ;; :main ^:skip-aot reporter.core
  :main reporter.core
  :aot [reporter.core]
  :target-path "target/%s"
  :uberjar-name "reporter.jar"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
