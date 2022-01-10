(ns tsca-webapp.ledger.subs
  (:require [re-frame.core :as re-frame]
            [tsca-webapp.common.subs-parts :as common]))

(re-frame/reg-sub
 ::apdu
 (fn [db _]
   (:apdu db)))

(re-frame/reg-sub
 ::apdu-status
 :<- [::apdu]
 (fn [apdu _]
   (:status apdu)))

(re-frame/reg-sub
 ::apdu-sending?
 :<- [::apdu-status]
 (fn [status _]
   (= status :sending)))

(re-frame/reg-sub
 ::apdu-message
 :<- [::apdu]
 (fn [apdu _]
   (let [msg (:result apdu)]
     (case msg
       nil "(no message yet)"
       "" "(empty)"
       msg))))

(re-frame/reg-sub
 ::apdu-error
 :<- [::apdu]
 (fn [apdu _]
   (some-> (:error apdu) str)))

(re-frame/reg-sub
 ::apdu-result
 :<- [::apdu-sending?]
 :<- [::apdu-message]
 :<- [::apdu-error]
 (fn [[sending? message error] _]
   (if sending?
     "(loading...)"
     (or error message))))

(re-frame/reg-sub
 ::button-class
 :<- [::apdu-sending?]
 (fn [sending? _]
   (str (common/button-class sending?)
        (when sending? " loading"))))
