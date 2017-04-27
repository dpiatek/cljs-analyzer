(ns cljs-analyzer.example
  (:require [cljs-analyzer.core :as c]))

(defn render [{:keys [analyser render-ctx bin-count] :as root}
              {:keys [byte-data width height] :as config}]
  (c/clear-canvas render-ctx config)
  (let [bytes (c/get-bytes! analyser byte-data :frequency)]
    (set! (.-font render-ctx) "16px serif")
    (loop [i 0]
      (let [val (aget bytes i)]
        (set! (.-fillStyle render-ctx) "rgb(255,255,255)")
        (.fillText render-ctx val (+ 10 (* i 30)) 50)
        (when (< i bin-count)
          (recur (inc i)))))))

(defonce state (atom {}))

(def config
  {:byte-data (js/Uint8Array. 64)
   :render render
   :width (.-innerWidth js/window)
   :height (.-innerHeight js/window)
   :track "/audio/heavy-soul-slinger.mp3"
   :background "rgb(0,0,0)"})

(defn on-js-reload []
  (c/reload config state))
