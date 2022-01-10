(ns tsca-webapp.common.util)

(defn mutez->tez [v]
  (/ v 1000000))

(defn format-as-tez [mutez]
  (str (mutez->tez mutez) "ꜩ"))
