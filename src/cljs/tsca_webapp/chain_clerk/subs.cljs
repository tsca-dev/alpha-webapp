(ns tsca-webapp.chain-clerk.subs
  (:require [re-frame.core :as re]
            [tsca-webapp.mock :as mock]
            [clojure.string :as s]
            [tsca-webapp.common.subs-parts :as common]))

(re/reg-sub
 ::clerk
 (fn [db]
   (:clerk db)))

(re/reg-sub
 ::current-step

 :<- [::clerk]
 (fn [clerk]
   (:current-step clerk)))

(re/reg-sub
 ::step-indicator
 :<- [::current-step]

 (fn [current-step]
   (let [steps [:user-confirmation :address-selection :simulation :run]]
     (->> steps
          (map-indexed
           (fn [i step]
             {:active? (= current-step step) :display (str "Step " (inc i))}))))))

(re/reg-sub
 ::step-visible?
 :<- [::current-step]
 (fn [current-step [_ step-id]]
   (= current-step step-id)))

(re/reg-sub
 ::ledger
 (fn [db _]
   (get-in db [:clerk :ledger])))

(re/reg-sub
 ::ledger-pubkey-state
 :<- [::ledger]
 (fn [ledger _]
   (get-in ledger [:state :pubkey])))

(re/reg-sub
 ::ledger-sim-state
 :<- [::ledger]
 (fn [ledger _]
   (get-in ledger [:state :sim])))

(re/reg-sub
 ::ledger-op-state
 :<- [::ledger]
 (fn [ledger _]
   (get-in ledger [:state :op])))

(re/reg-sub
 ::ledger-source-address
 :<- [::ledger-pubkey-state]
 (fn [state _]
   {:source (:source-address state)
    :public-key (:public-key state)}))

(re/reg-sub
 ::ledger-pubkey-status
 :<- [::ledger-pubkey-state]
 (fn [state _]
   (:status state)))

(re/reg-sub
 ::ledger-pubkey-loading?
 :<- [::ledger-pubkey-status]
 (fn [state _]
   (#{:finding-ledger :loading :finding-source-address} state)))

(re/reg-sub
 ::ledger-pubkey-message
 :<- [::ledger-pubkey-status]
 (fn [status _]
   (case status
     :finding-ledger "Connect your Ledger and launch Tezos App ..."
     :loading "loading public key ..."
     :confirming "Click OK on your Ledger"
     :finding-source-address "loading source address ..."
     :found "Source Address Found!"
     :error "ERROR!"
     "(unknown state)")))

(re/reg-sub
 ::ledger-pubkey-error
 :<- [::ledger-pubkey-state]
 (fn [state _]
   (:error state)))

(defn- error-message [{:keys [type reason]}]
  (or reason
      (case type
        :time-out "time out"
        :busy "device busy"
        :denied-by-user "operation denied"
        (:transport-status-error :transport-error) ""
        "")))

(re/reg-sub
 ::ledger-pubkey-error-message
 :<- [::ledger-pubkey-error]
 (fn [err _]
   (error-message err)))

(re/reg-sub
 ::network
 :<- [::common/query-params]
 (fn [_]
   (js/JSON.parse mock/testnet)))

(re/reg-sub
 ::operation
 :<- [::common/query-params]
 (fn [{:keys [for spell networks]}]
   (js/console.log "netowrks: " networks)
   (js/console.log "parsed netowrks: " (js/JSON.parse networks))
   (js/console.log "for: " for)
   (js/console.log "parsed for: " (js/JSON.parse (clj->js for)))
   (let [parsed-networks (str (js/JSON.parse networks))]
     (clj->js {:target  (js/JSON.parse for)
        :spell   spell
        :networks parsed-networks}))
   ))

(re/reg-sub
 ::ledger-sim-result
 (fn [db _]
   (get-in db [:clerk :ledger :state :sim :result])))

(re/reg-sub
 ::ledger-sim-detail
 :<- [::ledger-sim-result]
 (fn [{:keys [simulation_output]}]
   simulation_output))


(re/reg-sub
 ::ledger-sim-cli-description
 :<- [::ledger-sim-result]
 (fn [{:keys [instruction]}]
   instruction))

(re/reg-sub
 ::ledger-sim-book-app
 :<- [::ledger-sim-result]
 (fn [{:keys [book-app]}]
   book-app))

(re/reg-sub
 ::ledger-sim-status
 :<- [::ledger-sim-state]
 (fn [state _]
   (:status state)))

(re/reg-sub
 ::ledger-sim-finished?
 :<- [::ledger-sim-status]
 (fn [state _]
   (= state :done)))

(re/reg-sub
 ::ledger-sim-error?
 :<- [::ledger-sim-status]
 (fn [state _]
   (= state :error)))

(re/reg-sub
 ::ledger-sim-message
 :<- [::ledger-sim-status]
 (fn [state _]
   (case state
     :loading "Simulating..."
     :error   "Simulation failed."
     :done    "Simulation finished."
     "")))

(re/reg-sub
 ::ledger-sim-errmsg
 :<- [::ledger-sim-state]
 (fn [state _]
   (case (:status state)
     :loading "Simulating..."
     :error   (:message state)
     :done    "Simulation finished."
     "")))

(re/reg-sub
 ::ledger-op-status
 :<- [::ledger-op-state]
 (fn [state _]
   (:status state)))
(re/reg-sub
 ::ledger-op-loading?
 :<- [::ledger-op-status]
 (fn [state _]
   (#{:finding-ledger :signing :sending-op} state)))

(re/reg-sub
 ::ledger-op-cancellable?
 :<- [::ledger-op-status]
 (fn [state _]
   (not=  state :done)))

(re/reg-sub
 ::ledger-op-spirit-hash
 :<- [::ledger-sim-result]
 (fn [{:keys [sprthash]}]
   sprthash))

(re/reg-sub
 ::ledger-op-links
 :<- [::ledger-sim-result]
 (fn [{:keys [book-app]}]
   [{:label (:title book-app) :link (:url book-app)}]))

(re/reg-sub
 ::ledger-op-message
 :<- [::ledger-op-status]
 (fn [status _]
   (case status
     :finding-ledger "Connect your Ledger and launch Tezos App..."
     :confirming "confirming..."
     :signing "Click OK on your Ledger"
     :sending-op "sending the operation  ..."
     :waiting-for-done "processing..."
     :done "Your operation done successfully!"
     :error "ERROR!"
     "(unknown state)")))
(re/reg-sub
 ::ledger-op-error
 :<- [::ledger-op-state]
 (fn [state _]
   (:error state)))
(re/reg-sub
 ::ledger-op-error-message
 :<- [::ledger-op-error]
 (fn [err _]
   (error-message err)))

(re/reg-sub
 ::desc-state
 (fn [db]
   (get-in db [:clerk :ledger :state :desc])))

(re/reg-sub
 ::desc-status
 :<- [::desc-state]
 (fn [state]
   (:status state)))

(re/reg-sub
 ::description-style
 :<- [::desc-status]
 (fn [status]
   (case status
     :done "text-strong"
     :error "text-strong"
     "")))

(re/reg-sub
 ::description
 :<- [::desc-state]
 :<- [::common/query-params]
 (fn [[{:keys [status description]} {:keys [for spell sahash]}]]
   (s/join "\n" [(str "sahash: " sahash)
                            (str "target: " for)
                            (str "spell: " spell)])
   ;; (case status
   ;;   :loading "loading..."
   ;;   :done    description
   ;;   :error   (s/join "\n" [(str "sahash: " sahash)
   ;;                          (str "target: " for)
   ;;                          (str "spell: " spell)])
   ;;   "")
   ))

(re/reg-sub
 ::form
 (fn [db]
   (get-in db [:clerk :form])))

(re/reg-sub
 ::ledger-available?
 :<- [::form]
 (fn [form]
   (boolean (:public-key form))))
