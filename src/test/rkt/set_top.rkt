;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit "fusion.rkt")


"============================================================================="
"Attempted mutation of language bindings"

"Check our assumption that we have a language binding"
(check_pred is_procedure size)
(define our_size size)


(expect_syntax_error
  (set size "fail"))

(expect_syntax_error
  (module Fail racket
    (require "fusion.rkt")
    (set size "fail")))


"Value hasn't changed"
(check === size our_size)


"============================================================================="
"Attempted mutation of imported bindings"

(module M1 racket
  (define m1_bound "in M1")
  (provide m1_bound))
(require 'M1)

(check-exn exn:fail:syntax?
  (lambda ()
    (eval '(set m1_bound "outside"))))
(check === "in M1" m1_bound)

(expect_syntax_error
  (module Fail racket
    (require "fusion.rkt")
    (require 'M1)
    (set m1_bound "fail")))
