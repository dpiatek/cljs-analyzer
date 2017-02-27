(ns cljs-analyzer.wave
  (:require [cljs-analyzer.core :as c]
            [goog.string :as gstring]
            [goog.string.format]))

; API
(defn get-bytes [analyser freq-data]
  (.getByteFrequencyData analyser freq-data)
  [freq-data (.-frequencyBinCount analyser)]) ; bin count does not change!

(defn render [{w :width h :height :as config} ctx [freq-data buffer-length]]
  ; (c/clear-canvas ctx w h)
  (set! (.-font ctx) "14px serif")
  (loop [i 0 x 0]
    (let [data (aget freq-data i)
          bar-height (* (/ h 256) data)
          bar-width (js/Math.ceil (/ w buffer-length))
          color-data #(js/Math.ceil (+ 70 (/ % 3)))
          color-i #(js/Math.ceil (* % (/ %2 buffer-length)))]
      (set! (.-fillStyle ctx) (gstring/format "rgb(%s, %s, %s)" 200 (color-data data) (color-data data)))
      (.fillRect ctx x (- h bar-height) bar-width bar-height)
      (set! (.-fillStyle ctx) "rgb(200,200,200)")
      (.fillText ctx (str (.toFixed (/ (js/Math.ceil (* i (/ 44100 64))) 1000) 1) "k") (+ x 10) (- h 20))
      (when (or (< i buffer-length))
        (recur (inc i) (+ x bar-width))))))

(defn frame [{:keys [analyser canvas-context] :as r} {:keys [freq-data] :as config}]
  (render config canvas-context (get-bytes analyser freq-data))
  (set! c/animation-frame-id (.requestAnimationFrame js/window (partial frame r config))))

(def config
  {:freq-data (js/Uint8Array. 32)
   :root (atom {})
   :frame frame
   :width (.-innerWidth js/window)
   :height (.-innerHeight js/window)
   :track "/audio/heavy-soul-slinger.mp3"})

(defn reset [c]
  (c/teardown (deref (:root c)))
  (c/setup c))

(defn on-js-reload [])
  ; (reset config))
