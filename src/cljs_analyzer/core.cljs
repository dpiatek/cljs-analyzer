(ns cljs-analyzer.core
  (:require-macros [hiccups.core :as hiccups])
  (:require [goog.events :as events]))

(enable-console-print!)

(defn html []
  (hiccups/html
    [:div
      [:audio  {:id "track" :crossOrigin "anonymous" :src "http://rumyrashead.com/media/screwUp.mp3"}]]
    [:button {:id "play" :style "margin-right: 5px"} "Plays"]
    [:button {:id "pause"} "Pause"]
    [:pre {:id "test"}]
    [:canvas {:id "canvas" :style "display: block"}]))

(defn qid [id]
  (.querySelector js/document id))

(defn insert-dom [html]
  (let [app (qid "#app")]
    (set! (.-innerHTML app) html)
    app))

(defn play-track [track-el _]
  (.play track-el))

(defn pause-track [track-el _]
  (.pause track-el))

(defn setup [audio]
  (print "setup")
  (swap! audio assoc :context (new js/AudioContext))
  (let [app (insert-dom (html))
        play-btn (qid "#play")
        pause-btn (qid "#pause")
        track-el (qid "#track")
        canvas (qid "#test")
        audio-api (:context @audio)
        track (.createMediaElementSource audio-api track-el)
        analyser-node (.createAnalyser audio-api)]
      (set! (.-fftSize analyser-node) 64)
      (.connect track analyser-node)
      (.connect track (.-destination audio-api))
      (.connect analyser-node (.-destination audio-api))
      (events/listen play-btn "click" (partial play-track track-el))
      (events/listen pause-btn "click" (partial pause-track track-el))
      {:play-btn play-btn
       :pause-btn pause-btn
       :analyser-node analyser-node
       :canvas canvas
       :app app}))

(defn teardown [audio-api]
  (print "teardown")
  (events/removeAll (qid "#play") "click")
  (events/removeAll (qid "#pause") "click")
  (set! (.-innerText (qid "#app")) ""))

; API
(defonce freq-data (js/Uint8Array. 32))
(defonce audio (atom {:context (new js/AudioContext)}))

(defn get-bytes [analyser-node freq-data]
  (.getByteFrequencyData analyser-node freq-data)
  freq-data)

(defn render [canvas data]
  (set! (.-innerHTML canvas) data))

(defn fill [analyser-node canvas freq-data]
  (.requestAnimationFrame js/window (partial fill analyser-node canvas freq-data))
  (render canvas (get-bytes analyser-node freq-data)))

(defn init [audio fill freq-data]
  (let [setup-result (setup audio)]
    (fill (:analyser-node setup-result) (:canvas setup-result) freq-data)))

(defn on-js-reload []
  (teardown (:context @audio))
  (init audio fill freq-data))
