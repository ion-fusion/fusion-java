;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

#lang racket

;;; Test cases illustrating how Racket's require-spec forms determine the lexical context
;;; of imported identifiers.  There are a couple surprises here!
;;;
;;; Docs: https://docs.racket-lang.org/reference/require.html


(require rackunit
         "fusion.rkt"
         "require_macro.rkt")


;; Neither of these work, because there lexical context on the module-path is incorrect.

(require_grain_naive)
(expect_syntax_error barley)

(require_grain_wrong)
(expect_syntax_error barley)


;; The "risky" macro has context on the both `require` and the module path.

(define (risky_barley enable_require)
  ;; Test top-level handling of `require_grain_risky` which is expected
  ;; to fail when `require` does not have its normal meaning.
  ;; That case is performed when `enable_require` is untruthy.
  (parameterize [(current-namespace (make-base-namespace))]
    (eval (quasiquote
           (begin
             (require "require_macro.rkt")
             (unquote (or enable_require
                          (quote (define (require m) "HAHA"))))
             (require_grain_risky)
             barley)))))

;; With `require` enabled, things work.
(check === (risky_barley true) "soup")

;; With `require` disabled, we can't access the imported binding.
(check-exn exn:fail:contract:variable? (lambda () (risky_barley false)))


;; Finally, doing it the right way.
(module test_require racket
  (require rackunit
           "fusion.rkt"
           "require_macro.rkt")
  (require_grain_best)
  (check === barley "soup"))
(require 'test_require)


;; Now, similar tests for require only-in

(require_only_barley_naive)
(expect_syntax_error barley)

(require_only_barley_wrong)
(expect_syntax_error barley)

(module test_require_only racket
  (require rackunit
           "fusion.rkt"
           "require_macro.rkt")
  (require_only_barley_best)
  (check === barley "soup"))
(require 'test_require_only)


;; And for prefix-in

(require_prefix_barley_wrong)
(expect_syntax_error my-barley)
(expect_syntax_error barley)

(module test_require_prefix racket
  (require rackunit
           "fusion.rkt"
           "require_macro.rkt")
  (require_prefix_barley_best)
  (check === my-barley "soup"))
(require 'test_require_prefix)


;; And for rename-in, which works differently.

(require_rename_barley_wrong)
(expect_syntax_error barlee)
(expect_syntax_error barley)

(module test_require_rename racket
  (require rackunit
           "fusion.rkt"
           "require_macro.rkt")
  (require_rename_barley_best)
  (check === barlee "soup"))
(require 'test_require_rename)
