(ns tsca-webapp.aii.effects
  (:require
   [re-frame.core :as re-frame]
   [cljs.core.match :refer-macros [match]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tsca-webapp.task.effects :as task]
   [oops.core :refer [oset!]]
   ["../common/mock.js" :as mock]
   [tsca-webapp.mock :as m])
  (:require-macros [tsca-webapp.aii :refer [defcommand]]))

(declare aii)
(def loading-interval 250)
;; (def aii-url "https://devapi.tsca.kxc.io/aii-jslib.js")
;; (def aii-url "/js/aii-jslib.js")
(def aii-url "/_tscalibs/aii-jslib.js")

(defn bookapp-url [spirit-hash]
  (aii.Proto0.bookAppUrlForSpirit spirit-hash))

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
  (-> (load-script aii-url "TSCAInternalInterface")
      (.then (fn [obj]
               (def aii obj)))))

(defcommand get-default-network []
  (js/console.log "get-default-network: called")
  (aii.RefMaster.defaultTezosNetwork))

(defcommand bookhash-list []
  (aii.RefMaster.listAdvertizedBooks))

(defcommand book-info [bookhash]
  (aii.InfoBank.getBookEntry (js/String bookhash)))

(defcommand book-status [bookhash]
  (aii.RefMaster.getBookStatus (js/String bookhash)))

(defcommand provider-info [providerident]
  (aii.RefMaster.getProviderInfo  (js/String providerident)))

(defn book-info-and-provider [bookhash]
  (-> (book-info bookhash)
      (.then (fn [info]
               (-> (provider-info (:provider info))
                   (.then (fn [provider-detail]
                            (assoc info :provider-detail provider-detail))))))))

(defcommand forge-operation [js-ops-model]
  (aii.Proto0.forgeOperation js-ops-model))

(defcommand calculate-address-from-public-key [public-key]
  (aii.TezosUtils.calculateaddressfromledgerpublickey public-key))

(defcommand get-spell-verifier [sahash]
  (aii.Proto0.getSpellVerifier (js/String sahash)))

(defn generate-spell-verifier [sahash]
  (-> (get-spell-verifier sahash)
      (.then #(:verifier %))))

(defn- build-process-request [network for spell user-info]
  (if (= (.-spellkind for) "spellofgenesis")
    {:request-api aii.Proto0.processGenesisRequest
     :simulate-api aii.Proto0.simulateGenesis
     :request {:network network
               :template (.-tmplhash for)
               :requester (:source-address user-info)
               :name (:name user-info)
               :email (:e-mail user-info)
               :spell spell}}

    {:request-api aii.Proto0.processInvocationRequest
     :simulate-api aii.Proto0.simulateInvocation
     :request {:network network
               :spirit (.-sprthash for)
               :requester (:source-address user-info)
               :name (:name user-info)
               :email (:e-mail user-info)
               :spell spell}}))

(defn- book-app-info [proc]
  (some->
   (.-sprthash proc)
   aii.Proto0.bookAppUrlForSpirit))

(comment
  (let [{:keys [request-api simulate-api request]} (build-process-request (js/JSON.parse m/testnet)
                                                                          (clj->js {:spellkind "spellofgenesis"
                                                                                    :tmplhash "tmpL1Q7GJiqzmwuS3SkSiWbreXWsxWrk3Y"})
                                                                          m/spell-frozen
                                                                          {:name "Ichiro Sakai"
                                                                           :e-mail "i.sakai@example.org"
                                                                           :source-address "tz1gmHNqdXuM5bf8FU9dNaACuAqgRA9VLGUm"
                                                                           ;; "tz1NQ5Fk7eJCe1zGmngv2GRnJK9G1nEnQahQ"
                                                                           })]
    (def request-api request-api)
    (def request (clj->js request)))
  
  (.then (request-api request)
         #(def proc %))
  (js->clj proc)

  (some->
   (.-sprthash proc)
   TSCAInternalInterface.Proto0.bookAppUrlForSpirit
   (.then #(def book-url %)))
  
  (-> (TSCAInternalInterface.Proto0.simulateGenesis request proc)
      (.then
       (fn [[result-type message]]
         (if (= result-type "SimulationFailed")
           (js/Promise.reject (get (js->clj message) "error_message"))
           message)))
      (.then #(def sim-result (js->clj %)))
      (.catch prn))

  (aii.Helpers.formatCliInstructionIntoString (.-cli_instructions proc))

  (str (get sim-result "watermark")
       (get sim-result "unsigned_transaction")))

(defn- simulate [network for spell user-info]
  (let [{:keys [request-api simulate-api request]} (build-process-request network for spell user-info)
        request (clj->js request)]
    (-> (request-api request)
        (.then (fn [proc]
                 (let [instruction (aii.Helpers.formatCliInstructionIntoString (.-cli_instructions proc))]
                   (js/console.log "aii:simulate:proc" proc)
                   (js/console.log "aii:simulate:(book-app-info proc)" (book-app-info proc))
                   (-> (book-app-info proc)
                       (.then (fn [book-app]
                                (-> (simulate-api request proc)
                                    (.then (fn [[result-type result]]
                                             (if (= result-type "SimulationFailed")
                                               (js/Promise.reject (get (js->clj result) "error_message"))
                                               (assoc (js->clj result :keywordize-keys true)
                                                      :instruction instruction
                                                      :sprthash (.-sprthash proc)
                                                      :book-app (js->clj book-app :keywordize-keys true)))))))))))))))

(defcommand check-operation-injection [inject-token]
  (aii.Proto0.checkOperationInjection #js {:injtoken inject-token}))

(defn- same-fee? [{:keys [networkfees templatefees rawamount]} fees]
  (and (= networkfees (:networkfees fees))
       (= templatefees (:templatefees fees))
       (= rawamount (:rawamount fees))))

(defcommand inject-operation [txn signature js-network]
  (aii.Proto0.injectOperation #js {:network js-network
                                   :unsigned_transaction txn
                                   :signature signature}))

(comment
  (let [txn (js/String "8b03231294f42505690aeb53bbafad508ddf178a38eb15ea231b650db394455e6c001e44b16562327dd33068e4731764191461bcccbecb4cbdbf4f9be005e50a80897a01aa0cd86d31f79ef4fa5934134dd768e733816cc700ff000000007805050505050807070303070707070100000015737072743164616d337363746c67347a77706468740000070707070a00000010e80b9b85eb32a7143b16ae8a3473b8700a0000002005070702000000120100000005616c6963650100000003626f6200b0c78a820c07070080897a070700a0843d00a0843d")
        signature (js/String "a57e6eff8c043eb4a2758355109b7f78943a5c546afff82f8f4a78e5715b1c81e5fa081dc07a580a4264e96cb32d1ca511ea576b30b289768588bf9405943b04")
        js-network #js {:netident "testnet", :chain_id "NetXm8tYqnMWky1"}]
    (-> (TSCAInternalInterface.Proto0.injectOperation #js {:network js-network
                                                           :unsigned_transaction txn
                                                           :signature signature})
        (.then #(def inj-result %)))))

(defn- inject [txn public-key source-address signature js-network]
  (-> (inject-operation txn signature js-network)
      (.then (fn [[result-type result]]
               (prn "pass" result-type result)
               (if (= result-type "InjectionFailed")
                 (js/Promise.reject (:error_message result))
                 (js->clj result :keywordize-keys true))))))

(defcommand template-current-version [tmplhash]
  (aii.RefMaster.templateCurrentVersion #js {:tmplhash tmplhash}))

(defn all-book-info []
  (-> (bookhash-list)
      (.then (fn [books]
               (->> books
                    (map #(:bookhash %))
                    (map book-info)
                    (js/Promise.all))))))

(defcommand generate-description [for spell sahash]
  (clojure.string/join "\n" [(str "sahash: " sahash)
                             (str "spell: " spell)
                             (str "for: " for)]))

(re-frame/reg-fx
 :aii-initialize
 (fn [callback-ids]
   (task/callback callback-ids (initialize))))

(defn- repeat-until [f pred interval]
  (js/Promise. (fn [resolve reject]
                 (letfn [(step []
                           (try
                             (-> (f)
                                 (.then (fn [ret]
                                          (if (pred ret)
                                            (resolve ret)
                                            (js/setTimeout #(step) interval))))
                                 (.catch (fn [ex] (reject ex))))
                             (catch :default e (reject e))))]
                   (step)))))

(def suffix "+successful"
  ;; "+timeout"
  ;; "+failed"
  ;; "+toofreq"
  )
(def dummy-suffix (atom (cycle ["" "" "" suffix])))

(defn- waiting-for-done [injection-token interval]
  (-> (repeat-until #(let [suffix (ffirst (swap-vals! dummy-suffix rest))]
                       (check-operation-injection (str injection-token suffix)))
                    (fn [result]
                      (if (= (:status result) "progressing")
                        (js/console.log (str "progressing:" injection-token
                                             "(" interval ")")
                                        (clj->js result))
                        result))
                    interval)
      (.then (fn [result]
               (if (= (:status result) "successful")
                 result
                 (do
                   (js/console.error "injection error" (or (:logs result) (:reason result)))
                   (js/Promise.reject {:type (keyword (str "op-" (:status result)))
                                       :reason (:reason result)})))))))

(defn- process-single [command]
  (match [command]
         [{:type :all-book}] (all-book-info)
         [{:type :default-network}] (get-default-network)
         [{:type :book-info :bookhash bookhash}] (book-info-and-provider bookhash)
         [{:type :book-status :bookhash bookhash}] (book-status bookhash)
         [{:type :provider-info :providerident providerident}] (provider-info providerident)
         [{:type :source-address :public-key pk}] (calculate-address-from-public-key pk)
         [{:type :simulate :network network :for for :spell spell :user-info user-info}] (simulate network for spell user-info)
         [{:type :spell-verifier :sahash sahash}] (generate-spell-verifier sahash)
         [{:type :description :sahash sahash :for for :spell spell}] (generate-description for spell sahash)
         [{:type :inject-operation :signature signature
           :txn txn :public-key public-key :source-address source-address
           :network js-network}] (inject txn public-key source-address signature js-network)
         [{:type :confirm-injection :injection-token injection-token :interval interval}] (waiting-for-done injection-token interval)
         :else (js/Promise.reject (str "unknown command" command))))

(re-frame/reg-fx
 :aii
 (fn [{:keys [commands] :as callback-ids}]
   (let [promise (js/Promise.all (map process-single commands))]
     (task/callback callback-ids promise))))
