(ns tsca-webapp.sa-proto.events
  (:require
   [re-frame.core :as re-frame]
   [cljs.core.match :refer-macros [match]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-fx
 ::generate-verifier
 (fn-traced [{:keys [db]} _]
            (let [label (get-in db [:routing-params :label])
                  sahash (case label
                           "withdraw" "MOCK_sahash_proto0_frozen_withdraw"
                           "genesis" "MOCK_sahash_proto0_frozen_genesis"
                           (throw (str "Unknown label:" label)))]
              {:db (-> db
                       (assoc :screen {:state :verifier-loading}))})))

(re-frame/reg-event-db
 ::verifier-ready
 (fn-traced [db [_ [verifier]]]
            (-> db
                (assoc :screen {:state :verifier-loaded
                                :verifier verifier}))))

(re-frame/reg-event-db
 ::verifier-loading-error
 (fn-traced [db [ex]]
            (-> db
                (assoc :screen {:state :verifier-loading-error
                                :message ex}))))

