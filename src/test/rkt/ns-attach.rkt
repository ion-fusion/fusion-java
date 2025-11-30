;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

#lang racket
(require rackunit)


(define first-resolver-args #f)
(define first-resolver-current-ns #f)

(define my-resolver
  (let ([orig-resolver (current-module-name-resolver)])
    (lambda args
      (unless first-resolver-args
        (set! first-resolver-args args)
        (set! first-resolver-current-ns (current-namespace)))
      (apply orig-resolver args))))


(define src-namespace (make-base-namespace))
(eval '(module food racket/base
         (provide apple)
         (define apple "pie")) src-namespace)
(namespace-require ''food src-namespace)

(parameterize ([current-module-name-resolver my-resolver]
               [current-namespace (make-base-namespace)])
  (namespace-attach-module src-namespace ''food))

;; namespace-attach-module resolves the module name using the src-namespace
(check-equal? first-resolver-args '('food #f #f #f))
(check-eq? first-resolver-current-ns src-namespace)
