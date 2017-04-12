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

(s/def ::audio-ctx (partial constructor? js/AudioContext))
(s/def ::render-ctx (partial constructor? js/CanvasRenderingContext2D))
(s/def ::track-src (partial constructor? js/MediaElementAudioSourceNode))
(s/def ::analyser (partial constructor? js/AnalyserNode))

(s/def ::config
  (s/keys
    :req-un
    [::freq-data ::root ::width ::height ::track]))

(s/def ::contexts
  (s/keys
    :req-un
    [::audio-ctx ::render-ctx ::track-src ::analyser]))

(defn conform [spec data]
  (if (= (s/conform spec data) ::s/invalid)
     (throw (ex-info "Invalid input" (s/explain spec data)))
     data))

(def ids
  {:controls "controls"
   :track "track"
   :play "play"
   :pause "pause"
   :stop "stop"
   :canvas "canvas"})

(defn default-html [width height track]
  [:div
    [:div {:id (:controls ids)}
      [:audio  {:id (:track ids) :src track} ""]
      [:button {:id (:play ids)} "Play"]
      [:button {:id (:pause ids)} "Pause"]
      [:button {:id (:stop ids)} "Stop"]]
    [:canvas
      {:id (:canvas ids)
       :width (str width "px")
       :height (str height "px")}]])

(defn html [{:keys [width height track custom-html] :as config}]
  (hiccups/html
    (if (nil? custom-html)
      (default-html width height track)
      (custom-html config))))

(def ^:dynamic animation-frame-id nil)

(defn qid [id]
  (.querySelector js/document (str \# id)))

(defn insert-dom [html-string]
  (let [app (qid "app")]
    (set! (.-innerHTML app) html-string)
    app))

(defn clear-canvas [ctx width height]
  (set! (.-fillStyle ctx) "rgb(0, 0, 0)")
  (.fillRect ctx 0 0 width height))

(defn clear-frame! []
  (.cancelAnimationFrame js/window animation-frame-id)
  (set! animation-frame-id nil))

(defn play-callback [track-el frame]
  (.play track-el)
  (if (nil? animation-frame-id)
    (frame)))

(defn pause-callback [track-el _]
  (clear-frame!)
  (.pause track-el))

(defn stop-callback [track-el {:keys [root width height]} _]
  (clear-frame!)
  (.pause track-el)
  (set! (.-currentTime track-el) 0)
  (clear-canvas (:render-ctx @root) width height))

(defn create-contexts [nodes]
  (let [audio-ctx (new js/AudioContext)] ; need webkit prefix here
    {:audio-ctx audio-ctx
     :render-ctx (.getContext (:canvas nodes) "2d")
     :track-src (.createMediaElementSource audio-ctx (:track nodes))
     :analyser (.createAnalyser audio-ctx)}))

(defn connect-audio [{:keys [analyser audio-ctx track-src]} fft-size]
  (set! (.-fftSize analyser) fft-size)
  (.connect track-src analyser)
  (.connect track-src (.-destination audio-ctx))
  (.connect analyser (.-destination audio-ctx)))

(defn get-bytes! [analyser freq-data]
  (.getByteFrequencyData analyser freq-data)
  freq-data)

(s/def ::nodes (s/coll-of not-nil?))

(defn get-nodes [ids]
  (let [k (keys ids) v (vals ids)]
    (zipmap k (map qid v))))

(defn schedule! [frame derefed-root config]
  (set! animation-frame-id
    (.requestAnimationFrame
      js/window
      (partial frame derefed-root config))))

(defn frame [derefed-root config]
  ((:render config) derefed-root config)
  (schedule! frame derefed-root config))

(defn bind-events [config nodes]
  (events/listen (:play nodes) "click"
    (partial play-callback (:track nodes)
      (partial frame (deref (:root config)) config)))
  (events/listen (:pause nodes) "click"
    (partial pause-callback (:track nodes)))
  (events/listen (:stop nodes) "click"
    (partial stop-callback (:track nodes) config)))

(defn setup [{:keys [root width height freq-data] :as config}]
  ; (conform ::config config)
  (print "Setup")
  (let [app (insert-dom (html config))
        nodes (get-nodes ids)
        contexts (create-contexts nodes)
        bin-count (.-frequencyBinCount (:analyser contexts))
        sample-rate (.-sampleRate (:audio-ctx contexts))]
      (swap! root merge contexts {:sample-rate sample-rate :bin-count bin-count})
      (connect-audio contexts (.-length freq-data))
      (clear-canvas (:render-ctx contexts) width height)
      (bind-events config nodes)))

(defn teardown [config]
  ; (conform ::context context)
  (let [root (deref (:root config)) nodes (:nodes config)]
    (print "Teardown")
    (clear-frame!)
    (.close (:audio-ctx root))
    (events/removeAll (:play nodes) "click")
    (events/removeAll (:pause nodes) "click")
    (events/removeAll (:stop nodes) "click")
    (insert-dom "<div></div>")))

(defn reset [config]
  (teardown config)
  (setup config))

(defn reload [config]
  (print "Reload")
  (let [root (:root config)
        nodes (get-nodes ids)
        playing? (not-nil? animation-frame-id)
        frame-callback (partial frame @root config)]
    (clear-frame!)
    (events/removeAll (:play nodes) "click")
    (events/listen (:play nodes) "click"
      (partial play-callback (:track nodes) frame-callback))
    (if playing? (frame-callback))))
