(ns tsca-webapp.ledger.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-fx
 ::ledger-sign-op
 (fn-traced [coeffects [_ command]]
            (let [db (:db coeffects)]
              {:db (-> db
                       (assoc-in [:apdu :status] :sending)
                       (assoc-in [:apdu :error] nil))
               :ledger-sign {:command command
                             :success-id ::ledger-received-success
                             :error-id    ::ledger-received-failure
                             :cancel-id  ::ledger-received-canceled}})))

(re-frame/reg-event-db
 ::ledger-received-success
 (fn-traced [db [_ message]]
            (js/console.log "msg" message)
            (-> db
                (assoc-in [:apdu :status] :waiting)
                (assoc-in [:apdu :result] message))))

(re-frame/reg-event-db
 ::ledger-received-failure
 (fn-traced [db [_ ex]]
            (-> db
                (assoc-in [:apdu :status] :waiting)
                (assoc-in [:apdu :error] ex))))

(re-frame/reg-event-db
 ::ledger-received-canceled
 (fn-traced [db _]
            (-> db
                (assoc-in [:apdu :status] :waiting)
                (assoc-in [:apdu :error] nil))))
