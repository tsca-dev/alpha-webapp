(ns tsca-webapp.book-app.subs
  (:require [re-frame.core :as re]
            [cljs.core.match :refer-macros [match]]
            [tsca-webapp.common.subs-parts :as common]))

(re/reg-sub
 ::book-app
 (fn [{:keys [book-app]}]
   book-app))

(re/reg-sub
 ::status
 :<- [::book-app]
 (fn [{:keys [status]}]
   (or (#{:done :error} status)
       :loading)))

(re/reg-sub
 ::parameters
 :<- [::book-app]
 (fn [{:keys [values]}]
   [{:title "Fund Balance" :value (:balance values)}
    {:title "Frozen Until" :value (:unfrozen values)}
    {:title "Owners" :value (clojure.string/join "," (:owners values))}
    {:title "Contract" :value (:contract values)}]))

(re/reg-sub
 ::button-label
 :<- [::common/routing-params]
 (fn [_]
   "Withdraw"))

