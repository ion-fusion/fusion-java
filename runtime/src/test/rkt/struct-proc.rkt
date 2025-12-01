;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

#lang racket

(require rackunit)


;; Subtypes can override prop:procedure
(struct S1 (f) #:property prop:procedure 0)
(define s1 (S1 (lambda () 's1)))
(check-equal? 's1 (s1))

(struct S2 S1 (g) #:property prop:procedure 0)
(define s2 (S2 'not-proc (lambda () 's2)))
(check-equal? 's2 (s2))
