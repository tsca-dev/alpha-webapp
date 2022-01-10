(ns tsca-webapp.bil.effects
  (:require
   [re-frame.core :as re-frame]
   [cljs.core.match :refer-macros [match]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tsca-webapp.task.effects :as task]
   [oops.core :refer [oset!]]
   ["../common/mock.js" :as mock])
  (:require-macros [tsca-webapp.aii :refer [defcommand]]))

(declare bil)
(def loading-interval 250)
;; (def bil-url "/js/bil-jslib.js")
(def bil-url "/_tscalibs/bookapp-interface.js")

(defn- load-script [url object-name]
  (let [el (doto (js/document.createElement "script")
             (oset! :src url))]
    (js/document.body.appendChild el)
    (js/Promise.
     (fn [resolve reject]
       (letfn [(trial [] (if-let [obj (aget js/window object-name)]
                           (resolve obj)
                           (js/setTimeout trial loading-interval)))]
         (trial))))))

(defn- initialize []
  (-> (load-script bil-url "TSCABookappInterface")
      (.then (fn [obj]
               (def bil obj)))))

(re-frame/reg-fx
 :bil-initialize
 (fn [callback-ids]
   (task/callback callback-ids (initialize))))

(defn- parse-period [source]
  (-> (.split source ";")
      second))



(defn load-initial-values []
  (-> (bil.interpretSpiritStatus "basic.json")
      (.then (fn [[_ result-json]] result-json))
      (.then js/JSON.parse)
      (.then #(js->clj % :keywordize-keys true))))

(re-frame/reg-fx
 :bil
 (fn [{:keys [commands] :as callback-ids}]
   (task/callback callback-ids (load-initial-values))))
