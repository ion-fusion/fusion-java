;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit "fusion.rkt")


;(expect_syntax_error (require))    ; TODO this works in Racket
(expect_syntax_error (require 23))

(expect_syntax_error (require ""))
(expect_syntax_error (require " module"))

(module M1 racket
  (define one 1)
  (provide one))
(module M2 racket
  (define one 2)
  (provide one))

"Interesting that this works at top-level"
(require 'M1 'M2)
(check === 2 one)

(expect_syntax_error
  (module Fail racket
    (require 'M1 'M2)))

(expect_syntax_error
  (expand
    (quote_syntax (module Fail racket
                    (require 'M1 'M2)))))
