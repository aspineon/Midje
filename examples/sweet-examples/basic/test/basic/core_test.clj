
(ns basic.core-test
  (:use clojure.test)
  (:use midje.sweet)
)

(defn note-expected-failure [] (println "^^^^ The previous failure was expected ^^^^"))

                       ;;; 

;; This is an example of the Midje version of a clojure.test test that would 
;; look like this:
;;      (is (= (+ 1 1) 2))
;; In Midje's preferred metaphor, such statements are facts about the
;; world, so its version looks like this:

(fact (+ 1 1) => 2)

;; You can have multiple assertions within the fact form. The function
;; has an alias, facts, for those of you who are sticklers about
;; grammar:

(facts "arithmetic"
 (+ 1 1) => 2
 (+ 1 0) => 1)

;; Notice also that you can add "doc strings" to facts. Right now, they're just ignored.

;;
;; At the moment, midje uses the clojure.test reporting mechanism, so you can wrap
;; facts within deftest.

(deftest example-of-a-simple-equality-test
  (fact (+ 1 1) => 2))

;; If you don't wrap the facts within deftest, you can still run them with 'lein test',
;; but you won't get correct counts for successes and failures. In the future, Midje will
;; probably have its own reporting infrastructure that will Do the Right Thing.

;; Failing tests should look familiar:
;;     FAIL at (core_test.clj:35)
;;     expected: 3
;;       actual: 4

(fact ( #(+ 1 %) 3) => 3)                                          (note-expected-failure)

;; You can also use functions on the right-hand side. In that case,
;; the actual result is passed as the function's single argument.
(fact ( #(+ 1 %) 3) => odd?)                                       (note-expected-failure)
;; The failing test will look slighly different:
;;     FAIL at (core_test.clj:47)
;;     Actual result did not pass expected function.
;;     expected function: odd?
;;         actual result: 4

;; If you're testing something that produces a function, use
;; (exactly):

(fact (first [even? odd?]) => (exactly odd?))                      (note-expected-failure)

;;     FAIL at (core_test.clj:57)
;;     Actual result did not pass expected function.
;;     expected function: (exactly odd?)
;;         actual result: #<core$even_QMARK___4680 clojure.core$even_QMARK___4680@494b6bed>

;; There are a number of matching functions available. You can find them all with 
;;            (ns-publics (40 'midje.checkers))
;; They have doc strings.
;; Here's one of them:

(fact
  [3 1 2] => (in-any-order [1 2 3])                         ;; succeeds
  [3 3 1 2] => (in-any-order [1 2 3]))                       (note-expected-failure)
;;     FAIL at (core_test.clj:69)
;;     Actual result did not pass expected function.
;;     expected function: (in-any-order [1 2 3])
;;         actual result: [3 3 1 2]

;; Facts work with variables bound both within and outside of the fact form.


(let [a 3]
  (fact (+ a a) => 6)
  (fact (let [b 33]
	  (+ a b) => 36)))



;; Sometimes facts are only true provided other facts are true. Midje
;; has a notation for that. Suppose we're testing the rules for
;; Conway's Life (as used in Corey Haine's Code Retreat workshops). In
;; it, a cell "comes to life" if it is dead but has three living
;; neighbors. Let's say that rule will be part of an
;; alive-in-next-generation? function. Clojure requires it to be
;; declared before being used in something like a test or fact:

(defn alive-in-next-generation? [cell]  
  ; not started yet.
)

;; Here's an expression of the rule that lets you get the logic of
;; alive-in-next-generation? right before fussing with counting
;; neighbor cells:

(fact
 (alive-in-next-generation? ...cell...) => truthy
   (provided 
    (alive? ...cell...) => false
    (neighbor-count ...cell...) => 3))                                         (note-expected-failure)

;; Notes:
;; ...cell... : I use this shorthand to represent some 
;;              random value. None of its characteristics
;;              matter except those defined below. You
;;              don't have to define such "metavars" -
;;              Midje does it for you.
;;
;; truthy     : Truthy is another one of Midje's checking
;;              functions. It accepts any value other than
;;              false or nil.
;;
;; provided   : Provided gives a list of facts the original
;;              fact depends upon. (Note that the "provided"
;;              form isn't within the original claim, but follows
;;              just after it.  

;; When the fact is checked, it will fail like this:

;;     FAIL for (core_test.clj:92)
;;     This expectation was never satisfied:
;;     (alive? ...cell...) should be called at least once.

;; The message isn't the greatest -- it should probably say something
;; about a fact never being used -- and it will probably change in the
;; future.

;; If you rerun this test, you'll find that the line number in the 
;; actual error will point to the line where the fact begins, not to
;; the line line containing (alive? ...cell...). This will be improved
;; in later versions, but it can't be made as exact as we'd wish.

;; Notice that we didn't have to declare either alive? or
;; neighbor-count. Midje takes care of that. However, we have to do
;; that as soon as we fill in the body of the
;; alive-in-next-generation? I usually use this macro for that:

(only-mocked alive? neighbor-count)

;; It defines its arguments as functions that blow up spectacularly if
;; they're ever called, which adds a little clarity if you forget to
;; get around to implementing them. It also serves as a handy little
;; todo list.

;; You can mention a particular unimplemented function more than once,
;; giving it a different argument each time:

(only-mocked g)

(defn g-adder [n1 n2]
  (+ (g n1) (g n2)))

(fact
 (g-adder 2 3) => 11
 (provided
  (g 2) => 4
  (g 3) => 7))

;; When trying to match arguments in the provided functions, Midje
;; uses the same rules as when checking a function-under-test's actual
;; result. That means you can use single-argument matching functions
;; and you must use (exactly) to match a functional argument.

(only-mocked first-subfunction another-subfunction)
  
(defn function-under-test []
  (+ (first-subfunction 1 2 '(a blue cow))
     (another-subfunction inc)))

(fact (function-under-test) => 11
        (provided
          (first-subfunction odd? even? anything) => 1
	  (another-subfunction (exactly inc)) => 10))

;; The return values of a subfunction don't follow the rules for 
;; arguments. They're literal constant values.

