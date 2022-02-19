(ns tsca-webapp.book.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-fx
 ::open-list
 (fn-traced [{:keys [db]} _]
            {:db (-> db
                     (assoc :screen {:state :loading}))
             :aii {:commands [{:type :all-book} {:type :default-network}]
                   :success-id ::book-list-loaded
                   :error-id   ::book-load-failed}}))

(re-frame/reg-event-fx
 ::open
 (fn-traced [{:keys [db]} _]
            (let [bookhash (get-in db [:routing-params :bookhash])
                  commands (->> [:book-info :book-status]
                                (map (fn [t] {:type t :bookhash bookhash})))]
              {:db (-> db
                       (assoc :screen {:state :loading}))
               :aii {:commands (cons {:type :default-network} commands)
                     :success-id ::book-info-loaded
                     :error-id   ::book-load-failed}})))

(re-frame/reg-event-db
 ::book-list-loaded
 (fn-traced [db [_ [info default-network]]]
            (-> db
                (assoc :screen {:state :loaded
                                :books info})
                (assoc :default-network default-network))))

(re-frame/reg-event-db
 ::book-info-loaded
 (fn-traced [db [_ [default-network info status]]]
            (-> db
                (assoc :screen {:state :loaded
                                :info info
                                :status status})
                (assoc :default-network default-network))))

(re-frame/reg-event-db
 ::book-load-failed
 (fn-traced [db [_ ex]]
            (-> db
                (assoc :screen {:state :error}))))

(re-frame/reg-event-fx
 ::change-iframe-url
 (fn-traced [{:keys [db]} [_ url]]
            {:dom {:type :iframe :id "assistant-modal" :url url}}))

