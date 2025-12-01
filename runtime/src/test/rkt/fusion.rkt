;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

#lang racket

(require rackunit)
(provide (all-defined-out))
(provide (for-syntax (all-defined-out)))


(define RACKET #t)

(define-syntax only_in (make-rename-transformer #'only-in))

(define-syntax set (make-rename-transformer #'set!))

(define === equal?)

(define-syntax lets (make-rename-transformer #'let*))

(define size length)
(define pair cons)
(define head car)
(define tail cdr)

(define is_procedure procedure?)


;; Syntax

(define (datum_to_syntax datum (ctx #f) (loc #f))
  (datum->syntax ctx datum loc))

(define-syntax define_syntax  (make-rename-transformer #'define-syntax))
(define-syntax quote_syntax   (make-rename-transformer #'quote-syntax))

(define free_identifier_equal free-identifier=?)
(define syntax_is_original   syntax-original?)
(define syntax_property      syntax-property)
(define syntax_track_origin  syntax-track-origin)
(define syntax_unwrap        syntax-e)

;; Deprecated as of R23
(define (syntax_origin stx)
  (syntax-property stx 'origin))

(define UNSET_STX_PROP_VALUE #f) ; Fusion uses void for unset properties.


;; Testing

(define-syntax check_true  (make-rename-transformer #'check-true))
(define-syntax check_false (make-rename-transformer #'check-false))
(define-syntax check_same  (make-rename-transformer #'check-eq?))
(define-syntax check_pred  (make-rename-transformer #'check-pred))

(define-syntax-rule (expect_syntax_error expr)
  (check-exn exn:fail:syntax?
    (lambda ()
      (eval (quote expr)))))

(define-syntax-rule (expect_variable_exn expr)
  (check-exn exn:fail:contract:variable?
    (lambda ()
      (eval (quote expr)))))
