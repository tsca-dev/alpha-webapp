(ns tsca-webapp.routes.effects
  (:require
   [re-frame.core :as re-frame]
   [tsca-webapp.routes.events :as events]
   [tsca-webapp.routes.routes :as routes]))

(re-frame/reg-fx
 :routing
 (fn [_]
   (routes/app-routes ::events/set-active-panel)))

(re-frame/reg-fx
 :rewrite-url
 (fn [{:keys [event]}]
   (routes/rewrite-url event)))
