;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

#lang racket

(provide (all-defined-out))



(define-for-syntax (capturing_syntax stx datum)
  ; Transforms datum into syntax objects, using lexical context from
  ; stx which should be the original syntax sexp.
  (datum->syntax stx datum))


; WRONG: Bindings are introduced using the lexical context of the macro definition.
(define-syntax require_grain_naive
  (lambda (stx)
    (quote-syntax (require "grain.rkt"))))

; WRONG: Copies the original context onto just the `require` identifier.
(define-syntax require_grain_wrong
  (lambda (stx)
    (quasisyntax ((unsyntax (capturing_syntax stx (quote require))) "grain.rkt"))))

; RISKY: Copies the original context onto the entire expansion.
;        Will fail if require isn't right at point of use.
(define-syntax require_grain_risky
  (lambda (stx)
    (capturing_syntax stx (quote (require "grain.rkt")))))

; CORRECT: Copies the original context onto the module path.
(define-syntax require_grain_best
  (lambda (stx)
    (quasisyntax (require (unsyntax (capturing_syntax stx "grain.rkt"))))))


; Like basic require, non-renaming only-in uses the lexical context on the
; module path.

(define-syntax require_only_barley_naive
  (lambda (stx)
    (quote-syntax (require (only-in "grain.rkt" barley)))))

; WRONG: Copies the original context onto the bind-id
(define-syntax require_only_barley_wrong
  (lambda (stx)
    (quasisyntax (require (only-in "grain.rkt" (unsyntax (capturing_syntax stx 'barley)))))))

; CORRECT: Copies the original context onto the module path.
(define-syntax require_only_barley_best
  (lambda (stx)
    (quasisyntax (require (only-in (unsyntax (capturing_syntax stx "grain.rkt")) barley)))))


; prefix-in works the same as only-in; the context of the prefix-id is ignored.

; WRONG: Copies the original context onto the prefix-id
(define-syntax require_prefix_barley_wrong
  (lambda (stx)
    (quasisyntax (require (prefix-in (unsyntax (capturing_syntax stx 'my-)) "grain.rkt")))))

; CORRECT: Copies the original context onto the module path.
(define-syntax require_prefix_barley_best
  (lambda (stx)
    (quasisyntax (require (prefix-in my- (unsyntax (capturing_syntax stx "grain.rkt")))))))


; rename-in works a bit differently: the lexical context of the bind-id, not the require-spec, matters.

(define-syntax require_rename_barley_wrong
  (lambda (stx)
    (quasisyntax (require (rename-in (unsyntax (capturing_syntax stx "grain.rkt")) (barley barlee))))))

(define-syntax require_rename_barley_best
  (lambda (stx)
    (quasisyntax (require (rename-in "grain.rkt" (barley (unsyntax (capturing_syntax stx 'barlee))))))))
