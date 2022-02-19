(ns tsca-webapp.book.subs
  (:require [re-frame.core :as re-frame]
            [tsca-webapp.common.subs-parts :as common]))

(def help-link-of-status "https://tezos.foundation/")

(re-frame/reg-sub
 ::books
 (fn [db _]
   (get-in db [:screen :books])))

(re-frame/reg-sub
 ::books-loading-state
 (fn [db _]
   (or (get-in db [:screen :state]) :loading)))

(re-frame/reg-sub
 ::books-loaded?
 :<- [::books-loading-state]
 (fn [state _]
   (= :loaded state)))

(re-frame/reg-sub
 ::loading-message
 :<- [::books-loading-state]
 (fn [state _]
   (case state
     :loading "loading ..."
     :error   "load failed."
     (str "unknown state: " state))))



(re-frame/reg-sub
 ::routing-params
 (fn [db]
   (:routing-params db)))

(defn- parse-book-detail [{:keys [bookhash title synopsis provider-detail tmplhash
                                  contract_parameters_en contract_terms_en contract_caveats_en]}]
  {:title title
   :synopsis synopsis
   :provider-detail provider-detail
   :template-details {:contract-parameters contract_parameters_en
                      :contract-terms contract_terms_en
                      :caveats        contract_caveats_en}
   :bookhash bookhash
   :tmplhash tmplhash})

(re-frame/reg-sub
 ::screen
 (fn [db _]
   (-> db :screen)))

(re-frame/reg-sub
 ::genesis-network
 (fn [db _] (:default-network db)))

(re-frame/reg-sub
 ::book-info
 :<- [::screen]
 (fn [screen _]
   (-> screen :info parse-book-detail)))

(re-frame/reg-sub
 ::book-charge
 :<- [::screen]
 (fn [screen _]
   (:charges (:status screen))))

(re-frame/reg-sub
 ::book-status
 :<- [::screen]
 (fn [screen _]
   (:status screen)))

(re-frame/reg-sub
 ::book-contract-complexity
 :<- [::book-status]
 (fn [status _]
   {:value (get-in status [:review_results :contract_complexity])
    :url help-link-of-status}))

(re-frame/reg-sub
 ::book-certification-status
 :<- [::book-status]
 (fn [status _]
   {:value (get-in status [:review_results :certification_status])
    :url help-link-of-status}))

(defn- make-initial-array [xs]
  (mapv #(-> % :mandatory_consensus not) xs))

(re-frame/reg-sub
 ::initial-agreements
 :<- [::book-info]
 (fn [{:keys [contract_parameters_en contract_caveats_en]} _]
   {:contract-terms (make-initial-array contract_parameters_en)
    :caveats (make-initial-array        contract_caveats_en)}))

(re-frame/reg-sub
 ::book-provider
 :<- [::book-info]
 (fn [{:keys [provider-detail]} _]
   {:value (:display_name provider-detail)
    :url (:website provider-detail)}))

(re-frame/reg-sub
 ::expected-agreements
 :<- [::book-info]
 (fn [{:keys [template-details]} _]
   (merge (common/make-array-same-element template-details :contract-terms true)
          (common/make-array-same-element template-details :caveats true))))

(re-frame/reg-sub
 ::specifictions
 :<- [::screen]
 (fn [screen]
   (get-in screen [:info :specifications])))

