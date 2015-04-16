(ns cassidy.core-test
  (:require [clojure.test :refer :all]
            [cassidy.shon :as shon]
            [hiccup.core :refer [html]]
            [clojure.pprint :refer [pprint]])
  (:import [java.util.concurrent.atomic AtomicLong AtomicInteger]
           [java.net URL URI]))

(def multi-map {:nil            nil
                :boolean        true
                :byte           (Byte. "1")
                :short          (Short. "2")
                :integer        3
                :long           (Long. 4)
                :float          (Float. 5.55)
                :double         6.66
                :big-integer    (BigInteger. "7")
                :big-decimal    (BigDecimal. "8.8888")
                :atomic-integer (AtomicInteger. 9)
                :atomic-long    (AtomicLong. 10.101010)
                :keyword        :keyword
                :string         "string"
                :character      \c
                :url            (URL. "http://www.standyck.com")
                :uri            (URI. "http://www.standyck.com")
                :vector         [1 2 3]
                :map            {:a 1 :b 2 :c 3}
                :ratio          (/ 22 7)
                :array          (to-array ["a" "b" "c"])
                :with-escapes   "'This' & \"That\""
                :amp-char       \&})



(deftest write-string-list
  (let [multi-list (vals multi-map)]
    (testing "Writing a list of stuff"
      (println "Multi-list:")
      (pprint multi-list)
      (println "As SHON:")
      (shon/pprint multi-list))))

(deftest write-string-map
  (testing "Write a map of stuff"
    (println "Multi-map")
    (pprint multi-map)
    (println "As SHON:")
    (shon/pprint multi-map)))


;;; Let's define our own record
;;; and specialize the SHON output by extending the SHONWriter protocol
(defrecord BPReading [sys dia]
  shon/SHONWriter
  (-get-class-attribute [_] "stand.BPReading")
  (-write-str [bp el]
    (html [el {:class (shon/-get-class-attribute bp)}
           [:ul.bp
            [:li.sys (str "Systolic: " (:sys bp))]
            [:li.dia (str "Diastolic: " (:dia bp))]]])))


;;; Now let's use the same technique to  take advantage of existing html semantics.
(defrecord Image [src alt-text]
  shon/SHONWriter
  (-get-class-attribute [_] "stand.Img")
  (-write-str [img el]
    (html [el {:class (shon/-get-class-attribute img)}
           [:p [:label "Source URL: "] [:code (:src img)]]
           [:img {:src (:src img) :alt (:alt-text img) :title (:alt-text img)}]])))

;;; We can even extend an existing java object with a custom representation. Let's use UUID as an example.
(extend-protocol shon/SHONWriter
  java.util.UUID
  (-get-class-attribute [_] "stand.UUID")
  (-write-str [uuid el]
    (html [el {:class (shon/-get-class-attribute uuid)}
           [:label "UUID: "]
           [:span (str uuid)]])))

;;; Let's do a custom format that is useful. Try an hCard address microformat
(defrecord Adr [street-address locality region postal-code country-name]
  shon/SHONWriter
  (-get-class-attribute [_] "adr")
  (-write-str [adr el]
    (html [el {:class (shon/-get-class-attribute adr)}
           [:div.street-address (:street-address adr)]
           [:span.locality (:locality adr) ", "]
           [:span.region (:region adr) ", "]
           [:span.postal-code (:postal-code adr) " "]
           [:span.country-name (:country-name adr)]])))

(defrecord HCard [fn org email adr tel]
  shon/SHONWriter
  (-get-class-attribute [_] "shon.vcard")
  (-write-str [hcard el]
    (html [el {:class (shon/-get-class-attribute hcard)}
           [:div.vcard
            [:span.fn (:fn hcard)]
            [:div.org (:org hcard)]
            [:a.email {:href (str "mailto:" (:email hcard))} (:email hcard)]
            (shon/write-str adr)
            [:div.tel (:tel hcard)]]])))

(def stan (->HCard "Stan Dyck"
                   "Quest Diagnostics"
                   "stan.dyck@medplus.com"
                   (->Adr "8801 6th Av NW"
                          "Seattle" "WA" "98117" "USA")
                   "206-419-8059"))

;;; A macro that turned this (below) into the extend-protocol form above would be useful.
#_(defshon-representation UUID [uuid]
    (html [:span (str uuid)]))
;;; Here is a similar form for the Image record
#_(defshon-representation Img [{:keys [src alt-text]} img]
    (html [:div.image
           [:p [:label "Source URL: "] [:code src]
            [:img {:src src} :alt alt-text :title alt-text]]]))

(deftest write-test-file
  (testing "create a test file"
    (spit (java.io.File. "resources/testshon.html")
          (shon/wrap-in-page [multi-map (vals multi-map)]))))

(deftest custom-write
  (testing "We can define our own output."
    (let [bp (BPReading. 125 70)
          img (Image. "http://placekitten.com/g/300/450" "A Cute Kitten")
          uuid (java.util.UUID/randomUUID)
          m {:bp-reading bp :image img :uuid uuid
             :hcard stan}]
      (println "bp:" bp)
      (println "img:" img)
      (shon/pprint m)
      ;; Take a look
      (spit (java.io.File. "resources/testcustomshon.html") (shon/wrap-in-page m)))))
