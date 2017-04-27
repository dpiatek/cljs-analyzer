(ns ^:figwheel-always cljs-analyzer.core
  (:require-macros [hiccups.core :as hiccups])
  (:require [goog.events :as events]
            [hiccups.runtime]
            [clojure.spec :as s]))

(enable-console-print!)

(def not-nil? (complement nil?))

(defn constructor? [cstr inst]
  (if (not-nil? inst)
    (= (.. cstr -prototype -constructor) (.. inst -constructor))
    false))

(defn atom-map? [a]
  (map? @(atom {})))

(s/def ::byte-data (partial constructor? js/Uint8Array))
(s/def ::state atom-map?)
(s/def ::canvas-width number?)
(s/def ::canvas-height number?)
(s/def ::track string?)

(s/def ::audio-ctx (partial constructor? js/AudioContext))
(s/def ::render-ctx (partial constructor? js/CanvasRenderingContext2D))
(s/def ::track-src (partial constructor? js/MediaElementAudioSourceNode))
(s/def ::analyser (partial constructor? js/AnalyserNode))

(s/def ::config
  (s/keys
    :req-un
    [::byte-data ::width ::height ::track]))

(s/def ::contexts
  (s/keys
    :req-un
    [::audio-ctx ::render-ctx ::track-src ::analyser]))

(defn conform [spec data]
  (if (= (s/conform spec data) ::s/invalid)
     (throw (ex-info "Invalid input" (s/explain spec data)))
     data))

(def ^:dynamic animation-frame-id nil)

(def ids
  {:controls "controls"
   :track "track"
   :play "play"
   :pause "pause"
   :stop "stop"
   :canvas "canvas"})

(defn default-html [width height track]
  [:div
    [:div {:id (:controls ids)}
      [:button {:id (:play ids)} "Play"]
      [:button {:id (:pause ids)} "Pause"]
      [:button {:id (:stop ids)} "Stop"]]
    [:canvas
      {:id (:canvas ids)
       :width (str width "px")
       :height (str height "px")}]
    (if (not-nil? track)
      [:audio  {:id (:track ids) :src track} ""])])

(defn html [{:keys [width height track] :as config}]
  (hiccups/html
    (default-html width height track)))

(defn qid [id]
  (.querySelector js/document (str \# id)))

(defn insert-dom [html-string]
  (let [app (qid "app")]
    (set! (.-innerHTML app) html-string)
    app))

(defn clear-canvas [ctx {:keys [width height background]}]
  (set! (.-fillStyle ctx) (if (not-nil? background) background "rgb(255,255,255)"))
  (.fillRect ctx 0 0 width height))

(defn clear-frame! []
  (.cancelAnimationFrame js/window animation-frame-id)
  (set! animation-frame-id nil))

(defn play-callback [track frame]
  (.play track)
  (if (nil? animation-frame-id)
    (frame)))

(defn pause-callback [track _]
  (clear-frame!)
  (.pause track))

(defn stop-callback [track config state _]
  (clear-frame!)
  (.pause track)
  (set! (.-currentTime track) 0)
  (clear-canvas (:render-ctx @state) config))

(defn create-source [audio-ctx track]
  (.createMediaElementSource audio-ctx track))

(defn create-contexts [nodes]
  (let [audio-ctx (new js/AudioContext)] ; need webkit prefix here
    {:audio-ctx audio-ctx
     :render-ctx (.getContext (:canvas nodes) "2d")
     :analyser (.createAnalyser audio-ctx)}))

(defn connect-audio [{:keys [analyser audio-ctx track-src]} source fft-size]
  (set! (.-fftSize analyser) fft-size)
  (.connect source analyser)
  (.connect source (.-destination audio-ctx)))

(defn get-bytes!
  ([analyser byte-data]
   (.getByteFrequencyData analyser byte-data) byte-data)
  ([analyser byte-data domain]
   (if (= domain :frequency)
     (get-bytes! analyser byte-data)
     (do
       (.getByteTimeDomainData analyser byte-data)
       byte-data))))

(defn get-nodes [ids]
  (let [k (keys ids) v (vals ids)]
    (zipmap k (map qid v))))

(defn schedule! [frame derefed-state config]
  (set! animation-frame-id
    (.requestAnimationFrame
      js/window
      (partial frame derefed-state config))))

(defn frame [derefed-state config]
  ((:render config) derefed-state config)
  (schedule! frame derefed-state config))

(defn bind-events [config state nodes]
  (events/listen (:play nodes) "click"
    (partial play-callback (:track nodes)
      (partial frame @state config)))
  (events/listen (:pause nodes) "click"
    (partial pause-callback (:track nodes)))
  (events/listen (:stop nodes) "click"
    (partial stop-callback (:track nodes) config state)))

(defn setup [config state]
  (print "Setup")
  (conform ::config config)
  (insert-dom (html config))
  (let [nodes (get-nodes ids)
        contexts (create-contexts nodes)
        source (create-source (:audio-ctx contexts) (:track nodes))]
      (connect-audio contexts source (.-length (:byte-data config)))
      (swap! state merge contexts
        {:sample-rate (.-sampleRate (:audio-ctx contexts))
         :bin-count (.-frequencyBinCount (:analyser contexts))})
      (clear-canvas (:render-ctx contexts) config)
      (bind-events config state nodes)))

(defn teardown [config state]
  (print "Teardown")
  (conform ::config config)
  (let [nodes (:nodes config)]
    (clear-frame!)
    (.close (:audio-ctx @state))
    (events/removeAll (:play nodes) "click")
    (events/removeAll (:pause nodes) "click")
    (events/removeAll (:stop nodes) "click")
    (insert-dom "<div></div>")))

(defn reset [config state]
  (teardown config state)
  (setup config state))

(defn reload [config state]
  (print "Reload")
  (conform ::config config)
  (let [nodes (get-nodes ids)
        playing? (not-nil? animation-frame-id)
        frame-callback (partial frame @state config)]
    (clear-frame!)
    (events/removeAll (:play nodes) "click")
    (events/listen (:play nodes) "click"
      (partial play-callback (:track nodes) frame-callback))
    (if playing? (frame-callback))))
