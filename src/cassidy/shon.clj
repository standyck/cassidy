(ns cassidy.shon
  (:require [hiccup.core :refer [html]])
  (:import [java.io PrintWriter]))

(defprotocol SHONWriter
  (-write [object out]
    "Print object to PrintWriter as SHON"))


(defn- write-string [^CharSequence s ^PrintWriter out]
  (.print out (str s)))

(defn write-dl [m ^PrintWriter out]
  (.print
   out
   (html [:dl.shon
          (map #(let [[k v] %]
                  (when-not (string? k)
                    (throw (Exception. "SHON dl dt elements must be strings.")))
                  (html [:dt.field (-write k out)]
                        [:dd {:class (-write k out)} (-write v out)]))
               m)])))

(defn write-ol [m ^PrintWriter out])

(defn write-ul [m ^PrintWriter out])



(defn- write-bignum [x ^PrintWriter out]
  (.print out (str x)))

(defn- write-float [^Float x ^PrintWriter out]
  (cond (.isInfinite x)
        (throw (Exception. "SHON error: cannot write infinite Float."))
        (.isNaN x)
        (throw (Exception. "SHON error: cannot write Float NaN."))
        :else
        (.print out x)))

(defn- write-double [^Double x ^PrintWriter out]
  (cond (.isInfinite x)
        (throw (Exception. "SHON error: cannot write infinite Double."))
        (.isNaN x)
        (throw (Exception. "SHON error: cannot write Double NaN."))
        :else
        (.print out x)))

(defn- write-plain [x ^PrintWriter out]
  (.print out x))

(defn- write-null [x ^PrintWriter out]
  (.print out "null"))

(defn- write-named [x out]
  (write-string (name x) out))

(defn- write-generic [x out]
  (if (.isArray (class x))
    (-write (seq x) out)
    (throw (Exception. (str "Don't know how to write SHON of " (class x))))))

(defn- write-ratio [x out]
  (-write (double x) out))

;; nil, true, false
(extend nil                    SHONWriter {:-write write-null})
(extend java.lang.Boolean      SHONWriter {:-write write-plain})

;; Numbers
(extend java.lang.Byte         SHONWriter {:-write write-plain})
(extend java.lang.Short        SHONWriter {:-write write-plain})
(extend java.lang.Integer      SHONWriter {:-write write-plain})
(extend java.lang.Long         SHONWriter {:-write write-plain})
(extend java.lang.Float        SHONWriter {:-write write-float})
(extend java.lang.Double       SHONWriter {:-write write-double})
(extend clojure.lang.Ratio     SHONWriter {:-write write-ratio})
(extend java.math.BigInteger   SHONWriter {:-write write-bignum})
(extend java.math.BigDecimal   SHONWriter {:-write write-bignum})
(extend java.util.concurrent.atomic.AtomicInteger SHONWriter {:-write write-plain})
(extend java.util.concurrent.atomic.AtomicLong SHONWriter {:-write write-plain})

;; Symbols, Keywords and Strings
(extend clojure.lang.Named     SHONWriter {:-write write-named})
(extend java.lang.CharSequence SHONWriter {:-write write-string})

(extend java.lang.Object       SHONWriter {:-write write-generic})
