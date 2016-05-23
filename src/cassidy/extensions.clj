(ns cassidy.extensions
  "Extra SHONWriter implementations that may be of use."
  (:require [cassidy.shon :refer [-get-class-attribute SHONWriter
                                  write-str]]
            [hiccup.core :refer [html]]))

(defrecord Table [recs caption column-order]
  SHONWriter
  (-get-class-attribute [_] "SHON.Table")
  (-write-str [table el]
    (let [{:keys [recs caption column-order]} table
          headers (if column-order column-order (keys (first recs)))
          row-data (map #(map (fn [k] (get % k)) headers) recs)]
      (html [el {:class (-get-class-attribute table)}
             [:table
              [:caption caption]
              [:tr (map #(html [:th %]) headers)]
              (map #(html [:tr
                           (map (fn [v] (html (write-str v :root-element :td :root? false))) %)])
                   row-data)]]))))

(def tab (->Table [{:a 1 :b 2} {:a 10 :b 20}] "Simple Table" nil))

(defrecord Link [href text rel]
  SHONWriter
  (-get-class-attribute [_] "SHON.Link")
  (-write-str [link el]
    (html [el
           [:a {:class (-get-class-attribute link)
                :rel  (:rel link)
                :href (:href link)}
            (if (:text link) (:text link) (:href link))]])))
