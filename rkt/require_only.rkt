;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit "fusion.rkt")

"Check our assumptions"
(expect_variable_exn one)
(expect_variable_exn two)


(module M1 racket
  (define one "M1 1")
  (define two "M1 2")
  (provide one two))

(module M2 racket
  (define one "M2 1")
  (define two "M2 2")
  (provide one two))

(require (only_in 'M1 two))
(expect_variable_exn one)
(check === two "M1 2")

(require (only_in 'M2 one))
(check === one "M2 1")
(check === two "M1 2")

"`one` is the same, `two` changes modules"
(require (only_in 'M2 one two))
(check === one "M2 1")
(check === two "M2 2")


(module Good racket
  (require rackunit "fusion.rkt")
  (require (only_in 'M1 one) (only_in 'M2 two))
  (check === one "M1 1")
  (check === two "M2 2"))


"============================================================================"
"Importing an identifier that's not provided by the given module"

(expect_syntax_error
  (require (only_in 'M1 three)))
(expect_variable_exn three)

(expect_syntax_error
  (module Fails racket
    (require "fusion.rkt")
    (require (only_in 'M1 three))))


"============================================================================"
"An identifier appears multiple times in the same `require`"

"Duplicates are allowed at top-level"
(require (only_in 'M1 one)
         (only_in 'M2 one))
(check === "M2 1" one)

(expect_syntax_error
  (module Fails racket
    (require "fusion.rkt")
    (require (only_in 'M1 one))
    (require (only_in 'M2 one))))

(expect_syntax_error
  (module Fails racket
    (require "fusion.rkt")
    (require (only_in 'M1 one)
             (only_in 'M2 one))))
