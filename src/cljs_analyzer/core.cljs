(ns ^:figwheel-always cljs-analyzer.core
  (:require-macros [hiccups.core :as hiccups])
  (:require [goog.events :as events]
            [hiccups.runtime]
            [clojure.spec :as s]))

(enable-console-print!)

(def not-nil? (complement nil?))

(defn constructor? [cstr inst]
  (if (not-nil? inst)
    (= (.. cstr -prototype -constructor) (.. inst -constructor))
    false))

(defn atom-map? [a]
  (map? @(atom {})))

(s/def ::freq-data (partial constructor? js/Uint8Array))
(s/def ::root atom-map?)
(s/def ::canvas-width number?)
(s/def ::canvas-height number?)
(s/def ::track string?)

(s/def ::context (partial constructor? js/AudioContext))
(s/def ::canvas-context (partial constructor? js/CanvasRenderingContext2D))
(s/def ::track-src (partial constructor? js/MediaElementAudioSourceNode))
(s/def ::analyser (partial constructor? js/AnalyserNode))

(s/def ::config
  (s/keys
    :req-un
    [::freq-data ::root ::width ::height ::track]))

(s/def ::contexts
  (s/keys
    :req-un
    [::context ::canvas-context ::track-src ::analyser]))

(defn conform [spec data]
  (if (= (s/conform spec data) ::s/invalid)
     (throw (ex-info "Invalid input" (s/explain spec data)))
     data))

(defn html [{:keys [width height track] :as config}]
  (hiccups/html
    [:div
      [:audio  {:id "track" :src track} ""]
      [:button {:id "play"} "Play"]
      [:button {:id "pause"} "Pause"]
      [:button {:id "stop"} "Stop"]]
    [:canvas
      {:id "canvas"
       :width (str width "px")
       :height (str height "px")}]))

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

(defn clear-frame! []
  (.cancelAnimationFrame js/window animation-frame-id)
  (set! animation-frame-id nil))

(defn play-track [track-el frame]
  (.play track-el)
  (if (nil? animation-frame-id)
    (frame)))

(defn pause-track [track-el _]
  (clear-frame!)
  (.pause track-el))

(defn stop-track [track-el {:keys [root width height]} _]
  (clear-frame!)
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

(defn get-bytes! [analyser freq-data]
  (.getByteFrequencyData analyser freq-data)
  freq-data)

(s/def ::nodes (s/coll-of not-nil?))

(defn schedule [frame root config bin-count]
  (set! animation-frame-id
    (.requestAnimationFrame
      js/window
      (partial frame root config bin-count))))

(defn frame [render root config bin-count]
  (render @root config bin-count)
  (schedule (partial frame render) root config bin-count))

(defn setup [{:keys [root render freq-data] :as config}]
  ; (conform ::config config)
  (print "Setup")
  (let [app (insert-dom (html config))
        nodes (conform ::nodes
                (map qid ["#play" "#pause" "#stop" "#track" "#canvas"]))
        [play-btn pause-btn stop-btn track-el canvas-el] nodes
        contexts (create-contexts track-el canvas-el)]
      (swap! root merge contexts)
      (connect-audio contexts (.-length freq-data))
      (clear-canvas (:canvas-context contexts) (:width config) (:height config))
      (events/listen play-btn "click"
        (partial play-track track-el
          (partial frame render root config (.-frequencyBinCount (:analyser contexts)))))
      (events/listen pause-btn "click" (partial pause-track track-el))
      (events/listen stop-btn "click" (partial stop-track track-el config))
      (print (.-sampleRate (:context @root)))))

(defn teardown [{:keys [context]}]
  ; (conform ::context context)
  (print "Teardown")
  (clear-frame!)
  (.close context)
  (events/removeAll (qid "#play") "click")
  (events/removeAll (qid "#pause") "click")
  (insert-dom "<div></div>"))

(defn reload [config]
  (print "Reload")
  (let [root (:root config)
        nodes (map qid ["#play" "#pause" "#stop" "#track" "#canvas"])
        [play-btn pause-btn stop-btn track-el canvas-el] nodes
        playing? (not-nil? animation-frame-id)
        frame-applied (partial frame (:render config) root config (.-frequencyBinCount (:analyser @root)))]
    (clear-frame!)
    (events/removeAll (qid "#play") "click")
    (events/listen play-btn "click"
      (partial play-track track-el frame-applied))
    (if playing? (frame-applied))))

(defn reset [config]
  (teardown (deref (:root config)))
  (setup config))
