;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit)


;;============================================================================
;; Check use at module context

(check-exn exn:fail?
  (lambda ()
    (eval (quote
      (provide eval)))))   ; ERROR: not at module level


(check-exn exn:fail?
  (lambda ()
    (eval (quote
      (module M racket
         (if true (provide if) true))))))   ; ERROR: not at module level


;;============================================================================
;; Should be able to provide something twice

(module Twice racket
  (define twice 2)
  (provide twice)
  (provide twice))

(require 'Twice)
(check-eq? twice 2)


;;============================================================================
;; Check simple all_defined_out

(module M1 racket
  (define m1 1)
  (define m2 2)
  (provide (all-defined-out)))

(require 'M1)
(check-eq? m1 1)
(check-eq? m2 2)


;;============================================================================
;; Check that all_defined_out filters identifiers based on lexical context.

(define top "at-top")

(module M2 racket
;  (require "/fusion/experimental/syntax")
  (define-syntax deftop
    (lambda (stx)
      (quasisyntax
        (define top (unsyntax (cadr (syntax-e stx)))))))
  (define-syntax defpubtop
    (lambda (stx)
      (quasisyntax
        (begin (define top (unsyntax (cadr (syntax-e stx))))
               (provide (all-defined-out))))))
   (provide (all-defined-out)))

(require 'M2)
(deftop "macro")          ; Defines a different top, with marks from the macro
(check-eq? top "at-top")

(module M3 racket
  (require 'M2)
  (deftop "inside M3")
  ;; This should not export top since it was introduced via macro
  (provide (all-defined-out)))

(require 'M3)
(check-eq? top "at-top")


(module M4 racket
  (require 'M2)
  ;; This DOES export top
  (defpubtop "inside M4"))
(require 'M4)
(check-eq? top "inside M4")


;; Check failure exporting two bindings w/same name
(check-exn exn:fail?
  (lambda ()
    (eval
      '(module M5 racket
         (require 'M2)
         (define top "M5")
         (provide top)
         (defpubtop "macro M5")))))


;;============================================================================
;; Check requires using macro-introduced identifiers

(define top "at-top")

(module Top0 racket
  (define top "in Top0")
  (provide top))

(module top_import_mark racket
  (define-syntax import-top-mark
    (syntax-rules ()
      ;; Here our require uses a macro-introduced identifier
      [(_) (require (only-in 'Top0 top))]))
  (provide import-top-mark))

(require 'top_import_mark)
(import-top-mark)
(check-eq? top "at-top")

(require (only-in 'Top0 top))  ;; Same as import-top-mark, but not via macro
(check-eq? top "in Top0")
(define top "at-top")          ;; Restore state for consistency
(check-eq? top "at-top")

(module check_top_import_mark racket
  (define top "in top_import_mark")
  (require 'top_import_mark rackunit)
  (import-top-mark)
  (check-eq? top "in top_import_mark"))


(printf "success~n")
