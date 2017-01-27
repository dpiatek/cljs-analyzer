(ns cljs-analyzer.wave
  (:require [cljs-analyzer.core :as c]))

; API
(defn get-bytes [analyser freq-data]
  (.getByteFrequencyData analyser freq-data)
  [freq-data (.-frequencyBinCount analyser)]) ; bin count does not change!

(defn render [{w :width h :height :as config} ctx [freq-data buffer-length]]
  (set! (.-fillStyle ctx) "rgb(255, 255, 255)")
  (.fillRect ctx 0 0 w h)
  (loop [i 0 x 0]
    (let [bar-height (aget freq-data i)
          bar-width (* (/ w buffer-length) 1)]
      (set! (.-fillStyle ctx) (str "rgb(200, 50, 50)"))
      (.fillRect ctx x (- h bar-height) bar-width bar-height)
      (when (< i buffer-length)
        (recur (inc i) (+ x bar-width))))))

(defn frame [{:keys [analyser canvas-context] :as r} {:keys [freq-data] :as config}]
  (render config canvas-context (get-bytes analyser freq-data))
  (set! c/animation-frame-id (.requestAnimationFrame js/window (partial frame r config))))

(defonce config
  {:freq-data (js/Uint8Array. 16384)
   :root (atom {})
   :frame frame
   :width 1024
   :height 256
   :track "/audio/heavy-soul-slinger.mp3"})

(defn on-js-reload []
  (c/teardown (deref (:root config)))
  (c/setup config))
