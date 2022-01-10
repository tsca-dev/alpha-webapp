(ns tsca-webapp.common.view-parts
  (:require
   [reagent.core]                     ; need this due to override atom
   [re-frame.core :as re-frame]
   ["react-datetime" :as react-datetime]
   [clojure.string :as string]))

(defn input [state key-path]
  [:input {:class "form-input"
           :type "text"
           :value (get-in @state key-path)
           :on-change   #(swap! state assoc-in key-path (-> % .-target .-value))}])


(defn Datetime [attr]
  [:> (.-default react-datetime) attr])

(defn input-with-validate [state validation-state key-path validate-path invalid-message datetime]
  (let [ok? (get-in @validation-state validate-path)
        attr {:type "text"
              :value (get-in @state key-path)}]
    [:div
     (if datetime
       [Datetime
        {:onChange #(swap! state assoc-in key-path (.toISOString (js/Date. (js/Date.parse %)));; (.toISOString )
                           )
         :className (if ok? "form-input is-success" "form-input is-error")
         :dateFormat "YYYY-MM-DD"
         :closeOnSelect true
         :timeFormat "HH:mm:ss"}]
       [:input.form-input (assoc attr
                      :class (if ok? "is-success" "is-error")
                      :on-change #(swap! state assoc-in key-path (-> % .-target .-value)))])
     [:div.form-input-hint (if ok? "　" invalid-message)]]))

(defn input-with-trigger [id trigger-event r-atom]
  (let [on-key-down #(when (= (.-which %) 13)
                       (let [entered (-> @r-atom string/trim)]
                         (when (seq entered)
                           (re-frame/dispatch [trigger-event entered]))))]
    (fn []
      [:input {:class "form-input"
               :type "text"
               :id id
               :on-change   #(reset! r-atom (-> % .-target .-value))
               :on-key-down on-key-down}])))

(defn checkbox [on-change label]
  [:div.form-switch
   [:input {:type "checkbox" :on-change on-change}]
   [:i.form-icon] label])

(defn labeled-checkbox [index key-prefix label switch-label on-change show-index? check?]
  [:div.columns.checkbox {:key (str key-prefix "-" index)}
   [:div.column.col-1.text-right (if show-index? (str (inc index) ". ") "- ")]
   [:div.column.col-8 label]
   [:label.column.col-3
    (when check? (checkbox  #(on-change % index) switch-label))]])

(defn agreement-checkboxes [labels show-index? ratom path switch-label]
  (let [toggle (fn [e index]
                 (swap! ratom assoc-in [path index] (-> e .-target .-checked)))
        need-checkbox (fn [l] (case (:mandatory_consensus l)
                                true [(:contents l) true]
                                false [(:contents l) false]
                                [l true]))
        elements (map-indexed
                 (fn [i l]
                   (let [[message check?] (need-checkbox l)]
                     (labeled-checkbox i path message
                                       switch-label toggle
                                       show-index? check?)))
                 labels)]
    elements))

(defn- conditional-button [target subs-key label on-click]
  (let [expected (re-frame/subscribe [subs-key])]
    (fn []
      [:button.btn.btn-lg.btn-primary
       {:class (when (not= @expected @target) "disabled")
        :on-click on-click}
       label])))
