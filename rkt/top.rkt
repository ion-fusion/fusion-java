;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit)

(define my_top "at-top")
(check-eq? (#%top . my_top) "at-top")


(let [(my_top "shadow")]
  (check-eq? (#%top . my_top) "at-top"))


(module T racket
  (define my_top "in-T")
  (provide my_top))
(require 'T)
(check-eq? (#%top . my_top) "at-top")
(let [(my_top "shadow")]
  (check-eq? (#%top . my_top) "at-top"))

;; Hide the binding from T
(define my_top "at-top")

(check-exn exn:fail?
  (lambda ()
    (eval
      '(module M racket
         (define my_top "in-M")
         (let [(my_top "shadow")]
           (#%top . my_top))))))   ; ERROR


(module M racket
  (require rackunit)
  (define my_top "in-M")
  (check-eq? (#%top . my_top) "in-M")

  ;; Here the #%top finds the "real" top-level definition,
  ;; despite having different marks.
  (define-syntax define_accessor
    (syntax-rules ()
      [(_ name)
       (define (name)
         (#%top . my_top))]))

  ;; Here the %top will find the accompanying definition
  ;; but that defn won't interfere with the "real" top-level
  ;; my_top because it has different marks.
  (define-syntax define_accessor2
    (syntax-rules ()
      [(_ name)
       (begin
         (define my_top "another one")
         (define (name)
           (#%top . my_top)))]))

    (define-syntax define_accessor3
    (syntax-rules ()
      [(_ name)
       (begin
         (define my_top "yet another")
         (define_accessor name))]))

  (provide define_accessor define_accessor2 define_accessor3))

(require 'M)

; Macro-introduced #%top causes access to top-level, not module-level.
(define_accessor a1)
(check-eq? my_top "at-top")
(check-eq? (#%top . my_top) "at-top")
(check-eq? (a1) "at-top")

(define_accessor2 a2)
(check-eq? my_top "at-top")
(check-eq? (#%top . my_top) "at-top")
(check-eq? (a1) "at-top")
(check-eq? (a2) "another one")

(define_accessor3 a3)
(check-eq? my_top "at-top")
(check-eq? (#%top . my_top) "at-top")
(check-eq? (a1) "at-top")
(check-eq? (a2) "another one")
(check-eq? (a3) "at-top")

(printf "success~n")
