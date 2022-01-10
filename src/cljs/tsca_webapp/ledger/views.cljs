(ns tsca-webapp.ledger.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [tsca-webapp.common.view-parts :as common]
   [tsca-webapp.ledger.events :as events]
   [tsca-webapp.ledger.subs :as subs]
   [tsca-webapp.task.events :as task]))

(defn try-ledger []
  (let [entering-command (reagent/atom "")]
    [(fn []
       [:div {:class "docs-content"}
        [:div {:class "form-horizontal"}
         [:div {:class "form-group"}
          [:div {:class "col-3 col-sm12"}
           [:label {:class "form-label" :for "input-op"} "Operation"]]
          [:div {:class "col-9 col-sm12"}
           [common/input-with-trigger "input-op" :ledger-sign-op entering-command]]]
         [:div {:class "form-group"}
          [:button {:class @(re-frame/subscribe [::subs/button-class])
                    :on-click #(re-frame/dispatch
                                [::events/ledger-sign-op @entering-command])}
           "Sign"]
          (when @(re-frame/subscribe [::subs/apdu-sending?])
            [:button.btn.btn-link
             {:on-click task/cancel-all}
             "cancel"])]

         [:div {:class "form-group"}
          [:div {:class "col-3 col-sm12"}
           [:label {:class "form-label"} "Result"]]
          [:div {:class "col-9 col-sm12"}
           [:div @(re-frame/subscribe [::subs/apdu-result])]]]]])]))
