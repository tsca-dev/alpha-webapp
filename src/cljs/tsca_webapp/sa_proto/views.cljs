(ns tsca-webapp.sa-proto.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.common.view-parts :as common]
   [tsca-webapp.sa-proto.subs :as subs]
   [tsca-webapp.routes.events :as routes]
   [tsca-webapp.task.events :as task]
   [tsca-webapp.chain-clerk.events :as cc]
   [clojure.string :as s]
   [cljs.reader :as reader]))

(defn- to-js [obj]
  (-> (reduce-kv (fn [acc k v]
                   (let [key (s/replace (name k) "-" "_")]
                     (assoc acc key v))) {} obj)
      clj->js))

(defn- build-serializer [xs]
  (fn [obj]
    (-> (reduce (fn [m {:keys [field convert]}]
                  (let [v (get obj field)]
                    (assoc m field
                           (case convert
                             :number (reader/read-string v)
                             :comma-separated (s/split v #",")
                             v))))
                {}
                xs)
        to-js)))

(defn- build-validator [xs]
  (fn [obj]
    (reduce (fn [validated {:keys [field validate-by]}]
              (if validate-by
                (->> (get-in obj [:entering field])
                     validate-by
                     (assoc validated field))
                (assoc validated field true)))
            {}
            xs)))

(defn- everything-valid? [obj]
  (->> obj vals
       (every? identity)))

(defn- everything-valid-and-aii? [obj]
  (and (everything-valid? obj)
       (get-in obj [:aii-valid :valid?])))

(defn- info [state validation serializer]
  (let [st @state
        v  @validation
        build-spell @(re-frame/subscribe [::subs/spell-builder])
        aii-valid (:aii-valid v)]
    [:div.panel
     [:div.panel-body
      [:pre (str (try (build-spell (serializer (:entering st)))
                      (catch :default e "error!")))]
      (when (not (:valid? aii-valid))
        [:pre.text-error
         (if (:message aii-valid)
           (str "error message from aii: " (:message aii-valid))
           (str "exception from aii: " (str (:ex aii-valid))))])
      [:div "networks:" (str @(re-frame/subscribe [::subs/networks]))]
      [:div "target spec:" @(re-frame/subscribe [::subs/target-spec])]]]))

(defn- proceed-button [state validation serializer]
  [:button.btn.btn-primary
   {:disabled (not (everything-valid-and-aii? @validation))
       :on-click #(let [networks    @(re-frame/subscribe [::subs/networks])
                        target-spec @(re-frame/subscribe [::subs/target-spec])
                        sahash      @(re-frame/subscribe [::subs/sahash])
                        build-spell @(re-frame/subscribe [::subs/spell-builder])
                        spell       (build-spell (serializer (:entering @state))) ;todo error handling
                        params      {:query-params {:networks  networks
                                                    :for       target-spec
                                                    :spell     spell
                                                    :sahash    sahash}}]
                    (re-frame/dispatch [::routes/set-active-panel :clerk-panel params])
                    (re-frame/dispatch [::cc/load-description])
                    )}
   "Proceed"])

(defn- aii-validate [serializer aii-verifier state validation]
  ;; (try (let [{:keys [valid error]} (-> (:entering @state)
  ;;                                      serializer
  ;;                                      aii-verifier
  ;;                                      (js->clj :keywordize-keys true))]
  ;;        (swap! validation assoc :aii-valid {:valid? valid :message error}))
  ;;      (catch :default e
  ;;        (js/console.error "error from aii" e)
  ;;        (swap! validation assoc :aii-valid {:valid? false :ex e})))
  (swap! validation assoc :aii-valid {:valid? true :message nil})
  )

(defn- forms [xs]
  (let [aii-verifier nil ;; @(re-frame/subscribe [::subs/verifier])
        validator (build-validator xs)
        validation (reagent/atom (validator {}))
        serializer (build-serializer xs)
        state (reagent/atom {})
        _ (add-watch state :entering
                     (fn [x state old new]
                       (aii-validate serializer aii-verifier state validation)
                       (swap! validation merge (validator new))))]
    [:div.form-horizontal
     (for [{:keys [label field validate-by invalid-message datetime]} xs]
       [:div.form-group {:key field}
        [:div.col-3.col-md-12
         [:label.form-label {:for "input-text"} label]]
        [:div {:class "col-9 col-md-12"}
         (if validate-by
           [common/input-with-validate state validation
            [:entering field] [field]
            invalid-message datetime]
           [common/input state [:entering field]])]])
     [:div.gap]
     [proceed-button state validation serializer]
     [:div.gap]
     [info state validation serializer]]))

(defn- not-empty? [str]
  (not (empty? str)))

(defn- positive-number? [str]
  (boolean (and str (re-matches #"\d+(\.\d+)?" str))))

(defn- iso8601? [str]
  (boolean (and str (re-matches #"^([\+-]?\d{4}(?!\d{2}\b))((-?)((0[1-9]|1[0-2])(\3([12]\d|0[1-9]|3[01]))?|W([0-4]\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\d|[12]\d{2}|3([0-5]\d|6[1-6])))([T\s]((([01]\d|2[0-3])((:?)[0-5]\d)?|24\:?00)([\.,]\d+(?!:))?)?(\17[0-5]\d([\.,]\d+)?)?([zZ]|([\+-])([01]\d|2[0-3]):?([0-5]\d)?)?)?)?$" str))))

(defn- withdraw []
  [{:label "Amount"      :field :amount       :validate-by positive-number? :convert :number
    :invalid-message "should be positive number"}
   {:label "Beneficiary" :field :beneficiary  :validate-by not-empty?
    :invalid-message "required"}])

(defn- genesis []
  [{:label "Fund_amount" :field :fund-amount :validate-by positive-number? :convert :number
    :invalid-message "positive number ony"}
   {:label "Frozen_until" :field :unfrozen :validate-by iso8601?
    :datetime true
    :invalid-message "date format (e.g. 2020-07-02 00:00:00 )"}
   {:label "Fund_owners" :field :fund-owners :validate-by not-empty?
    :convert :comma-separated
    :invalid-message "required"}])

(defn main [agreed?]
  [:div {:style {:display (when (not @agreed?) "none")}}
   (case @(re-frame/subscribe [::subs/label])
     "withdraw" (forms (withdraw))
     "genesis" (forms (genesis))
     [:div "unknown label: " @(re-frame/subscribe [::subs/label])])])

(defn- assistant-term [agreed?]
  (let [initial-agreements @(re-frame/subscribe [::subs/initial-agreements-assistant])
        agreements (reagent/atom initial-agreements)
        terms @(re-frame/subscribe [::subs/assistant-terms])]
    [:div {:style {:display (when @agreed? "none")}}
     [:div.text-center.h3 "Terms Of The Service"]
     (common/agreement-checkboxes terms false agreements :terms "agree")
     [:div.gap]
     [:div.text-center
      [common/conditional-button agreements ::subs/expected-agreements-assistant
       [:div "Use this template and create a Smart Contract now!" " "]
       #(reset! agreed? true)]]]))

(defn top []
  (let [agreed? (reagent/atom true)]
    [:div.docs-content
     [assistant-term agreed?]
     [(fn []
        (case @(re-frame/subscribe [::subs/verifier-state])
          :verifier-loading nil
          :verifier-loading-error [:h4 "unexpected error!"]
          [main agreed?]))]]))
