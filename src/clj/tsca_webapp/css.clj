(ns tsca-webapp.css
  (:require [garden.def :refer [defstyles]]))

(defstyles screen
  [:.p            {:margin "0 0 1.2rem"}]
  [:.pre          {:white-space "pre-wrap"
                   :word-break  "break-all"}]
  [:.docs-content {:margin "36px"}]
  [:.gap          {:height "16px"}]
  [:.modal-large  {:width "90vw"
                   :height "95vh"
                   :max-width "none"
                   :max-height "none"
                   :overflow-y "none"}
   [:.modal-body  {:flex-grow 1
                   :margin "18px 0 0"
                   :overflow "hidden"
                   :padding 0}]
   [:iframe        {:width "100%"
                    :height "100%"}]]

  [:.text-small   {:font-size "80%"}]
  [:.monospace    {:font-family "monospace"}]
  [:.card         {:height "100%"}]
  [:.shape        {:width "18px" :height "18px"
                   :margin "2px 6px"
                   :line-height "1.5em"
                   :display "inline-block"}]
  [:.flexbox      {:display "flex"
                   :justify-content "center"
                   :flex-wrap "wrap"}
   [:a   {:margin  "0 18px"}]]
  [:.popover-container.popover-large {:width "380px"}]
  [:.columns.checkbox {:margin "12px 0"}]
  [:.text-highlight {:background "yellow"}]
  [:.console {:font-size "70%"
              :overflow "scroll"}]
  [:.form-input [:input {:text-decoration 'none
                         :outline 'none
                         :border 'none
                         :width "100%"}
                 ]])
