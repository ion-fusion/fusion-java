;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

;; Demonstrates how Racket interprets relative module paths WRT submodules.
;;  https://github.com/ion-fusion/fusion-java/issues/166

(require rackunit)

(module M1 racket
  (define m1 "in M1")
  (provide m1))

(require 'M1)
(check-equal? "in M1" m1)


(module M2 racket
  (require 'M1)
  (define m1-via-M2 m1)
  (provide (all-defined-out)))

(require 'M2)
(check-equal? "in M1" m1-via-M2)


(module M3 racket
  (module M1 racket
    (define m1 "in M3/M1")
    (provide m1))
  (require 'M1)               ; references submodule M1
  (define m1-via-M3 m1)
  (provide (all-defined-out)))

(require 'M3)
(check-equal? "in M3/M1" m1-via-M3)


(module M4 racket
  (require 'M1)               ; references top-level M1; submodule not declared yet
  (module M1 racket
    (define m1 "in M4/M1")
    (provide m1))
  (define m1-via-M4 m1)
  (provide (all-defined-out)))

(require 'M4)
(check-equal? "in M1" m1-via-M4)
