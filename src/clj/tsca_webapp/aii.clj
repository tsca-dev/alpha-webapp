(ns tsca-webapp.aii)

(defmacro defcommand [name args & body]
  `(defn- ~name ~args (-> ~@body
                          (js/Promise.resolve)
                          (.then #(cljs.core/js->clj % :keywordize-keys true)))))


