(ns cljs-analyzer.example
  (:require [cljs-analyzer.core :as c]))

(defn render [{:keys [analyser render-ctx bin-count] :as root} {:keys [freq-data width height]}]
  (c/clear-canvas render-ctx width height)
  (let [bytes (c/get-bytes! analyser freq-data)]
    (set! (.-font render-ctx) "16px serif")
    (loop [i 0]
      (let [val (aget bytes i)]
        (set! (.-fillStyle render-ctx) "rgb(255,255,255)")
        (.fillText render-ctx val (+ 10 (* i 30)) 50)
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

(defn on-js-reload []
  (c/reload config))
