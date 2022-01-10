(ns tsca-webapp.task.effects
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ["../common/mock.js" :as mock]))

(defonce processes (atom {}))

(defn- call-after [f]
  (js/setTimeout f 0))

(defn- register-process
  [id promise success-id error-id cancel-func cancel-id]
  (let [dispatch   (fn [id event-id x]
                     (if (get @processes id)
                       (do (swap! processes #(dissoc % id))
                           (when event-id (re-frame/dispatch [event-id x])))

                       (js/console.log (str "id:" id " canceled already")
                                       event-id
                                       x)))
        id         (or id (random-uuid))
        on-success (fn [result]
                     (dispatch id success-id result))
        on-error   (fn [ex]
                     (js/console.error "Error" (str {:success-id success-id
                                             :error-id error-id
                                             :cancel-id cancel-id})
                                       ex)
                     (dispatch id error-id ex))
        swapping   (fn [table]
                     (call-after
                      (fn []
                        (-> promise
                            (.then on-success)
                            (.catch on-error))))
                     (assoc table id {:promise promise
                                      :cancel-func cancel-func
                                      :cancel-id cancel-id}))]
    (swap! processes swapping)))

(defn callback [{:keys [success-id error-id cancel-id cancel-func]} promise]
  (register-process nil promise success-id error-id cancel-func cancel-id))

(defn cancel-process [id]
  (swap! processes (fn [table]
                     (when-let [{:keys [cancel-func cancel-id]} (get table id)]
                       (when cancel-func (cancel-func))
                       (when cancel-id (re-frame/dispatch [cancel-id])))
                     (dissoc table id))))

(defn cancel-all []
  (swap! processes (fn [table]
                     (doseq [{:keys [cancel-func cancel-id] :as m} (vals table)]
                       (when cancel-func (cancel-func))
                       (when cancel-id (re-frame/dispatch [cancel-id])))
                     {})))

(re-frame/reg-fx
 :process-cancel
 (fn [{:keys [callback]}]
   (cancel-all)
   (when callback (re-frame/dispatch callback))))

(re-frame/reg-fx
 :sleep
 (fn [{:keys [success-id cancel-id return-value sec task-id]}]
   (let [x (mock/cancelableSleep (* 1000 sec) return-value)]
     (register-process task-id
                       x.promise success-id nil
                       x.cancel cancel-id))))

(re-frame/reg-fx
 :sleep-force
 (fn [{:keys [success-id cancel-id return-value sec task-id]}]
   (register-process task-id
                     (mock/sleep (* 1000 sec) return-value) success-id nil
                     nil cancel-id)))

