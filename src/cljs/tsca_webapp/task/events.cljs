(ns tsca-webapp.task.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-fx
 ::cancel
 (fn-traced [{:keys [db]} [_ event]]
            {:process-cancel {:callback event}}))

(defn cancel-all []
  (re-frame/dispatch [::cancel]))
