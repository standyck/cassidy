(ns cassidy.shon
  (:require [hiccup.core :refer [html h]]
            [clojure.java.io :as io]
            [saxon :refer [compile-xml compile-xslt]]
            [clojure.pprint :as pp])
  (:import [java.io PrintWriter]))

(def ^{:dynamic true :private true} *key-fn*)
(def ^{:dynamic true :private true} *value-fn*)

(defn- printerr [& x]
  (binding [*out* *err*] (apply println x)))

(defn- default-write-key-fn
  [x]
  (cond (instance? clojure.lang.Named x)
        (name x)
        (nil? x)
        (throw (Exception. "SHON dl keys may not be nil"))
        :else (str x)))

(defn default-value-fn [k v] v)

(defprotocol SHONWriter
  (-write-str [object element]
    "Write object as a SHON string inside an HTML element.")
  (-get-class-attribute [object]
    "The class attribute to assign when writing the object."))

(defn- h-with-class [x el]
  (html [el {:class (-get-class-attribute x)} x]))

(defn- write-null [x el]
  (html [el {:class (-get-class-attribute x)} "null"]))

(defn- write-unescaped-str [x el]
  (h-with-class x el))

(defn- write-escaped-string [x el]
  (h-with-class (h x) el))

(defn- get-classname [x]
  (.getName (class x)))

(defn- write-float [^Float x el]
  (cond (.isInfinite x)
        (throw (Exception. "SHON error: cannot write infinite Float."))
        (.isNaN x)
        (throw (Exception. "SHON error: cannot write Float NaN."))
        :else
        (h-with-class x el)))

(defn- write-double [^Double x el]
  (cond (.isInfinite x)
        (throw (Exception. "SHON error: cannot write infinite Double."))
        (.isNaN x)
        (throw (Exception. "SHON error: cannot write Double NaN."))
        :else
        (h-with-class x el)))

(defn- write-ratio [x el]
  (-write-str (double x) el))

(defn- write-named [x el]
  (-write-str (name x) el))

(defn- write-ol [coll el]
  (html [el
         [:ol {:class (-get-class-attribute coll)}
         (map #(-write-str (*value-fn* nil %) :li) coll)]]))

(defn- write-ul [coll el]
  (html [el
         [:ul {:class (-get-class-attribute coll)}
         (map #(-write-str (*value-fn* nil %) :li) coll)]]))

(defn- write-dl [m el]
  (html [el
         [:dl {:class (-get-class-attribute m)}
         (map #(let [[k v] %
                     out-key (*key-fn* k)
                     out-value (*value-fn* k v)]
                 (when-not (string? (name k))
                   (throw (Exception. "SHON dl dt elements must be strings.")))
                 (html [:dt.field (h out-key)] (-write-str out-value :dd)))
              m)]]))

(defn- write-generic [x el]
  (if (.isArray (class x))
    (-write-str (seq x) el)
    (throw (Exception. (str "Don't know how to write SHON of " (class x))))))

(defn- write-hyperlink [x el]
  (html [el [:a {:href (str x)} (str x)]]))

(def shon-nil (constantly "SHON.null"))
(def shon-number (constantly "SHON.number"))
(def shon-collection (constantly nil))

;; nil, true, false
(extend nil                    SHONWriter
        {:-write-str write-null :-get-class-attribute shon-nil})
(extend java.lang.Boolean      SHONWriter
        {:-write-str write-unescaped-str :-get-class-attribute (constantly "SHON.boolean")})

;; Numbers
(extend java.lang.Byte         SHONWriter
        {:-write-str write-unescaped-str :-get-class-attribute shon-number})
(extend java.lang.Short        SHONWriter
        {:-write-str write-unescaped-str :-get-class-attribute shon-number})
(extend java.lang.Integer      SHONWriter
        {:-write-str write-unescaped-str :-get-class-attribute shon-number})
(extend java.lang.Long         SHONWriter
        {:-write-str write-unescaped-str :-get-class-attribute shon-number})
(extend java.lang.Float        SHONWriter
          {:-write-str write-float :-get-class-attribute shon-number})
(extend java.lang.Double       SHONWriter
          {:-write-str write-double :-get-class-attribute shon-number})
(extend clojure.lang.Ratio     SHONWriter
          {:-write-str write-ratio :-get-class-attribute shon-number})
(extend java.math.BigInteger   SHONWriter
        {:-write-str write-unescaped-str :-get-class-attribute shon-number})
(extend java.math.BigDecimal   SHONWriter
        {:-write-str write-unescaped-str :-get-class-attribute shon-number})
(extend java.util.concurrent.atomic.AtomicInteger SHONWriter
        {:-write-str write-unescaped-str :-get-class-attribute shon-number})
(extend java.util.concurrent.atomic.AtomicLong    SHONWriter
        {:-write-str write-unescaped-str :-get-class-attribute shon-number})

;; Symbols, Keywords and Strings
(extend clojure.lang.Named     SHONWriter
        {:-write-str write-named :-get-class-attribute (constantly nil)})
(extend java.lang.Character    SHONWriter
        {:-write-str write-escaped-string :-get-class-attribute (constantly nil)})
(extend java.lang.CharSequence SHONWriter
        {:-write-str write-escaped-string :-get-class-attribute (constantly nil)})

(extend java.util.Map SHONWriter
        {:-write-str write-dl :-get-class-attribute shon-collection})
(extend java.util.List         SHONWriter
        {:-write-str write-ol
         :-get-class-attribute shon-collection})
(extend java.util.Collection   SHONWriter
        {:-write-str write-ul
         :-get-class-attribute shon-collection})

;; html things
(extend java.net.URL           SHONWriter
        {:-write-str write-hyperlink :-get-class-attribute (constantly nil)})
(extend java.net.URI           SHONWriter
        {:-write-str write-hyperlink :-get-class-attribute (constantly nil)})
(extend java.lang.Object       SHONWriter
        {:-write-str write-generic
         :-get-class-attribute (constantly "SHON")})

(defn- parse-options [options]
  (let [{:keys [key-fn value-fn root-element]
           :or {key-fn default-write-key-fn
                value-fn default-value-fn
                root-element :div}} options]
      [key-fn value-fn root-element]))

(defn write-str
  "Converts x into a SHON string. The value of x can be anything though if it is
  or contains  an unsupported object an exception will be thrown asking for a
  handler function.

  The options available are as follows:

  :key-fn - For Maps: a function (fn [k]) of the key. The returned value will
  be substituted for the input key.
  :value-fn - For Maps, Lists and other Collections: A function (fn [k v]) of the
  key and value. The returned value will be substituted for the input value.
  :root-element - Specifies the root element under which the x value will appear. By
  default it will be a div element and regardless of what it is it will have a class
  attribute of 'shonroot'."
  [x & options]
  (let [[key-fn value-fn el] (parse-options options)]
    (binding [*key-fn* key-fn
              *value-fn* value-fn]
      (-write-str x (keyword (str (name el) ".shonroot"))))))

(defn pprint
  "Pretty prints an object as a SHON string. Options are the same as in
  write-str."
  [x & options]
  (let [[key-fn value-fn el] (parse-options options)
        transform-fn (compile-xslt (io/resource "Identity.xslt"))]
    (if-let [xml (try (compile-xml (write-str x :key-fn key-fn :value-fn value-fn :root-element el))
                      (catch Exception e (printerr (.getMessage e))))]
      (println (str (transform-fn xml)))
      (pp/pprint x))))

(defn wrap-in-page [x & options]
  (let [[key-fn value-fn] (parse-options options)
        shon (write-str x :key-fn key-fn :value-fn value-fn)]
    (html [:html
           [:head
            [:title "SHON Document"]
            [:link {:type "text/css" :rel "stylesheet" :href "shon.css"}]]  ;TODO make this an option.
           [:body shon]])))
