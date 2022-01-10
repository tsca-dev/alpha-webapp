(ns tsca-webapp.routes.events
  (:require
   [re-frame.core :as re-frame]
   [tsca-webapp.db :as db]
   [tsca-webapp.routes.routes :as routes]
   [tsca-webapp.task.events :as task]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))


(re-frame/reg-event-fx
 ::initialize-db
 (fn-traced [_ _]
            {:db (-> db/default-db
                     (assoc :aii {:state :loading}))
             :aii-initialize {:success-id ::aii-ready
                              :error-id   ::aii-loading-error}}))

(re-frame/reg-event-fx
 ::aii-ready
 (fn-traced [{:keys [db]} _]
            {:db (dissoc db :aii)
             :routing {}}))

(re-frame/reg-event-db
 ::aii-loading-error
 (fn-traced [db _]
            (assoc db :aii {:state :error})))

(re-frame/reg-event-fx
 ::set-active-panel
 (fn-traced [{:keys [db]} [_ active-panel params keep-url? :as event]]
            (let [initialize-event (routes/page-open-event active-panel)
                  cofx {:db (assoc db :active-panel active-panel
                                   :routing-params params)
                        :dispatch  [::task/cancel initialize-event]}]
              (if keep-url?
                cofx
                (assoc cofx :rewrite-url {:event event})))))
