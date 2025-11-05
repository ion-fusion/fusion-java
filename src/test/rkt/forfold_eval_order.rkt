;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

#lang racket

(require rackunit)

;; This corresponds to the evaluation-order test case in for.test.fusion

(let* [(evaluation_order '())
       (record (lambda (v) (set! evaluation_order (cons v evaluation_order)) v))]
  (check-equal?
    (reverse
      (for/fold
       [(v (begin (record "v") '()))]
       [(a (begin (record "a") (sequence-map record (quote (1 3)))))
        (b (begin (record "b") (sequence-map record (quote (2 4)))))]
      (record "body")
      (cons (list a b) v)))
    '((1 2) (3 4)))
  ; It's unexpected that the accumulator v is initialized after the
  ; sequences a and b, but that's how it works in Racket 6.5.
  (check-equal?
    (reverse evaluation_order)
    '("a" "b" "v" 1 2 "body" 3 4 "body")))
