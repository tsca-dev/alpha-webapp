(ns tsca-webapp.book-app.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(defn- parse-url-path []
  (let [[_ bahash sprthash] (.split js/window.location.pathname "/")]
    {:bahash bahash
     :sprthash sprthash}))

(re-frame/reg-event-fx
 ::open
 (fn-traced [{:keys [db]} _]
            {:db (-> db
                     (assoc :book-app {:status :loading-bil})
                     (assoc :routing-params (parse-url-path)))
             :bil-initialize {:success-id ::bil-ready
                              :error-id   ::bil-loading-error}}))

(re-frame/reg-event-fx
 ::bil-ready
 (fn-traced [{:keys [db]} _]
            {:db (-> db
                     (assoc-in [:book-app] {:status :loading-value}))
             :bil {:commands [{:type :initial-values}]
                   :success-id ::load-values-done
                   :error-id   ::bil-loading-error}}))

(re-frame/reg-event-db
 ::load-values-done
 (fn-traced [db [_ values]]
            (-> db
                (assoc-in [:book-app] {:status :done
                                       :values values}))))

(re-frame/reg-event-db
 ::bil-loading-error
 (fn-traced [db _]
            (prn db)
            (-> db
                (assoc-in [:book-app] {:status :error}))))

(re-frame/reg-event-fx
 ::change-iframe-url
 (fn-traced [{:keys [db]} [_ url]]
            {:dom {:type :iframe :id "assistant-modal" :url url}}))

