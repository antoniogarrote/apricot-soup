(ns apricot-soup-test
  (:use [apricot-soup] :reload-all)
  (:use [clojure.test])
  (:use [clojure.contrib.logging]))

(deftest parse-doc
  (let [doc ($ "<p>A simple <b>test</b> string.</p>")]
    (is ((comp not nil?) doc))
    (is (instance? org.jsoup.nodes.Document doc))))

(deftest parse-doc-from-URL-1
  (let [doc ($ "http://www.google.com")]
    (is ((comp not nil?) doc))
    (is (instance? org.jsoup.nodes.Document doc))))

(deftest parse-doc-from-URL-2
  (let [elems ($ "http://www.google.com" div)]
    (is (> (count elems) 0))))

(deftest parse-doc-from-URL-3
  (let [elems ($ "http://www.google.com" :csi)]
    (is true))) ;; we just test this does not launch an exception

(deftest attr-test1
  (is (= "mundo" ($ "<a hola=mundo>test</a><a adios=mundo>test2</a>" "a[hola=mundo]" (attr "hola")))))

(deftest attr-test2
  (is (= "cosa" ($ "<a hola=mundo>test</a>" a (attr "hey" "cosa") (attr "hey")))))

(deftest has-class-test
  (is ($ "<a hola=mundo class='hey world'>test</a>" a (has-class "world"))))

(deftest html-test
  (is (= "\n<a hola=\"mundo\" class=\"hey world\">test</a>" ($ "<a hola=mundo class='hey world'>test</a>" a (html)))))

(deftest html-test2
  (is (= "\n<a hola=\"mundo\" class=\"hey world\">adios</a>" ($ "<a hola=mundo class='hey world'>test</a>" a (html "adios") (html)))))

(deftest remove-class-test
  (is (= "world" ($ "<a hola=mundo class='hey world'>test</a>" a (remove-class "hey") (attr "class")))))

(deftest test-add
  (is (= " item 1 item 2 hey a paragraph" ($ "<ul><li>item 1</li><li>item 2</li></ul><p>a paragraph</p>" li (add ($ "<a>hey</a>" a)) (add p) (text)))))

(deftest test-children
  (is (= " test" ($ "<div><span><a>test</a></span></div>" div (children) (children) (text)))))

(deftest test-s-expressions
  (is (= '([:div {} [[:div.other.test {:class "test other"} "hola"] [:ul {} [[:li {} "uno"] [:li#two {:id "two"} "dos"]]]]] [:div.other.test {:class "test other"} "hola"])
         ($ "<div><div class='test other'>hola</div><ul><li>uno</li><li id='two'>dos</li></ul></div>" div (s-expressions)))))
