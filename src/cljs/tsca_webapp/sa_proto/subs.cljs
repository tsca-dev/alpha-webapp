(ns tsca-webapp.sa-proto.subs
  (:require [re-frame.core :as re]
            [tsca-webapp.mock :as mock]
            [tsca-webapp.common.subs-parts :as common]
            [clojure.string :as s]))

(re/reg-sub
 ::label
 :<- [::common/routing-params]
 (fn [params]
   (:label params)))

(re/reg-sub
 ::target-spec
 :<- [::common/routing-params]
 (fn [params]
   (get-in params [:query-params :for])))

(re/reg-sub
 ::sahash
 :<- [::common/routing-params]
 (fn [params]
   mock/sahash-frozen))

(re/reg-sub
 ::networks
 :<- [::common/routing-params]
 (fn [params]
   (get-in params [:query-params :networks])))

(def assistant-terms
  ["I have read through carefully all the materials above and completely understand the features, functions, and associated caveats of the described contract template. "
   "I understand and acknowledge that this service is provided as a technical demonstration and the service providers— including the TSCA team and the provider of this contract template—disclaims all warranties with regards to this service including all implied warranties of merchantability and fitness. in no events shall the providers be liable for any special, direct, indirect, or consequential damages or any damages whatsoever resulting from loss of use, data or profits, blah, blah, blah (legal language to be determined)"
   "I understand and acknowledge that the TSCA team strongly advice against using this service OUTSIDE the Carthagenet test network of the Tezos blockchain. Any loss of tokens of any kind is of the sole responsibility of myself and in no event the TSCA team or the provider of the contract template shall be held of liability of any kind."])

(re/reg-sub
 ::assistant-terms
 (fn [db]
   assistant-terms))

(re/reg-sub
 ::initial-agreements-assistant
 (fn [db]
   (common/make-array-same-element {:terms assistant-terms} :terms false)))

(re/reg-sub
 ::expected-agreements-assistant
 (fn [db]
   (common/make-array-same-element {:terms assistant-terms} :terms true)))

(re/reg-sub
 ::screen
 (fn [db]
   (:screen db)))

(re/reg-sub
 ::verifier-state
 :<- [::screen]
 (fn [screen]
   :verifier-loaded))

(re/reg-sub
 ::verifier
 :<- [::screen]
 (fn [screen]
   (:verifier screen)))

(re/reg-sub
 ::spell-builder
 :<- [::label]
 (fn [label]
   (case label
     "withdraw" (fn [o]
                  (let [{:strs [amount beneficiary]} (js->clj o)]
                   (str "(frozen0.withdraw "
                         amount "tz "
                         beneficiary
                        ")")))
     "genesis" (fn [o]
                 (let [{:strs [fund_amount unfrozen fund_owners]} (js->clj o)]
                   (str "(frozen0.gen "
                        fund_amount "tz "
                        "(" (s/join " " fund_owners) ") "
                        unfrozen
                        ")")))
     "unknown")
   ))

