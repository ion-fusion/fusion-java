;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit)

(check-exn exn:fail?
  (lambda ()
    (eval
      'free)))

(define (free_ref)
  free)

(check-exn exn:fail?
  (lambda ()
    (eval
      '(free_ref))))   ; ERROR

(check-exn exn:fail?
  (lambda ()
    (eval
      '(define free free))))

;; Still not defined
(check-exn exn:fail?
  (lambda ()
    (eval
      'free)))


(define free "bird")
(check-eq? "bird" (free_ref))

(define free "ball")
(check-eq? "ball" (free_ref))

(module M racket
  (define free "money")
  (provide free))

(require 'M)

(check-eq? "money" free)
(check-eq? "ball" (free_ref))

(define free "beer")
(check-eq? "beer" free)
(check-eq? "beer" (free_ref))

;; Body of the definition uses the existing binding
(define (+ a b)
  (+ 100 a b))

(define num 1)
(define num (+ 3 num))
(check-eq? 104 num)


(printf "success~n")
