(ns tsca-webapp.chain-clerk.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-db
 ::go-to-next-step
 (fn-traced
  [db [_ _]]
  (let [current-step (get-in db [:clerk :current-step])
        next-step (case current-step
                    :user-confirmation :doit
                    :doit :doit)]
    (assoc-in db [:clerk :current-step] next-step))))

;; load-description
(re-frame/reg-event-fx
 ::load-description
 (fn-traced [{:keys [db]} _]
            (let [{:keys [sahash for spell]} (get-in db [:routing-params :query-params])]
              {:db (-> db
                       (assoc-in [:clerk :ledger :state :desc] {:status :loading}))
               :aii {:commands [{:type :description
                                 :for for :spell spell
                                 :sahash sahash}]
                     :success-id ::load-description-done
                     :error-id   ::load-description-error}})))

(re-frame/reg-event-db
 ::load-description-done
 (fn-traced [db [_ [desc]]]
            (-> db
                (assoc-in [:clerk :ledger :state :desc] {:status :done
                                                         :description desc}))))
(re-frame/reg-event-db
 ::load-description-error
 (fn-traced [db [_ ex]]
            (-> db
                (assoc-in [:clerk :ledger :state :desc] {:status :error
                                                         :error ex}))))

;; pubkey

(re-frame/reg-event-fx
 ::start-ledger-pubkey
 (fn-traced [{:keys [db]} _]
            {:db (-> db
                     (assoc-in [:clerk :ledger :state :pubkey ] {:status :finding-ledger
                                                                 :source-address nil}))
             :ledger-ready? {:success-id ::ledger-connected-pubkey
                             :error-id   ::error-occured-pubkey}}))

(re-frame/reg-event-fx
 ::ledger-connected-pubkey
 (fn-traced [{:keys [db]} _]
            {:db (-> db
                     (assoc-in [:clerk :ledger :state :pubkey :status] :confirming))
             :ledger-pk {:success-id ::find-ledger-source-address
                         :error-id   ::error-occured-pubkey}}))

(re-frame/reg-event-fx
 ::find-ledger-source-address
 (fn-traced [{:keys [db]} [_ public-key]]
            (js/console.log "public key" public-key)
            {:db (-> db
                     (assoc-in [:clerk :ledger :state :pubkey :status]
                               :finding-source-address)
                     (assoc-in [:clerk :ledger :state :pubkey :public-key]
                               public-key))
             :aii {:commands [{:type :source-address
                               :public-key public-key}]
                   :success-id ::found-source-address
                   :error-id ::error-occured-pubkey}}))

(re-frame/reg-event-db
 ::error-occured-pubkey
 (fn-traced [db [_ ex]]
            (-> db
                (assoc-in [:clerk :ledger :state :pubkey :status] :error)
                (assoc-in [:clerk :ledger :state :pubkey :error] ex))))


(re-frame/reg-event-db
 ::found-source-address
 (fn-traced [db [_ [source-address]]]
            (-> db
                (assoc-in [:clerk :ledger :state :pubkey :status] :found)
                (assoc-in [:clerk :ledger :state :pubkey :source-address] source-address))))


;; simulate
(re-frame/reg-event-fx
 ::start-simulate
 (fn-traced [{:keys [db]} [_ form ops]]
            (let [{:keys [for networks spell]} (get-in db [:routing-params :query-params])]
              {:db (-> db
                       (assoc-in [:clerk :ledger :state :sim] {:status :loading})
                       (assoc-in [:clerk :ledger :state :op ] {})
                       (assoc-in [:clerk :form] form))
               :aii {:commands [{:type :simulate
                                 :network (js/JSON.parse networks) :for (js/JSON.parse for)
                                 :spell spell :user-info form}]
                     :success-id ::simulation-done
                     :error-id   ::simulation-error}})))

(re-frame/reg-event-db
 ::simulation-done
 (fn-traced [db [_ [result]]]
            (-> db
                (assoc-in [:clerk :ledger :state :sim] {:status :done
                                                        :result result}))))
(re-frame/reg-event-db
 ::simulation-error
 (fn-traced [db [_ message]]
            (js/console.error "simulation error" message)
            (-> db
                (assoc-in [:clerk :ledger :state :sim] {:status :error
                                                        :message message}))))

;; op
(re-frame/reg-event-fx
 ::start-ledger-op
 (fn-traced [{:keys [db]} [_]]
            {:db (-> db
                     (assoc-in [:clerk :ledger :state :op ] {:status :finding-ledger}))
             :ledger-ready? {:success-id ::ledger-connecting-op
                             :error-id   ::error-occured-op}}))

(re-frame/reg-event-fx
 ::ledger-connecting-op
 (fn-traced [{:keys [db]} _]
            (let [{:keys [unsigned_transaction watermark]} (get-in db [:clerk :ledger :state :sim :result])]
              {:db (assoc-in db [:clerk :ledger :state :op :status] :signing)
               :ledger-sign {:watermark watermark
                             :operation-text unsigned_transaction
                             :success-id ::ledger-signed
                             :error-id   ::error-occured-op}})))

(re-frame/reg-event-fx
 ::ledger-signed
 (fn-traced [{:keys [db]} [_ {:keys [_txn signature]}]]
            (let [form (get-in db [:clerk :form])
                  unsigned_transaction (get-in db [:clerk :ledger :state :sim :result :unsigned_transaction])]
              {:db (assoc-in db [:clerk :ledger :state :op :status] :sending-op)
               :aii {:commands [(merge {:type :inject-operation :signature signature
                                        :txn unsigned_transaction}
                                       (select-keys form
                                                    [:public-key :source-address :network]))]
                     :success-id ::injection-requested
                     :error-id   ::error-occured-op}})))

(re-frame/reg-event-fx
 ::injection-requested
 (fn-traced [{:keys [db]} [_ [result]]]
            {:db (assoc-in db [:clerk :ledger :state :op :status] :waiting-for-done)
             :dispatch [::done [result]]}))

(re-frame/reg-event-db
 ::done
 (fn-traced [db [_ [result]]]
            (-> db
                (assoc-in [:clerk :ledger :state :op :status] :done)
                (assoc-in [:clerk :ledger :state :op :result] result))))

(re-frame/reg-event-db
 ::error-occured-op
 (fn-traced [db [_ ex]]
            (-> db
                (assoc-in [:clerk :ledger :state :op :status] :error)
                (assoc-in [:clerk :ledger :state :op :error] ex))))
