(ns cassidy.shon
  (:require [hiccup.core :refer [html]])
  (:import [java.io PrintWriter]))

(defprotocol SHONWriter
  (-write-str [object]
    "Print object to as a SHON string."))


(defn- write-string [^CharSequence s]
  (str s))

(defn write-dl [m]
  (html [:dl.shon
         (map #(let [[k v] %]
                 (when-not (string? (name k))
                   (throw (Exception. "SHON dl dt elements must be strings.")))
                 (html [:dt.field (-write-str k)]
                       [:dd {:class (-write-str k)} (-write-str v)]))
              m)]))

(defn write-ol [l]
  (html [:ol.shon
         (map #(html [:li (-write-str %)]) l)]))

(defn write-ul [l]
  (html [:ul.shon
         (map #(html [:li (-write-str %)]) l)]))



(defn- write-bignum [x]
  (str x))

(defn- write-float [^Float x]
  (cond (.isInfinite x)
        (throw (Exception. "SHON error: cannot write infinite Float."))
        (.isNaN x)
        (throw (Exception. "SHON error: cannot write Float NaN."))
        :else
        (str x)))

(defn- write-double [^Double x]
  (cond (.isInfinite x)
        (throw (Exception. "SHON error: cannot write infinite Double."))
        (.isNaN x)
        (throw (Exception. "SHON error: cannot write Double NaN."))
        :else
        (str x)))

(defn- write-plain [x]
  (str x))

(defn- write-null [x]
  (str "null"))

(defn- write-named [x]
  (write-string (name x)))

(defn- write-generic [x]
  (if (.isArray (class x))
    (-write-str (seq x))
    (throw (Exception. (str "Don't know how to write SHON of " (class x))))))

(defn- write-ratio [x]
  (-write-str (double x)))

;; nil, true, false
(extend nil                    SHONWriter {:-write-str write-null})
(extend java.lang.Boolean      SHONWriter {:-write-str write-plain}) ;*

;; Numbers
(extend java.lang.Byte         SHONWriter {:-write-str write-plain}) ;*
(extend java.lang.Short        SHONWriter {:-write-str write-plain}) ;*
(extend java.lang.Integer      SHONWriter {:-write-str write-plain}) ;*
(extend java.lang.Long         SHONWriter {:-write-str write-plain}) ;*
(extend java.lang.Float        SHONWriter {:-write-str write-float}) ;*
(extend java.lang.Double       SHONWriter {:-write-str write-double}) ;*
(extend clojure.lang.Ratio     SHONWriter {:-write-str write-ratio})
(extend java.math.BigInteger   SHONWriter {:-write-str write-bignum}) ;*
(extend java.math.BigDecimal   SHONWriter {:-write-str write-bignum}) ;*
(extend java.util.concurrent.atomic.AtomicInteger SHONWriter {:-write-str write-plain}) ;*
(extend java.util.concurrent.atomic.AtomicLong SHONWriter {:-write-str write-plain})    ;*

;; Symbols, Keywords and Strings
(extend clojure.lang.Named     SHONWriter {:-write-str write-named})
(extend java.lang.CharSequence SHONWriter {:-write-str write-string}) ;*

(extend java.util.Map          SHONWriter {:-write-str write-dl})
(extend java.util.Collection   SHONWriter {:-write-str write-ul})

(extend java.lang.Object       SHONWriter {:-write-str write-generic})

(defn write-str [x]
  (-write-str x))
