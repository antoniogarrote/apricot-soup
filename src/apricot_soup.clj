(ns apricot-soup
  (:use [clojure.java.io]
        [clojure.string :only [join]])
  (:import [org.jsoup Jsoup]
           [org.jsoup.select Elements]
           [org.jsoup.nodes Element Document]))

(defonce *docs-queue* (atom []))

(defn enqueue-doc
  "Adds a document to the docs queue"
  ([doc]
     (swap! *docs-queue* conj doc)))

(defn last-doc
  "Returns the last queued doc"
  ([] (last @*docs-queue*)))

(defn dequeue-doc
  "Removes the last queued doc"
  ([] (when-not (empty? @*docs-queue*)
        (let [last-val (last-doc)]
          (swap! *docs-queue* pop)
          last-val))))

(defn- from-url
  "Retrieves HTML document form a URL"
  ([uri]
     (with-open [data (reader uri)]
       (apply str (line-seq data)))))

(defn- is-uri
  "Checks if the provided string is a URI"
  ([uri-str]
     (or (= 0 (.indexOf uri-str "http://"))
         (= 0 (.indexOf uri-str "https://")))))


(defn parse
  "Parses a new HTML document from a string"
  ([html-string]
     (if (is-uri html-string)
       (let  [content (from-url html-string)
              doc (Jsoup/parse content)]
         (do (.setBaseUri doc html-string)
             doc))
      (Jsoup/parse html-string))))

(defn to-elems
  "Wrap a list of elements into a Elements object"
  ([elems]
     (if (instance? Document elems) elems
         (if (instance? Elements elems) elems
             (if (instance? Element elems)
               (Elements. (list elems))
               (Elements. elems))))))

(defmacro $
  ([uri-string & body]
     (let [string-to-search-elems# (map #(if (string? %1) `(search ~%1)
                                             (if (symbol? %1)
                                               `(search ~(str %1))
                                               (if (keyword? %1)
                                                 `(search ~(str "#"(name %1)))
                                                 %1))) body)]
       `(do (enqueue-doc (parse ~uri-string))
            (let [to-return# (->> (last-doc)
                                  (identity)
                                  ~@string-to-search-elems#)]
              (dequeue-doc)
              to-return#)))))

(defn append-elems
  ([elemsa elemsb]
     (Elements. (concat elemsa elemsb))))

(defmacro add
  ([& body-args]
     (let [body# (butlast body-args)
           node# (last body-args)
           string-to-search-elems# (map #(if (string? %1) `(search ~%1)
                                             (if (symbol? %1)
                                               `(search ~(str %1))
                                               (if (keyword? %1)
                                                 `(search ~(str "#"(name %1)))
                                                 %1))) body#)]
       `(if (try (instance? Elements ~(first string-to-search-elems#)) (catch Exception ex# false))
          (append-elems ~node# ~(first string-to-search-elems#))
          (let [new-elems# (->> (last-doc) ~@string-to-search-elems#)]
            (append-elems ~node# new-elems#))))))

(defmacro combine
  ([doc-or-elems & body]
     `(->> ~doc-or-elems ~@body)))

(defn search
  "Searches in the document"
  ([expr node]
     (.select (to-elems node) expr)))

(defn id
  "Extracts the ID of an element"
  ([node]
     (if (instance? Elements node)
       (vec (map #(.id %1) node))
       (.id node))))

(defn base-uri
  "Returns the URI of the current document"
  ([node] (.baseUri (last-doc)))
  ([uri node] (do (.setBaseUri (last-doc) uri) node)))

(defn get-all-elements
  "Extracts all the elements of the wrapped node"
  ([& args]
     (let [node (last args)
           params (butlast args)]
       (if (instance? Elements node)
         (Elements. (flatten (map (fn [elem] (vec`(.getAllElements ~elem ~@params))) node)))
         (Elements. (list `(.getAllElements ~node ~@params)))))))

(defn attr
  "Get the value of an attribute for the first element in the set of matched elements or set the value of all elements when invoked with two parameters"
  ([attr-name node]
     (.attr (to-elems node) attr-name))
  ([attr-name attr-value node]
     (.attr (to-elems node) attr-name attr-value)))

(defn has-class
  "Determine whether any of the matched elements are assigned the given class"
  ([class-name node]
     (.hasClass (to-elems node) class-name)))

(defn html
  "Get the HTML contents of the first element in the set of matched elements. When one argument is passed, sets the inner HTML of the element"
  ([node]
     (if (instance? Elements (to-elems node))
       (if (> (count node) 0)
         (.outerHtml (first (to-elems node))) "")
       (.outerHtml node)))
  ([value node]
     (.html (to-elems node) value)))

(defn remove-attr
  "Remove an attribute from each element in the set of matched elements"
  ([attr-name node]
     (.removeAttr (to-elems node) attr-name)))

(defn remove-class
  "Remove a class from each element in the set of matched elements"
  ([class-name node]
     (.removeClass (to-elems node) class-name)))

(defn value
  "Get the form element's value of the first matched elemen"
  ([node]
     (.val node))
  ([value node]
     (.val node value)))


(defn text
  "Get the combined text contents of each element in the set of matched elements, including their descendants"
  ([node]
     (if (instance? Element node)
       (.text node)
       (reduce #(str %1 " " %2)  "" (map #(.text %1) (to-elems node))))))

(defn children
  "Get the children of each element in the set of matched elements"
  ([node]
     (if (instance? Element node)
       (.children node)
       (let [elems (flatten (map  #(vec (.children %1)) node))]
         (Elements. elems)))))


(defn parent
  "Get the parents of each element in the set of matched elements"
  ([node]
     (if (instance? Element node)
       (.parents node)
       (let [elems (flatten (map  #(vec (.parents %1)) node))]
         (Elements. elems)))))

(defn attr-map
  "Builds a mapping with the link URI and link text from a certaing wrapped set"
  ([node] (combine node  (map (fn [e] attr "href" e)) (reduce (fn [ac e] (assoc ac (attr "href" e) (text e))) {}))))

(defn filter-relative-uris
  "Removes all the links that holding relative uris"
  ([node]
     (let [elems (to-elems node)]
       (to-elems (filter (fn [e] (let [href (attr "href" e)]
                         (.startsWith href "http")))
                         elems)))))

(defn external-links
  "Filter all the links to the same domain passed as an argument"
  ([base-uri node]
     (let [filtered (filter (fn [e] (let [href (attr "href" e)]
                                      (not (.startsWith href base-uri))))
                            node)]
       (to-elems filtered)))
  ([node]
     (let [filtered (filter (fn [e] (let [href (attr "href" e)]
                                      (not (.startsWith href (base-uri node)))))
                            node)]
       (to-elems filtered))))

(defn tagname
  "Returns the tagnanme of an element"
  ([node]
     (if (instance? Elements node)
       (map (fn [elem] (.tagName elem)) node)
       (.tagName node))))

(defn class-names
  "Returns a set with all the clases for an element"
  ([node]
     (if (instance? Elements node)
       (map (fn [elem] (class-names elem)) node)
       (set (filter #(not= "" %1) (.classNames node))))))

(defn attributes
  "Returns a map with the attributes of a node"
  ([node]
     (if (instance? Elements node)
       (map (fn [elem] (attributes elem)) node)
       (let [attributes (.asList (.attributes node))]
         (reduce (fn [m attr]
                   (let [key (keyword (.getKey attr))
                         val (.getValue attr)]
                     (assoc m key val)))
                 {} attributes)))))

(defn s-expressions
  "transforms the wrapped elements into a list of s-expressions"
  ([node]
     (if (instance? Elements node)
       (map s-expressions node)
       (let [tag-name (tagname node)
             tag-id (id node)
             classes (vec (class-names node))
             children (.children node)
             check-id (fn [tn] (if (empty? tag-id) tn (str tn "#" tag-id)))
             check-classes (fn [tn] (if (empty? classes) tn (str tn "." (join "." classes))))
             name (-> tag-name
                      check-id check-classes keyword)]
         (if (> (count children) 0)
           [name (attributes node) (vec (map s-expressions children))]
           [name (attributes node) (text node)])))))
