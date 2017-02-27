(ns cljs-analyzer.core
  (:require-macros [hiccups.core :as hiccups])
  (:require [goog.events :as events]
            [hiccups.runtime]))

(enable-console-print!)

(defn html [{w :width h :height src :track}]
  (hiccups/html
    [:div {:style "position: fixed; top: 5px; left: 5px;"}
      [:audio  {:id "track" :src src} ""]
      [:button {:id "play" :style "margin-right: 5px"} "Play"]
      [:button {:id "pause" :style "margin-right: 5px"} "Pause"]
      [:button {:id "stop"} "Stop"]]
    [:canvas
      {:id "canvas"
       :style "display: block"
       :width (str w "px")
       :height (str h "px")}]))

(def ^:dynamic animation-frame-id nil)

(defn qid [id]
  (.querySelector js/document id))

(defn insert-dom [html-string]
  (let [app (qid "#app")]
    (set! (.-innerHTML app) html-string)
    app))

(defn clear-canvas [ctx width height]
  (set! (.-fillStyle ctx) "rgb(0, 0, 0)")
  (.fillRect ctx 0 0 width height))

(defn play-track [track-el frame]
  (.play track-el)
  (frame))

(defn pause-track [track-el _]
  (.cancelAnimationFrame js/window animation-frame-id)
  (.pause track-el))

(defn stop-track [track-el {:keys [root width height]} _]
  (.cancelAnimationFrame js/window animation-frame-id)
  (.pause track-el)
  (set! (.-currentTime track-el) 0)
  (clear-canvas (:canvas-context @root) width height))

(defn create-contexts [track-el canvas-el]
  (let [context (new js/AudioContext)] ; need webkit prefix here
    {:context context
     :canvas-context (.getContext canvas-el "2d")
     :track-src (.createMediaElementSource context track-el)
     :analyser (.createAnalyser context)}))

(defn connect-audio [{:keys [analyser context track-src]} fft-size]
  (set! (.-fftSize analyser) fft-size)
  (.connect track-src analyser)
  (.connect track-src (.-destination context))
  (.connect analyser (.-destination context)))

(defn setup [{:keys [root frame freq-data] :as config}]
  (print "Setup")
  (let [app (insert-dom (html config))
        nodes (map qid ["#play" "#pause" "#stop" "#track" "#canvas"])
        [play-btn pause-btn stop-btn track-el canvas-el] nodes
        contexts (create-contexts track-el canvas-el)]
      (swap! root merge contexts)
      (connect-audio contexts (.-length freq-data))
      (clear-canvas (:canvas-context contexts) (:width config) (:height config))
      (events/listen play-btn "click" (partial play-track track-el (partial frame @root config)))
      (events/listen pause-btn "click" (partial pause-track track-el))
      (events/listen stop-btn "click" (partial stop-track track-el config))
      (print (.-sampleRate (:context @root)))))

(defn teardown [{:keys [context]}]
  (print "Teardown")
  (.cancelAnimationFrame js/window animation-frame-id)
  (.close context)
  (events/removeAll (qid "#play") "click")
  (events/removeAll (qid "#pause") "click")
  (set! (.-innerText (qid "#app")) ""))
