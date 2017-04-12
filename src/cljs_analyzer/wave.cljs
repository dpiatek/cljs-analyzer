(ns cljs-analyzer.wave
  (:require [cljs-analyzer.core :as c]
            [goog.string :as gstring]
            [goog.string.format]
            [clojure.spec :as s]))
;
; (defn freq-for-bin [bin]
;   (str (.toFixed (/ (js/Math.ceil (* (inc bin) (/ 44100 64))) 1000) 1)))
;
; (defn render [{w :width h :height :as config} ctx freq-data buffer-length]
;   (c/clear-canvas ctx w h)
;   (set! (.-font ctx) "12px serif")
;   (loop [i 0 x 0]
;     (let [data (aget freq-data i)
;           bar-height (* (/ h 256) data)
;           bar-width (js/Math.ceil (/ w buffer-length))
;           color-data #(js/Math.ceil (+ 70 (/ % 3)))
;           color-i #(js/Math.ceil (* % (/ %2 buffer-length)))]
;       (set! (.-fillStyle ctx) (gstring/format "rgb(%s, %s, %s)" 10 (color-data data) (color-data data)))
;       (.fillRect ctx x (- h bar-height) bar-width bar-height)
;       (set! (.-fillStyle ctx) "rgb(200,200,200)")
;       (.fillText ctx (freq-for-bin i) (+ x (/ bar-width 4)) (- h 20))
;       (when (or (< i buffer-length))
;         (recur (inc i) (+ x bar-width))))))
;
; (def config
;   {:freq-data (js/Uint8Array. 64)
;    :root (atom {})
;    :render render
;    :width (.-innerWidth js/window)
;    :height (.-innerHeight js/window)
;    :track "/audio/heavy-soul-slinger.mp3"})
;
; (defn setup [c]
;   (c/setup c))
;
; (defn reset [c]
;   (c/reset c))
;
; (defn on-js-reload [])
  ; (reset config))
