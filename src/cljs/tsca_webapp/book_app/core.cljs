(ns tsca-webapp.book-app.core
  (:require
   [reagent.core :as reagent]
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [tsca-webapp.book-app.views :as views]
   [tsca-webapp.book-app.events :as events]
   [tsca-webapp.bil.effects]
   [tsca-webapp.dom.effects]
   [tsca-webapp.config :as config]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/top] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/open])
  (dev-setup)
  (mount-root))
