(ns cassidy.xmlrunoff
  (:require [saxon :as saxon]))


(def saxon-testfile (saxon/compile-xml (java.io.File. "resources/testshon.html")))

(def top-level (saxon/query "//body//*[@class='shonroot']" saxon-testfile))

(defmacro defsaxon-fn
  "Creates a named function that takes an XdmNode and returns the value
  of xpath on that node with nil handling."
  [fn-name xpath]
  (let [query-with-nil-handling
        (fn [xp node]
          (if (instance? net.sf.saxon.s9api.XdmNode node)
            (saxon/query xp node)))]
    `(def ~fn-name (partial ~query-with-nil-handling ~xpath))))

(defsaxon-fn context-node-name "name()")
(defsaxon-fn shon-node?        "boolean(./@class[.='shonroot'])")

(defn shon-type [^net.sf.saxon.s9api.XdmNode shon-node]
  (let [node-name (context-node-name shon-node)]
    (if (seq node-name)
      (keyword node-name))))
