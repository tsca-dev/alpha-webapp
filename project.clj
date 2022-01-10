(defproject tsca-webapp "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.764"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library
                               org.clojure/google-closure-library-third-party]]
                 [thheller/shadow-cljs "2.9.3"]
                 [reagent "0.10.0"]
                 [re-frame "0.12.0"]
                 [binaryage/oops "0.7.0"]
                 [day8.re-frame/tracing "0.5.5"]
                 [clj-commons/secretary "1.2.4"]
                 [garden "1.3.10"]
                 [ns-tracker "0.4.0"]
                 [org.clojure/core.match "1.0.0"]]

  :plugins [[lein-shadow "0.2.0"]
            [lein-garden "0.3.0"]
            [lein-shell "0.5.0"]]

  :min-lein-version "2.9.0"

  :jvm-opts ["-Xmx1G"]

  :source-paths ["src/clj" "src/cljs" "gen/js" ]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "resources/public/css"]


  :garden {:builds [{:id           "screen"
                     :source-paths ["src/clj"]
                     :stylesheet   tsca-webapp.css/screen
                     :compiler     {:output-to     "resources/public/css/screen.css"
                                    :pretty-print? true}}]}

  :shell {:commands {"open" {:windows ["cmd" "/c" "start"]
                             :macosx  "open"
                             :linux   "xdg-open"}}}

  :shadow-cljs {:nrepl {:port 8777}
                :ssl {:keystore "ssl/keystore.jks"
                      :password "shadow-cljs"}
                :builds {:app {:target :browser
                               :output-dir "resources/public/js/compiled"
                               :asset-path "/js/compiled"
                               :modules {:app {:init-fn tsca-webapp.core/init
                                               :preloads [devtools.preload
                                                          day8.re-frame-10x.preload]}}
                               :dev {:compiler-options {:closure-defines {re-frame.trace.trace-enabled? true
                                                                          day8.re-frame.tracing.trace-enabled? true}}}
                               :release {:compiler-options
                                         {:optimizations :simple
                                          :infer-externs true
                                          :output-feature-set :es6}
                                         :build-options
                                         {:ns-aliases
                                          {day8.re-frame.tracing day8.re-frame.tracing-stubs}}}

                               ;; :dev {:compiler-options {}
                               ;;           :build-options
                               ;;           {:ns-aliases
                               ;;            {day8.re-frame.tracing day8.re-frame.tracing-stubs}}}

                               :devtools {:http-root "resources/public"
                                          :http-port 8280
                                          }}
                         :book-app {:target :browser
                               :output-dir "resources/bookapp/js/compiled"
                               :asset-path "/js/compiled"
                                    :modules {:book-app {:init-fn tsca-webapp.book-app.core/init
                                                         :preloads [devtools.preload
                                                                    day8.re-frame-10x.preload]}}
                               :dev {:compiler-options {:closure-defines {re-frame.trace.trace-enabled? true
                                                                          day8.re-frame.tracing.trace-enabled? true}}}
                               :release {:compiler-options {:optimizations :simple}
                                         :build-options
                                         {:ns-aliases
                                          {day8.re-frame.tracing day8.re-frame.tracing-stubs}}}

                                    :devtools {:http-root "resources/bookapp"
                                               :http-port 8282}}}}

  :aliases {"dev"          ["with-profile" "dev" "do"
                            ["shadow" "watch" "app"]]
            "prepare"      ["do"
                            ["shadow" "npm-deps"]
                            ["garden" "once"]
                            ["shell" "npm" "run" "vendor-make"]]
            "prod"         ["with-profile" "prod" "do"
                            ["shadow" "release" "app"]]
            "build-report" ["with-profile" "prod" "do"
                            ["shadow" "run" "shadow.cljs.build-report" "app" "target/build-report.html"]
                            ["shell" "open" "target/build-report.html"]]
            "karma"        ["with-profile" "prod" "do"
                            ["shadow" "compile" "karma-test"]
                            ["shell" "karma" "start" "--single-run" "--reporters" "junit,dots"]]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "1.0.0"]
                   [day8.re-frame/re-frame-10x "0.6.5"]]
    :source-paths ["dev"]}

   :prod
   {:dependencies [[day8.re-frame/re-frame-10x "0.6.5"]]}
   
}

  :prep-tasks [["garden" "once"]])
