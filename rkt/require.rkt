"Copyright (c) 2016 Amazon.com, Inc.  All rights reserved."

(require rackunit "fusion.rkt")


;(expect_syntax_exn (require))    ; TODO this works in Racket
(expect_syntax_exn (require 23))

(expect_syntax_exn (require ""))
(expect_syntax_exn (require " module"))

(module M1 racket
  (define one 1)
  (provide one))
(module M2 racket
  (define one 2)
  (provide one))

"Interesting that this works at top-level"
(require 'M1 'M2)
(check === 2 one)

(expect_syntax_exn
  (module Fail racket
    (require 'M1 'M2)))

(expect_syntax_exn
  (expand
    (quote_syntax (module Fail racket
                    (require 'M1 'M2)))))
