;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

#lang racket
(require racket/sandbox
         rackunit)

(define (current-registry)
  (namespace-module-registry (current-namespace)))

(define (module->registry m)
  (namespace-module-registry (module->namespace m)))

(define (sandbox->registry s)
  (s '(namespace-module-registry (current-namespace))))


(define curr-ns (current-namespace))
(define-namespace-anchor anchor)
(define base-reg (module->registry 'racket/base))


; User-created namespaces get individual registries.
(check-not-eq? (namespace-module-registry (make-empty-namespace))
               (namespace-module-registry (make-empty-namespace)))

; Sandboxes get individual registries.
(check-not-eq? (sandbox->registry (make-evaluator 'racket))
               (sandbox->registry (make-evaluator 'racket)))


(define empty      (make-empty-namespace))
(define base-empty (make-base-empty-namespace))
(define base       (make-base-namespace))


;;============================================================
(define sb-racket
  (make-evaluator 'racket))

;; This form of sandbox allows redefinition!
(check-eq? 7 (sb-racket '(+ 5 2)))
(sb-racket '(begin (define orig-+ +) (define + -)))
(check-eq? 3 (sb-racket '(+ 5 2)))
(sb-racket '(define + orig-+))

;; Sandboxed expressions are not at module-level.
(check-exn exn:fail:syntax? (lambda () (sb-racket '(provide))))

;;============================================================
(define sb/defns
  (make-evaluator
   'racket                              ; The module's language
   '(define progn-defn 'predefined)))   ; The module's body

(check-eq? 'predefined (sb/defns 'progn-defn))

;; Cannot redefine bindings from a module body.
(check-exn exn:fail? (lambda () (sb/defns '(define progn-defn 'redefined))))

;; Sandboxed expressions are not at module-level.
(check-exn exn:fail:syntax? (lambda () (sb-racket '(provide))))


;;============================================================
(define sb/begin
  (make-evaluator
   '(begin (define lang-var1 'predefined)   ; The "language", but really its racket/base
           (define lang-var2 'predefined))
   '(begin (define prog-var1 'predefined)
           (define lang-var1 'redefined)))) ; interesting!

(check-eq? 'redefined (sb/begin 'lang-var1))

; Language and program variables can both be redefined.
(check-eq? 'shadow (sb/begin '(begin (define lang-var1 'shadow) lang-var1)))
(check-eq? 'shadow (sb/begin '(begin (define prog-var1 'shadow) prog-var1)))

; Surprisingly, current-directory is retained inside the sandbox:
(check-true (string-suffix? (path->string (sb/begin '(current-directory))) "/src/FusionJava/rkt/"))

;; Sandboxed expressions are not at module-level.
(check-exn exn:fail:syntax? (lambda () (sb-racket '(provide))))

;; These is surprisingly slow, whereas (make-evaluator 'racket/base) is fast.
;(make-evaluator '(begin) #:requires '(racket/base))
;(make-evaluator 'racket/base  #:requires '(racket/base))
