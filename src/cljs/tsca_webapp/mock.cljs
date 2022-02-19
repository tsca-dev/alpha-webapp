(ns tsca-webapp.mock)

(defn- clj->str [o]
  (-> o clj->js js/JSON.stringify))

;; (def testnet (clj->str {:netident "testnet", :chain_id "NetXm8tYqnMWky1"}))
(def testnet (clj->str {:netident "testnet", :chain_id "NetXgbFy27eBoxH"}))

(def sahash-frozen "MOCK_sahash_proto0_frozen_genesis")
;; (def tmplhash-frozen "tmpL1Q7GJiqzmwuS3SkSiWbreXWsxWrk3Y")
(def tmplhash-frozen "tmpL1Q6jgUaZeMvDpuw5axKCwYXGUSvX2P")

(def target-spec-frozen (clj->str {:spellkind "spellofgenesis"
                                   :tmplhash tmplhash-frozen}))
(def spell-frozen "(frozen0.gen 1tz (alice bob) 2021-02-08T15:00:00.000Z)")
;; (def target-spec-frozen-withdraw
;;   (clj->str {:spellkind "spelltospirit"
;;              :sprthash "sprthash_proto0_withdraw"
;;              :tmplhash "tmpL1Q7GJiqzmwuS3SkSiWbreXWsxWrk3Y"}))
(def target-spec-frozen-withdraw
  (clj->str {:spellkind "spelltospirit"
             :sprthash "sprthash_proto0_withdraw"
             :tmplhash "tmpL1Q6jgUaZeMvDpuw5axKCwYXGUSvX2P"}))
