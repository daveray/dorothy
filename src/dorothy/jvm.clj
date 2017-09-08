(ns dorothy.jvm
  (:require [clojure.java.io :as jio]))

(set! *warn-on-reflection* true)

;; ----------------------------------------------------------------------
;; # Graph Rendering
;; Functions responsible for taking a DOT language graph and rendering it
;; to an image.

(defn ^:private build-render-command [{:keys [format layout scale invert-y?]}]
  (->>
    ["dot"
     (if format    (str "-T" (name format)))
     (if layout    (str "-K" (name layout)))
     (if scale     (str "-s" scale))
     (if invert-y? "-y")]
    (remove nil?)))

(defn ^:private ^java.lang.ProcessBuilder init-process-builder
  [{:keys [dir] :as options}]
  (let [pb (java.lang.ProcessBuilder. ^java.util.List (build-render-command options))]
    (when dir (.directory pb (if (instance? java.io.File dir)
                               dir
                               (java.io.File. (str dir)))))
    pb))

(def ^:private binary-formats
  #{:bmp :eps :gif :ico :jpg :jpeg :pdf :png :ps :ps2 :svgz :tif :tiff :vmlz :wbmp})

(defn ^:private read-dot-result [input-stream {:keys [format binary?]}]
  (if (or binary? (binary-formats format))
    (let [result (java.io.ByteArrayOutputStream.)]
      (jio/copy input-stream result)
      (.toByteArray result))
    (slurp input-stream)))

(defn render
  "Render the given graph (must be the string result of (dorothy.core/dot))
  using the Graphviz 'dot' tool. The 'dot' executable must be on the system
  path.

  Depending on the requested format (see options below), returns either a string
  or a Java byte array.

  options is a map with the following options:

    :dir       The working directory in which dot is executed. Defaults to '.'
    :format    The desired output format, e.g. :png, :svg. If the output format
               is known to be binary, a byte array is returned.
    :layout    Dot layout algorithm to use. (-K command-line option)
    :scale     Input scale, defaults to 72.0. (-s command-line option)
    :invert-y? If true, y coordinates in output are inverted. (-y command-line option)

  Examples:

    ; Simple 3 node graph, converted to dot and rendered as SVG.
    (-> (digraph [[:a :b :c]]) dot (render {:format :svg))

  See:

  * (dorothy.core/dot)
  * http://www.graphviz.org/content/command-line-invocation
  * http://www.graphviz.org/content/output-formats
  "
  [graph options]
  (let [p        (.start (init-process-builder options))
        from-dot (future (with-open [from-dot (.getInputStream p)]
                           (read-dot-result from-dot options)))]
    (with-open [to-dot (.getOutputStream p)]
      (spit to-dot graph))
    @from-dot))

(defn save!
  "Render and save the given graph (string result of (dorothy.core/dot)) to an
  output stream. f is any argument acceptable to (clojure.java.io/ouput-stream).

  Examples:

    ; Write a graph to a png file
    (-> (digraph [[:a :b :c]])
        dot
        (save! \"out.png\" {:format :png}))

  See:

  * (dorothy.core/render)
  * (dorothy.core/dot)
  * http://clojure.github.com/clojure/clojure.java.io-api.html#clojure.java.io/make-output-stream
"
  [graph f & [options]]
  (let [bytes (render graph (merge options {:binary? true}))]
    (with-open [output (jio/output-stream f)]
      (jio/copy bytes output)))
  graph)

(defonce ^:private frames (atom {}))

(defn- get-frame [id options]
  ((swap! frames (fn [fs]
                   (if (contains? fs id)
                     fs
                     (let [f (javax.swing.JFrame. "Dorothy")]
                       (.setLocationByPlatform f true)
                       (assoc fs id f)))))
    id))

(defn show!
  "Show the given graph (must be the string result of (dorothy.core/dot)) in a
  new Swing window with scrollbars. Supports same options as
  (dorothy.core/render) except that :format is ignored.

  Examples:

    ; Simple 3 node graph, converted to dot and displayed.
    (-> (digraph [[:a :b :c]]) dot show!)

  Additional options:

  * :frame supply to reuse frames.
  * :frame-width specify maximum frame width.
  * :frame-height specify maximum frame width.

  Notes:

  * Closing the resulting frame will not cause the JVM to exit.

  See:

  * `(dorothy.core/render)`
  * `(dorothy.core/dot)`
  "
  [graph & [options]]
  (let [id (:frame options (gensym))
        ^javax.swing.JFrame frame (get-frame id options)
        shortcut-mask (int (.. java.awt.Toolkit getDefaultToolkit getMenuShortcutKeyMask))
        close-key (javax.swing.KeyStroke/getKeyStroke java.awt.event.KeyEvent/VK_W shortcut-mask)
        ^bytes bytes (render graph (merge options {:format :png}))
        icon  (javax.swing.ImageIcon. bytes)
        max-w (:frame-width options 640)
        max-h (:frame-height options 480)
        w     (.getIconWidth icon)
        h     (.getIconHeight icon)
        lbl   (javax.swing.JLabel. icon)
        sp    (javax.swing.JScrollPane. lbl)]
    (.. sp getInputMap (put close-key "closeWindow"))
    (.. sp getActionMap (put "closeWindow" (proxy [javax.swing.AbstractAction] []
                                             (actionPerformed [e]
                                               (.setVisible frame false)
                                               (.dispose frame)
                                               (swap! frames dissoc id)))))
    (doto frame
      (.setTitle (format "Dorothy %s (%dx%d)" id w h))
      (.setContentPane sp)
      (.setSize (min max-w (+ w 50)) (min max-h (+ h 50)))
      (.setVisible true))
    nil))
