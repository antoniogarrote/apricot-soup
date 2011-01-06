# apricot-soup

A library that makes easy the manipulation of HTML documents.
This library is a wrapper around the JSoup Java library.

One example: retrieving the title of all the news in Hacker news:

    ($ "http://news.ycombinator.com" td.title a (map #(text %1)))


## Usage

- Retrieval and parsing

    ($ "http://news.ycombinator.com")

- Selecting DOM elements
  (all the available JSoup selectors can be found here:
  http://jsoup.org/apidocs/org/jsoup/select/Selector.html

    ; equivalent
    ($ "http://news.ycombinator.com" (search "tr")  (search "a"))
    ($ "http://news.ycombinator.com" "tr" "a")
    ($ "http://news.ycombinator.com" tr a)
     
    ; equivalent
    ($ "http://www.google.com" (search "#footer"))
    ($ "http://www.google.com" "#footer")
    ($ "http://www.google.com" :footer)

- Mixing any function in the chain of selectors

    ($ "http://news.ycombinator.com" td a
       (filter (fn [e] (.startsWith (attr "href" e) "http")))
       (map (fn [e] {(attr "href" e) (text e)})))
     
    ;; returns a map with uri -> title

- Manipulating the wrapped set adding elements from the same document
  or a different document

    ($ "<ul><li>item 1</li><li>item 2</li></ul><p>a paragraph</p>" li (add ($ "<a>hey</a>" a)) (add p) (text))
    ; gets " item 1 item 2 hey a paragraph"

- Transforming an element into a set of S-Expressions

    ($ "<div><div class='test other'>hola</div><ul><li>uno</li><li id='two'>dos</li></ul></div>" div (s-expressions))

    ; outputs
    ([:div {}
      [[:divother.test {:class "test other"} "hola"]
       [:ul {}
         [[:li {} "uno"]
          [:li#two {:id "two"} "dos"]]]]]
     [:div.other.test {:class "test other"} "hola"])

## Installation

leiningen and clojars:

    [apricot-soup "0.0.3-SNAPSHOT"]

## License

Copyright (C) 2010 Antonio Garrote

Distributed under the LGPL license.
