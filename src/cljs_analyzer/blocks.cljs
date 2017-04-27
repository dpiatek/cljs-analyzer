(ns cljs-analyzer.blocks
  (:require [cljs-analyzer.core :as c]
            [goog.string :as gstring]
            [goog.string.format]
            [clojure.spec :as s]))

(defn render [{:keys [analyser render-ctx bin-count] :as root}
              {:keys [byte-data width height] :as config}]
  (let [bytes (c/get-bytes! analyser byte-data)
        bin-width (js/Math.ceil (- (/ width bin-count) 8))
        max-squares (js/Math.ceil (/ height (+ bin-width 8)))
        multi (/ max-squares 255)]
    (c/clear-canvas render-ctx config)
    (loop [i 0]
      (let [val (aget bytes i)
            squares-per-bar (js/Math.ceil (* multi val))]
        (loop [j 0]
          (if (< j squares-per-bar)
            (set! (.-fillStyle render-ctx) (str "rgb(255,0,0)"))
            (set! (.-fillStyle render-ctx) (str "rgb(255,255,255)")))
          (.fillRect render-ctx
            (+ 30 (* i (+ 5 bin-width)))
            (- (- height 40) (* j (+ 5 bin-width)))
            bin-width
            bin-width)
          (when (< j max-squares)
            (recur (inc j)))))
      (when (< i bin-count)
        (recur (inc i))))))

(defonce state (atom {}))

(def config
  {:byte-data (js/Uint8Array. 128)
   :render render
   :width (.-innerWidth js/window)
   :height (.-innerHeight js/window)
   :track "/audio/heavy-soul-slinger.mp3"
   :background "rgb(0,0,0)"})

(defn on-js-reload []
  (c/reload config state))
