;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit)


(define ns (make-base-namespace))

(eval '(define top 17220) ns)

(define top 17110)

(check-eq?
  (eval (quote-syntax (+ top 2)) ns)
  17222)

(check-eq?
  (eval (list (quote +) (quote-syntax top) 2) ns)
  17222)
