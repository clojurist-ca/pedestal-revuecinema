(ns ca.clojurist.revuecinema
  {:author "Robert Medeiros" :email "robert@clojurist.ca"}
  (:import
   [java.time LocalDate LocalTime]
   [java.time.format DateTimeFormatter])
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as string])
  (:require
   [net.cgrand.enlive-html :as html]
   [org.httpkit.client :as http])
  (:gen-class))

;; TODO output as JSON
;; TODO output as edn
;; TODO output as transit

(def base-url "http://revuecinema.ca/movies/what%E2%80%99s-playing")

(defn fetch-url
  [url]
  (html/html-resource (java.net.URL. url)))

(defn count-substring
  [s pattern]
  (count (re-seq (re-pattern pattern) s)))

(defn date-or-nil
  [[header content :as show]]
  ;; (html/select) returns a possibly empty sequence, use seq to obtain
  ;; nil if empty and then (first) to extract the XML node map contained
  ;; by the sequence.
  (if-let [d (-> (html/select header [:h3]) seq first)]
    (let [dt-formatter (DateTimeFormatter/ofPattern "EEEE, MMMM d uuuu")
          date-str (html/text d)
          year (.. (LocalDate/now) getYear)
          date-str+year (string/join " " [date-str year])
          local-date (LocalDate/parse date-str+year dt-formatter)]
      ;; Should we bother converting LocalDate into java.util.Date?
      local-date)))

(defn dates
  [show-seq]
  (let [;; Generates sequence of date strings mixed with nils.
        dates-or-nils (map date-or-nil show-seq)
        ;; Reducing fn takes accumulator and date string/nil, and
        ;; returns the date string if not-nil, otherwise returns the
        ;; last date that was not nil.
        last-date-if-nil-fn
        (fn [acc d]
          (if d
            (conj acc d)
            (conj acc (peek acc))))
        ;; Generate list of dates, with nils replaces by last available date.
        all-dates (reduce last-date-if-nil-fn [] dates-or-nils)
        ;; Turn dates into maps having key :date and the date as value.
        date-maps (map hash-map (repeatedly (constantly :date)) all-dates)]
    date-maps))

(defn shows
  [r]
  (partition 2 (interleave
                (html/select r [:div.accordionHeader])
                (html/select r [:div.accordionContent]))))

(defn show-rating
  [[header content]]
  (let [rating-seq (map html/text (html/select header [:span.rating]))
        rating (first rating-seq)]
    (if-not (string/blank? rating)
      {:rating (string/replace rating #"[\(\)]" "")})))

(def labels #{"Foreign Feature Film"
              "Parent & Baby Screening"
              "Canadian Indie Film Series"
              "Drunk Feminist Films Presents"
              "Docs in Revue"
              "Revue Recommends"
              "Classics Revue"
              "Cereal Classics"
              "Hot Docs Film Festival"
              "Art for Eternity Presents"})

;; show-title
;; -----------------------------------------------------------------------------
;; This multimethod uses a dispatch function that checks:
;; - if the title element has a sub-span (which is typically the label)
;; - if the title element has more than one colon, in which case the
;;   piece before the first colon is typically the label
;; - if the part of the title element before the first colon matches a
;;   known label string e.g. "Revue Recommends", "Classics Revue", etc.
;; - otherwise we use the title string as-is
;;
;; Even better would be to contact the creators of the page and ask them
;; to consistently use a <span class="label"> around the label text.

(defn get-title-parts
  [[header content :as show]]
  (if-let [raw-title (first (map html/text (html/select header [:span.title])))]
      (let [title (string/trim raw-title)
            parts (map string/trim (string/split title #":"))]
        parts)))

(defmulti show-title
  (fn [[header content :as show]]
    (count (get-title-parts show))))

;; When there are three parts to the title (separated by colons) the
;; first part is almost certainly a Revue-provided label, while the
;; remaining parts are the title.
(defmethod show-title 3
  [[header content :as show]]
  (let [[label & title-parts] (get-title-parts show)
        title (string/join ": " title-parts)]
    {:label label :title title}))

;; When there are two parts to the title that are separated by a colon,
;; it's possible that the title has a colon within it, or that the colon
;; is being introduced to separate a label from the title, e.g.
;;
;;   Some Movie: The Returning
;; vs.
;;   Revue Recommends: Some Movie
;;
;; Ideally there would always be markup to separate the label from the
;; title, and sometimes there is, but not always.
(defmethod show-title 2
  [[header content :as show]]
  (let [[possible-label title :as parts] (get-title-parts show)]
    (if (contains? labels possible-label)
      {:label possible-label :title title}
      {:title (string/join ": " parts)})))

;; If there's only a single part to the title we just use it as the
;; title without specifying a label.
(defmethod show-title 1
  [[header content :as show]]
  {:title (first (get-title-parts show))})

(defmethod show-title :default
  [[header content :as show]]
  nil)

;; show-time
;; -----------------------------------------------------------------------------

(defn show-time
  [[header content]]
  (if-let [raw-time (first (map (comp string/trim html/text) (html/select header [:span.time])))]
    (let [dt-formatter (DateTimeFormatter/ofPattern "h:m a")
          time (LocalTime/parse raw-time dt-formatter)]
      {:time time})))

;; show-duration
;; -----------------------------------------------------------------------------

(defn show-duration
  [[header content]]
  (let [raw-duration (first (map html/text (html/select header [:span.runningTime])))
        ;; NB: the duration isn't always present so checking is required.
        [_ duration] (when raw-duration (re-matches #"(\d+) min" raw-duration))]
    ;; TODO turn duration into seconds or minutes
    (if-not (string/blank? duration)
      {:duration (Integer/parseUnsignedInt duration)})))

;; show-year
;; -----------------------------------------------------------------------------

(defn show-year
  [[header content]]
  (let [raw-year (first (map html/text (html/select content [:span.year])))
        year (when-not (string/blank? raw-year) (Integer/parseUnsignedInt raw-year))]
    {:year year}))

;; show-director
;; -----------------------------------------------------------------------------

(defn show-director
  [[header content]]
  (if-let [raw-director (first (map html/text (html/select content [:span.director])))]
    (let [director (-> raw-director
                       ;; Strip parentheses from beginning and end of string.
                       (string/replace #"^\(|\)$" "")
                       ;; Sometimes there is more than one director, separated by a comma.
                       (string/split #","))
          director (mapv string/trim director)]
      {:director director})))

;; show-cast
;; -----------------------------------------------------------------------------

(defn show-cast
  [[header content]]
  ;; NB the cast listing isn't always present
  (if-let [raw-cast (first (map html/text (html/select content [:span.cast])))]
    (let [;; Trim off initial "With:" string
          raw-cast (string/replace raw-cast #"^With:\s+" "")
          ;; Return a vector of cast member names
          cast (filterv (complement string/blank?)
                        (string/split raw-cast #"[,;]\s+"))]
      {:cast cast})))

;; show-poster
;; -----------------------------------------------------------------------------

(defn show-poster
  [[header content]]
  (let [raw-image (first (html/select content [:img.poster]))
        src-attr (get-in raw-image [:attrs :src])
        alt-attr (get-in raw-image [:attrs :alt])]
    {:poster-alt alt-attr :poster-src src-attr}))

;; show-review
;; -----------------------------------------------------------------------------

(defn show-review
  [[header content]]
  (if-let [raw-review (map html/text (html/select content [:p]))]
    (let [;; Remove non-breaking spaces
          clean-review (map #(string/replace % #" " "") raw-review)
          ;; Filter out empty strings and place into vector
          review (filterv (comp not string/blank?) clean-review)]
      {:review review})))

;; show-author
;; -----------------------------------------------------------------------------

(defn show-author
  [[header content]]
  (if-let [raw-author (first (map html/text (html/select content [:span.author])))]
    (let [;; Strip out "by" prefix
          author (-> raw-author
                     (string/replace #"^by\s+" "")
                     (string/trim))]
      {:author author})))

;; show-trailer-link
;; -----------------------------------------------------------------------------

(defn show-trailer-link
  [[header content]]
  (if-let [raw-link (first (html/select content [:a.trailerLink]))]
    (let [link (get-in raw-link [:attrs :href])]
      {:trailer-link link})))

;; show-language
;; -----------------------------------------------------------------------------

(defn show-language
  [[header content]]
  (if-let [language-seq (map html/text (html/select header [:span.language]))]
    (let [language-raw (first language-seq)
          language (when language-raw (string/replace language-raw #"\s+-\s+" ""))]
      (if-not (string/blank? language)
        {:language language}))))

;; pair->map
;; -----------------------------------------------------------------------------
;; Given a pair consisting of the two parts of a show listing on the
;; page from which it was scraped, return a map containing the listing details
;; that are extracted.
;;
;; The header entry contains the metadata about the show listing, while
;; the content item contains additional detail.

(defn pair->map
  [[header content :as pair]]
  (merge
   (show-title pair)
   (show-time pair)
   (show-duration pair)
   (show-year pair)
   (show-director pair)
   (show-cast pair)
   (show-poster pair)
   (show-review pair)
   (show-trailer-link pair)
   (show-author pair)
   (show-language pair)))

(defn assoc-date-time
  [{:keys [date time] :as show}]
  (if (and (instance? java.time.LocalDate date)
           (instance? java.time.LocalTime time))
    (assoc show :date-time (.atTime date time))
    show))

(defn convert
  [url]
  (let [resp (fetch-url url)
        show-seq (shows resp)
        date-seq (dates show-seq)
        show-maps (map merge (map pair->map show-seq) date-seq)]
    (map assoc-date-time show-maps)))

(defn -main
  [& args]
  (pprint (convert base-url)))
