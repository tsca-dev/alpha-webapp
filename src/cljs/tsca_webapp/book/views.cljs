(ns tsca-webapp.book.views
  (:require
   [reagent.core :as reagent]
   [tsca-webapp.mock :as mock]
   [clojure.string :as s]
   [re-frame.core :as re-frame]
   [tsca-webapp.book.subs :as subs]
   [tsca-webapp.routes.events :as routes]
   [tsca-webapp.book.events :as events]
   [tsca-webapp.routes.routes :as rt]
   [tsca-webapp.common.view-parts :as common]
   [tsca-webapp.common.util :as u]
   [tsca-webapp.aii.effects :as aii-effects]))

(defn- highlightable [phrase text]
  (->> (interleave (.split text phrase) (repeat phrase))
       (map-indexed (fn [i word]
                      [:span {:key (str "highlightable-" i)
                              :class (when (= (mod i 2) 1) "text-highlight")} word]))
       (drop-last)
       doall))

(defn- show-book-list [state]
  [:div
   (doall (for [{:keys [bookhash title synopsis]} @(re-frame/subscribe [::subs/books])]
            [:div.p {:key bookhash}
             [:a.c-hand {:on-click #(re-frame/dispatch [::routes/set-active-panel :book-top
                                                        {:bookhash bookhash}])}
              [:h3 title]]
             [:div (highlightable (:search-text @state) synopsis)]]))])

(defn- open-bookapp [spirit-hash]
  (when-not (empty? spirit-hash)
    (some-> spirit-hash
            aii-effects/bookapp-url
            (.then #(js/window.open (.-url %))))))
(defn- open-bookapp-button [state]
  (let [spirit-hash (:spirit-hash @state)]
      [:span.btn
       {:class (when-not (empty? spirit-hash)
                 "bg-primary")
        :on-click #(open-bookapp spirit-hash)}
       "Open Bookapp"
       ]))

(defn home-panel []
  (let [loaded (re-frame/subscribe [::subs/books-loaded?])
        state (reagent/atom {:search-text ""
                             :spirit-hash ""})]
    [:div.docs-content
     [:h1 "TSC Agency"]
     [:form.form-horizontal
      [:div.form-group
       [:div.col-1.col-sm-12
        [:label "Spirit Hash"]]
       [:div.col-3.col-sm-12
        [common/input state [:spirit-hash]]]
       [:div.col-3.col-sm-12
        [:span "　"]
        [open-bookapp-button state]]]]
     [:hr]
     [:div.columns
      [:div.column.col-6 ]
      [:div.column.col-6
       [:form.form-horizontal
        [:div.form-group
         [:div.col-3.col-sm-12
          [:label.form-label.text-right "Search:　"]]
         [:div.col-8.col-sm-12
          [common/input state [:search-text]]]]]]]
     [:div.divider]
     (if @loaded
       [show-book-list state]
       [:h4 @(re-frame/subscribe [::subs/loading-message])])]))

(defn- term-block [xs indexed? ratom path switch-label]
  (common/agreement-checkboxes xs indexed? ratom path switch-label))


(defn- show-modal [modal-atom genesis-network]
  (let [url (rt/sa-proto0 {:label "genesis"
                           :query-params {:networks (js/JSON.stringify (clj->js genesis-network))
                                          :for mock/target-spec-frozen}})]
    (reset! modal-atom {:show true :url url})
    (re-frame/dispatch [::events/change-iframe-url url])))

(defn- close-modal [modal-atom]
  (reset! modal-atom {:show false :url nil}))

(defn- agreement-button [agreements modal-atom subs-key genesis-network label]
  (js/console.log (str "genesis-network: " genesis-network))
  (common/conditional-button agreements subs-key label #(show-modal modal-atom genesis-network)))

(defn- info-icon [popover]
  [:div.popover.popover-bottom
   [:span.bg-primary.shape.s-circle.text-center.text-small.text-bold "i"]
   [:div.popover-container.popover-large
    [:div.card
     [:div.card-body popover]]]])

(defn- link-icon [link-url]
  [:a {:href link-url :target "_blank"} [:i.icon.icon-link]])

(defn- text-with-link-icon [{:keys [value url]}]
  [:div value " " (link-icon url)])

(defn- fee-block [{:keys [provider_charge agency_charge]}]
  (let [sum (u/format-as-tez (+ provider_charge agency_charge))
        provider (u/format-as-tez provider_charge)
        agency (u/format-as-tez agency_charge)]
    [:div
     [:div sum " in total per origination"]
     [:div [:span.text-small "( " provider " + " agency " )"]
      (info-icon [:div
                  [:div provider " payable to the Template Provider, "]
                  [:div agency " payable to the Agency"]])]]))

(defn- term-pair [key-prefix index label]
  [:div.columns {:key (str key-prefix "-" index)}
   [:div.column.col-1.text-right "-"]
   [:div.column.col-11 label]])

(defn- book-header
  [{:keys [title synopsis basic-facts template-details bookhash tmplhash]}
   modal-atom]
  (let [charge @(re-frame/subscribe [::subs/book-charge])
        initial-agreements @(re-frame/subscribe [::subs/initial-agreements])
        agreements (reagent/atom initial-agreements)]
    (fn []
      [:div.docs-content
       [:h1 "Book: " title]
       [:div.p synopsis]

       [:div.card
        [:div.card-header [:h2 "Basic Facts"]]
        [:div.card-body
         [:div.columns
          [:div.column.col-2.text-bold "Provider"] [:div.column.col-4 (text-with-link-icon @(re-frame/subscribe [::subs/book-provider]))]
          [:div.column.col-2.text-bold "Contract Complexity"] [:div.column.col-4 (text-with-link-icon @(re-frame/subscribe [::subs/book-contract-complexity]))]
          [:div.column.col-2.text-bold "Certification Status"] [:div.column.col-4 (text-with-link-icon @(re-frame/subscribe [::subs/book-certification-status]))]
          [:div.column.col-2.text-bold "Template Fees"] [:div.column.col-4 (fee-block charge)]]
         [:div.gap]
         [:div.columns
          [:div.column.col-xl-12.col-6.text-gray (str "book hash: " bookhash)]
          [:div.column.col-xl-12.col-6.text-gray (str "template hash: " tmplhash)]
          [:div.column.col-xl-12.col-6.text-gray (str "network: " (str @(re-frame/subscribe [::subs/genesis-network])))]
          ]]]
       [:div.gap]
       [:div.card
        [:div.card-header [:h2 "Contract Details"]]
        [:div.card-body
         [:h5 "Contract Parameters"]
         (->> (:contract-parameters template-details)
              (map-indexed (fn [i [ident desc]]
                             [:div.columns {:key ident}
                              [:b.column.col-xl-12.col-2.monospace (str " " (inc i) ". " ident)]
                              [:div.column.col-xl-12.col-9 desc]])))
         [:div.gap]
         [:h5 "Contract Terms in English"]
         (term-block (:contract-terms template-details) true
                     agreements :contract-terms "I understand")
         [:div.gap]
         [:h5 "Caveats"]
         (term-block (:caveats template-details) false
                     agreements :caveats "I understand")
         [:div.gap]
         [:h5 "Formal Specification"]
         [:div
          (map-indexed (fn [index {:keys [title synopsis url]}]
                         (term-pair "specification" index
                                    [:div [:b [:a {:href url :target "_blank"} title]]
                                     " "
                                     synopsis]))
                       @(re-frame/subscribe [::subs/specifictions]))]]]
       [:div.gap]
       [:div.column
        [agreement-button agreements modal-atom ::subs/expected-agreements
         @(re-frame/subscribe [::subs/genesis-network])
         [:div "Launch " [:i.icon.icon-upload]]]]])))

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
          {:on-click #(show-modal modal-atom nil)}
          "Start over"]
         " "
         [:button.btn
          {:on-click #(close-modal modal-atom)}
          "Close"]]]])))

(defn book-top []
  (let [modal (reagent/atom {:show false :url nil})
        loaded      (re-frame/subscribe [::subs/books-loaded?])
        book        (re-frame/subscribe [::subs/book-info])]
    (if @loaded
      [:div
       [book-header @book modal]
       [assistnt-modal modal]]
      [:h4 @(re-frame/subscribe [::subs/loading-message])])))
