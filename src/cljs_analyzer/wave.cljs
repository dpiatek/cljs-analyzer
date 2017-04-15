(ns cljs-analyzer.wave
  (:require [cljs-analyzer.core :as c]
            [goog.string :as gstring]
            [goog.string.format]
            [clojure.spec :as s]))

(defn render [{:keys [analyser render-ctx bin-count] :as root}
              {:keys [freq-data width height] :as config}]
  (let [bytes (c/get-bytes! analyser freq-data :time)]
    (set! (.-shadowColor render-ctx) "black")
    (set! (.-shadowBlur render-ctx) 0)
    (set! (.-fillStyle render-ctx) "rgba(0,0,0,0.03)")
    (.fillRect render-ctx 0 0 width height)

    (.beginPath render-ctx)
    (set! (.-shadowColor render-ctx) "red")
    (set! (.-shadowBlur render-ctx) 10)
    (.moveTo render-ctx 0 (js/Math.ceil (/ height 2)))
    (loop [i 0]
      (let [val (aget bytes i)
            x (js/Math.ceil (* i (/ width bin-count)))
            y (js/Math.ceil (* val (/ height 255)))]
        (set! (.-strokeStyle render-ctx) (str "rgb(" val "," val "," val ")"))
        (.lineTo render-ctx x y)
        (when (< i bin-count)
          (recur (inc i)))))
    (.stroke render-ctx)))

(defonce state (atom {}))

(def config
  {:freq-data (js/Uint8Array. 128)
   :render render
   :width (.-innerWidth js/window)
   :height (.-innerHeight js/window)
   :track "/audio/heavy-soul-slinger.mp3"
   :background "rgb(0,0,0)"})

(defn on-js-reload []
  (c/reload config state))
