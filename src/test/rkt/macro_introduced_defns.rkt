;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit "fusion.rkt")
(require (for-syntax "fusion.rkt"))


"Here we are making sure that the macro-introduced binding `c` is visible"
"to other code from the macro."
(define_syntax define_c
  (lambda (stx)
    (quasisyntax
      (define (c) c))))
(define_c)

"Similar, but within a module.  Here `d` is unbound where it occurs."
(module M1 racket
  (require "fusion.rkt")
  (define_syntax define_d
    (lambda (stx)
      (quasisyntax
        (define (d) d))))
  (provide define_d))
(require 'M1)
(define_d)


"============================================================================"


(define top "outside")

"Macro that defines a binding with a private identifier."
"Use of this macro crashed the compiler at one point."
(define_syntax define_top
  (lambda (stx)
    (lets ((args (tail (syntax_unwrap stx)))
           (expr (head args)))
      (quasisyntax
        (define top (unsyntax expr))))))
(define_top "macro1")
(check === "outside" top)


"Similar for macro exported from a module"
(module M2 racket
  (require "fusion.rkt" (for-syntax "fusion.rkt"))
  (define_syntax define_top
    (lambda (stx)
      (lets ((args (tail (syntax_unwrap stx)))
             (expr (head args))
             (id   (head (tail args))))
        (quasisyntax
          (begin
            (define top (unsyntax expr))
            (define (unsyntax id) (lambda () top)))))))
   (provide define_top))
(require 'M2)
(define_top "macro2" get_top_1)
(check === "outside" top)
(check === "macro2" (get_top_1))


"Now try using the macro at module level"
(module M3 racket
  (require 'M2 rackunit "fusion.rkt")
  (define top "in_M3")
  (define_top "macro_in1" get_top_in1)
  (check === "in_M3" top)
  (check === "macro_in1" (get_top_in1))
  (define_top "macro_in2" get_top_in2)
  (check === "in_M3" top)
  (check === "macro_in2" (get_top_in2)))
(require 'M3)


"============================================================================"
"A macro-introduced identifier cannot reference a language binding."

(module HasMacro racket
  (require "fusion.rkt" (for-syntax "fusion.rkt"))
  (define_syntax reference_mib
    (lambda (stx)
      (quote_syntax mib)))
  (provide reference_mib))

(module Language racket
  (define mib 17316)
  (provide mib quote require #%module-begin))

"This works:"
(module Test2 'Language
  (require 'HasMacro)
  mib)

"This doesn't work:"
(expect_syntax_error
  (module Test2 'Language
    (require 'HasMacro)
    (reference_mib)))
