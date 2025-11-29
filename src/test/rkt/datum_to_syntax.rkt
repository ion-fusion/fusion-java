;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit)

(define ctx (quote-syntax ctx))

(define (check-loc actual expected)
  (check-eq? (syntax-line     actual) (syntax-line     expected))
  (check-eq? (syntax-column   actual) (syntax-column   expected))
  (check-eq? (syntax-position actual) (syntax-position expected)))

;; When given a location, datum->syntax uses it for all new syntax objects
;; created, not just the outermost one.

(let ([s (datum->syntax ctx '(a (b)) ctx)])
  (check-loc s ctx)
  (check-loc (car   (syntax-e s)) ctx)  ; a
  (check-loc (cadr  (syntax-e s)) ctx)  ; (b)
  (check-loc (car (syntax-e (cadr (syntax-e s)))) ctx))  ; b

;; Exiting syntax objects keep their locations.

(define inner1 (quote-syntax inner1))
(define inner2 (datum->syntax ctx 'inner2)) ; no location

(let ([s (datum->syntax ctx `(,inner1 (,inner2)) ctx)])
  (check-loc s ctx)
  (check-loc (car   (syntax-e s)) inner1)                   ; inner1
  (check-loc (cadr  (syntax-e s)) ctx)                      ; (inner)
  (check-loc (car (syntax-e (cadr (syntax-e s)))) inner2))  ; inner2


(define val 'outside_M)

(module M racket
  (define val 'inside_M)
  (define inside_id #'inside_id)
  (provide inside_id))
(require 'M)

(check-eq? (eval (datum->syntax inside_id #'val)) 'outside_M)

(let ((val (quote-syntax val)))
  (check-eq? (datum->syntax inside_id val) val))

(let ((val (quote-syntax val)))
  (check-eq?
    (cadr (syntax-e (datum->syntax inside_id (list 1 val))))
    val))
