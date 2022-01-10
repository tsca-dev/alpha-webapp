(ns tsca-webapp.chain-clerk.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.common.view-parts :as common]
   [tsca-webapp.chain-clerk.subs :as subs]
   [tsca-webapp.chain-clerk.events :as events]
   [tsca-webapp.task.events :as task]))

(defn- class-for-visibility [step-id]
  {:style (when (not @(re-frame/subscribe [::subs/step-visible? step-id]))
            {:display "none"})})

(defn- error-block [message-key on-retry on-cancel]
  [:div.empty
       [:div.text-large.empty-title.text-error "ERROR!"]
       [:div.empty-subtitle @(re-frame/subscribe [message-key])]
       [:div.empty-action
        [:div.btn.btn-warning.btn-lg
         {:on-click on-retry}
         "Retry"]
        " "
        [:button.btn.btn-link {:on-click on-cancel} "Cancel"]]])

(defn- ledger-pubkey-block [state]
  (when (get-in @state [:ledger-pubkey :show])
    (if @(re-frame/subscribe [::subs/ledger-pubkey-error])
      [error-block ::subs/ledger-pubkey-error-message
       #(re-frame/dispatch [::events/start-ledger-pubkey])
       #(swap! state assoc-in [:ledger-pubkey "show"] false)]

      (let [{:keys [source public-key]} @(re-frame/subscribe [::subs/ledger-source-address])]
        [:div.empty
         [:div.empty-title @(re-frame/subscribe [::subs/ledger-pubkey-message])]
         (when source
           [:div.empty-subtitle "Your source address: " [:b source]])
         (if @(re-frame/subscribe [::subs/ledger-pubkey-loading?])
           [:div.empty-icon [:div.loading.loading-lg]])
         [:div.empty-action
          (when source
            [:button.btn.btn-success.btn-lg
             {:on-click (fn []
                          (swap! state #(-> %
                                            (assoc-in [:ledger-pubkey :show] false)
                                            (assoc-in [:entering :source-address] source)
                                            (assoc-in [:ledger :public-key] public-key))))}
             [:i.icon.icon-check]
             " Use This Address"])
          [:button.btn.btn-link
           {:on-click #(do (swap! state assoc-in [:ledger-pubkey :show] false)
                           (re-frame/dispatch [::task/cancel]))}
           "Cancel"]]]))))

(defn- fields-filled? [obj fields]
  (->> (map obj fields)
       (every? #(not (or (nil? %) (empty? %))))))

(defn- start-simulating [form]
  (let [ops @(re-frame/subscribe [::subs/operation])]
    (re-frame/dispatch [::events/start-simulate form ops])))

(defn- ledger-op-block [state]
  (when (get-in @state [:ledger-op :show])
    (if @(re-frame/subscribe [::subs/ledger-op-error])
      [error-block ::subs/ledger-op-error-message
       #(do (swap! state assoc :agreement false)
            (swap! state assoc-in [:ledger-op :show] false)
            (start-simulating @(re-frame/subscribe [::subs/form])))
       #(swap! state assoc-in [:ledger-op :show] false)]

      [:div.empty
       [:h4.empty-title @(re-frame/subscribe [::subs/ledger-op-message])]
       (if @(re-frame/subscribe [::subs/ledger-op-loading?])
         [:div.empty-icon [:div.loading.loading-lg]]
         [:div
          [:div "Spirit Hash: " @(re-frame/subscribe [::subs/ledger-op-spirit-hash])]
          [:div.gap]
          (map-indexed (fn [index {:keys [label link]}]
                         [:a {:href link :target "_blank" :rel "noopener noreferrer" :key index} label])
                       @(re-frame/subscribe [::subs/ledger-op-links]))
          [:div "(It may take a while to be available)"]])
       (when @(re-frame/subscribe [::subs/ledger-op-cancellable?])
         [:div.empty-action
          [:button.btn.btn-link
           {:on-click #(do (swap! state assoc-in [:ledger-op :show] false)
                           (re-frame/dispatch [::task/cancel]))}
           "Cancel"]])])))

(defn- proceed-button [state]
  [:div
   [:button.btn.btn-primary
    {:disabled (not (fields-filled? (get-in @state [:entering])
                                    [:name :e-mail :source-address]))
     :on-click #(let [st   @state
                      form (-> (:entering st)
                               (assoc :network @(re-frame/subscribe [::subs/network]))
                               (assoc :public-key (get-in st [:ledger :public-key])))]
                  (re-frame/dispatch [::events/go-to-next-step])
                  (start-simulating form))}
    [:i.icon.icon-arrow-right]
    " Proceed"]])

(defn- check-understand [state]
  [:label.form-switch
   [:input {:type "checkbox"
            :on-change #(swap! state update :agreement not)}]
   [:i.form-icon] "It looks good to me"])

(defn- simulation-block [state]
  [:div
   [:h3 @(re-frame/subscribe [::subs/ledger-sim-message])]
   (when @(re-frame/subscribe [::subs/ledger-sim-finished?])
     [:div
      [:pre.console {:style {:height "300px"}} @(re-frame/subscribe [::subs/ledger-sim-detail])]
      [check-understand state]])
   (when @(re-frame/subscribe [::subs/ledger-sim-error?])
     [:div
      [:pre.console {:style {:height "300px"}} @(re-frame/subscribe [::subs/ledger-sim-errmsg])]
      [:button.btn.btn-link
       {:on-click #(start-simulating @(re-frame/subscribe [::subs/form]))}
       "Retry"]])])

(defn- tab-block [state state-key tabs]
  (let [current (state-key @state)
        block   (nth (->> tabs
                          (filter #(= (first %) current))
                          first)
                     2)]
    [:div
     [:ul.tab.tab-block
      (for [[key name] tabs]
        [:li.tab-item {:class (when (= key current) "active")
                       :key key}
         [:a.c-hand {:on-click #(swap! state assoc state-key key)} name]])]
     [block state]]))

(defn- using-ledger [state]
  [:div.card-body
   [:h4 "if you have Ledger device"]
   (if @(re-frame/subscribe [::subs/ledger-available?])
     [:button.btn
      {:on-click #(do
                    (swap! state assoc-in [:ledger-op :show] true)
                    (re-frame/dispatch [::task/cancel])
                    (re-frame/dispatch [::events/start-ledger-op]))}
      "Proceed with Ledger"]
     [:button.btn.disabled
      "You need to load source address by ledger"])
   [:div.gap]
   [ledger-op-block state]])

(defn- using-cli [state]
  (let [cli-instructions @(re-frame/subscribe [::subs/ledger-sim-cli-description])
        book-app @(re-frame/subscribe [::subs/ledger-sim-book-app])]
    [:div.card-body
     [:h4 "use CLI"]
     (when-let [{:keys [title url synopsis]} book-app]
       [:div title " : " [:a {:href url} url]])
     (if cli-instructions
       [:pre cli-instructions]
       [:div "loading instructions..."])]))

(defn- operation-block [state]
  [:div
   [:div.divider]
   [tab-block state :pay-by
    [[:ledger "Ledger" using-ledger]
     [:cli    "use CLI" using-cli]]]])

(defn- doit-block []
  (let [initial-tab (if @(re-frame/subscribe [::subs/ledger-available?])
                      :ledger :cli)
        state (reagent/atom {:ledger-op     {:show false}
                             :agreement false
                             :pay-by initial-tab})]
    [(fn []
       [:div
        [simulation-block state]
        (when (:agreement @state)
          [operation-block state])])]))

(defn- form-block [state]
  [:div
   [:h3 "Please enter your information"]
   [:div.form-horizontal
    [:div.form-group
     [:div.col-3.col-md-12
      [:label.form-label {:for "input-text"} "Name"]]
     [:div {:class "col-9 col-md-12"}
      [common/input state [:entering :name]]]]
    [:div.form-group
     [:div {:class "col-3 col-md-12"}
      [:label.form-label {:for "input-text"} "e-mail"]]
     [:div {:class "col-9 col-md-12"}
      [common/input state [:entering :e-mail]]]]

    [:div.divider]
    [:div.form-group
     [:div {:class "col-3 col-md-12"}
      [:label.form-label {:for "input-text"} "Source Address"]]
     [:div {:class "col-6 col-md-6"}
      [common/input state [:entering :source-address]]]
     [:div.text-right {:class "col-3 col-md-6"}
      [:div "　or　"
       [:button.btn
        {:on-click #(do
                      (swap! state assoc-in [:ledger-pubkey :show] true)
                      (re-frame/dispatch [::task/cancel])
                      (re-frame/dispatch [::events/start-ledger-pubkey]))}
        "Load from Ledger"]]]]
    [ledger-pubkey-block state :ledger-pubkey]]
   [:div.gap]
   [proceed-button state]])

(defn- load-default-user-info []
  (let [{:strs [name email tzaddr]} (-> (js/window.TSCAInternalInterface.Proto0.defaultClerkUserInfo)
                                               js->clj)]
    {:name name :e-mail email :source-address tzaddr}))

(defn clerk-top []
  (let [state (reagent/atom {:entering (load-default-user-info)
                             :ledger-pubkey {:show false}})]
    [:div.docs-content
     [:div.columns
      [:div.column.col-4.col-xl-12
       [:div.card
        [:div.card-header [:h4 "Chain Clerk"]]
        [:div.card-body
         [:div.pre {:class @(re-frame/subscribe [::subs/description-style])}
          @(re-frame/subscribe [::subs/description])]]]]
      [:div.column.col-8.col-xl-12
       [:div.card
        [:div.card-body
         [:div (class-for-visibility :user-confirmation)
          [form-block state]]

         [:div (class-for-visibility :doit)
          [doit-block]]]]]]]))
