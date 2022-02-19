(ns tsca-webapp.book-app.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [secretary.core]
   [tsca-webapp.routes.routes :as rt]
   [tsca-webapp.mock :as mock]
   [tsca-webapp.book-app.events :as events]
   [tsca-webapp.book-app.subs :as subs]))

(defn- build-spell-assistant-url [label params]
  (let [path (str "/widgets/o/spellassistant/proto0/frozen/" label "?" (secretary.core/encode-query-params params))]
    (js/TSCABookappInterface.agencyUrl (clj->js path))))

(defn- show-modal [modal-atom]
  (let [url (build-spell-assistant-url
             "withdraw"
             {:networks (js/JSON.stringify js/TSCABookappInterface.network)
              :for (js/JSON.stringify (clj->js {:spellkind "spelltospirit"
                    :tmplhash mock/tmplhash-frozen
                    :sprthash (js->clj js/TSCABookappInterface.sprthash)}))})]
    (reset! modal-atom {:show true :url url})
    (re-frame/dispatch [::events/change-iframe-url url])))

(defn- close-modal [modal-atom]
  (reset! modal-atom {:show false :url nil}))

(defn- main-page [modal-atom]
  [:div
   [:h3 "Bookapp for Frozen Contract"]
   [:div.card
    [:div.card-body
     (map-indexed (fn [i {:keys [title value]}]
                    [:div.columns {:key (str "p-" i)}
                     [:div.col-4 title]
                     [:div.col-4 value]])
                  @(re-frame/subscribe [::subs/parameters]))
     [:div.gap]
     [:button.btn
      {:on-click #(show-modal modal-atom)}
      @(re-frame/subscribe [::subs/button-label])]]]])

(defn- assistnt-modal [modal-atom]
  (let [{:keys [show url]} @modal-atom]
    (when show
      [:div.modal.active
       [:div.modal-overlay]
       [:div.modal-container.modal-large
        [:div.modal-body
         [:iframe {:id "assistant-modal"}]]
        [:div.modal-footer
         [:button.btn.btn-error
          {:on-click #(show-modal modal-atom)}
          "Start over"]
         " "
         [:button.btn
          {:on-click #(close-modal modal-atom)}
          "Close"]]]])))

(defn top []
  (let [modal-atom (reagent/atom {:show false :url nil})]
    [:div
     (case @(re-frame/subscribe [::subs/status])
       :loading [:h4 "Loading..."]
       :error   [:h4.text-error "loading ERROR!"]
       [main-page modal-atom])
     [assistnt-modal modal-atom]]))
