(ns tsca-webapp.dom.effects
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(defn- change-iframe-url [id url]
  (if-let [el (js/document.getElementById id)]
    (set! (.-src el) url)
    (js/setTimeout #(change-iframe-url id url) 50)))

(defmulti dispatch :type)
(defmethod dispatch :iframe [{:keys [id url]}]
  (change-iframe-url id url))

(re-frame/reg-fx :dom dispatch)

