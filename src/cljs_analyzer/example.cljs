(ns cljs-analyzer.example
  (:require [cljs-analyzer.core :as c]))

(defn render [{:keys [analyser canvas-context]} {:keys [freq-data width height]} bin-count]
  (c/clear-canvas canvas-context width height)
  (let [bytes (c/get-bytes! analyser freq-data)]
    (set! (.-font canvas-context) "16px serif")
    (loop [i 0]
      (let [val (aget bytes i)]
        (set! (.-fillStyle canvas-context) "rgb(255,255,255)")
        (.fillText canvas-context val (+ 5 (* i 30)) 50)
        (when (or (< i bin-count))
          (recur (inc i)))))))

(defonce root (atom {}))

(def config
  {:freq-data (js/Uint8Array. 64)
   :root root
   :render render
   :width (.-innerWidth js/window)
   :height (.-innerHeight js/window)
   :track "/audio/heavy-soul-slinger.mp3"})

(defn setup [c]
  (c/setup c))

(defn reset [c]
  (c/reset c))

(defn on-js-reload []
  (c/reload config))
