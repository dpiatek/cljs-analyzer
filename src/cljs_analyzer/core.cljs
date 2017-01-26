(ns cljs-analyzer.core
  (:require-macros [hiccups.core :as hiccups])
  (:require [goog.events :as events]
            [hiccups.runtime]))

(enable-console-print!)

(defn html [w h track-url]
  (hiccups/html
    [:div [:audio  {:id "track" :src track-url}]]
    [:button {:id "play" :style "margin-right: 5px"} "Play"]
    [:button {:id "pause"} "Pause"]
    [:canvas
      {:id "canvas"
       :style "display: block"
       :width (str w "px")
       :height (str h "px")}]))

(defn qid [id]
  (.querySelector js/document id))

(defn insert-dom [html-string]
  (let [app (qid "#app")]
    (set! (.-innerHTML app) html-string)
    app))

(defn play-track [track-el _]
  (.play track-el))

(defn pause-track [track-el _]
  (.pause track-el))

(defn create-contexts [track-el canvas-el]
  (let [context (new js/AudioContext)]
    {:context context
     :canvas-context (.getContext canvas-el "2d")
     :track-src (.createMediaElementSource context track-el)
     :analyser (.createAnalyser context)}))

(defn connect-audio [{:keys [analyser context track-src]} fft-size]
  (set! (.-fftSize analyser) fft-size)
  (.connect track-src analyser)
  (.connect track-src (.-destination context))
  (.connect analyser (.-destination context)))

(defn setup [{:keys [root width height track freq-data]}]
  (print "Setup")
  (let [app (insert-dom (html width height track))
        play-btn (qid "#play")
        pause-btn (qid "#pause")
        track-el (qid "#track")
        canvas-el (qid "#canvas")
        config (create-contexts track-el canvas-el)]
      (swap! root merge config)
      (connect-audio config (.-length freq-data))
      (events/listen play-btn "click" (partial play-track track-el))
      (events/listen pause-btn "click" (partial pause-track track-el))))

(defn teardown [{:keys [context]}]
  (print "Teardown")
  (.close context)
  (events/removeAll (qid "#play") "click")
  (events/removeAll (qid "#pause") "click")
  (set! (.-innerText (qid "#app")) ""))

; API
(defn get-bytes [analyser freq-data]
  (.getByteFrequencyData analyser freq-data)
  [freq-data (.-frequencyBinCount analyser)])

(defn render [{w :width h :height :as config} ctx [data buffer-length]]
  (set! (.-fillStyle ctx) "rgb(255, 255, 255)")
  (.fillRect ctx 0 0 w h)
  (loop [i 0 x 0]
    (let [bar-height (aget data i)
          bar-width (* (/ w buffer-length) 1)]
      (set! (.-fillStyle ctx) (str "rgb(255, 50, 50)"))
      (.fillRect ctx x (- h bar-height) bar-width bar-height)
      (when (< i buffer-length)
        (recur (inc i) (+ x bar-width))))))

(defn frame [{:keys [analyser canvas-context] :as r} {:keys [root freq-data] :as config}]
  (.requestAnimationFrame js/window (partial frame r config))
  (render config canvas-context (get-bytes analyser freq-data)))

(defn init [{:keys [root] :as config}]
  (setup config)
  (frame @root config))

(defonce config
  {:freq-data (js/Uint8Array. 4096)
   :root (atom {})
   :width 1024
   :height 256
   :track "/audio/heavy-soul-slinger.mp3"})

(init config)

(defn on-js-reload []
  (teardown (deref (:root config)))
  (init config))
