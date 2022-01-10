(ns tsca-webapp.routes.views
  (:require
   [re-frame.core :as re-frame]
   [tsca-webapp.routes.subs :as subs]
   [tsca-webapp.book.views :as book]
   [tsca-webapp.chain-clerk.views :as clerk]
   [tsca-webapp.sa-proto.views :as sa-proto]
   [tsca-webapp.book-app.views :as book-app]
   [tsca-webapp.ledger.views :as ledger]))

(defn- panels [panel-name]
  (case panel-name
    :ledger-panel       [ledger/try-ledger]
    :home-panel         [book/home-panel]
    :book-top           [book/book-top]
    :spell-assistant    [sa-proto/top]
    :clerk-panel        [clerk/clerk-top]
    :book-app           [book-app/top]
    [:h1 "not found"]))

(defn show-panel [panel-name]
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    (if @active-panel
      [(fn [] [panels @active-panel])]
      [:h4 "Loading...."])))

(defn main-panel []
  (if @(re-frame/subscribe [::subs/aii-ready?])
    [show-panel]
    (if @(re-frame/subscribe [::subs/aii-loading?])
      [:h4 "Loading.."]
      [:div "Loading error"])))
