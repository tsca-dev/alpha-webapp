(ns tsca-webapp.routes.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::aii
 (fn [db _]
   (:aii db)))

(re-frame/reg-sub
 ::aii-ready?
 (fn [db _]
   (not (:aii db))))

(re-frame/reg-sub
 ::aii-loading?
 :<- [::aii]
 (fn [aii _]
   (= (:state aii) :loading)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::routing-params
 (fn [db _]
   (:routing-params db)))

