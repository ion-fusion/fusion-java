;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit)

(define key1 "key")
(define key2 (quote key2))
(define key3 (cons 3 3))

(define val (cons 1 2))

(check-false (syntax-property (quote-syntax sym) key1))


(check eq? val
  (syntax-property
    (syntax-property (quote-syntax sym) key1 val)
    key1))


(define stx
  (syntax-property
    (syntax-property
      (syntax-property (quote-syntax sym) key3 3)
      key1 val)
    key2 2))

(check eq? val (syntax-property stx key1))
(check =   2   (syntax-property stx key2))
(check =   3   (syntax-property stx key3))


;; syntax-property uses eq? to match keys
(define _key (string-append "k" "ey"))   ;; not eq? to key
(check-false (eq? _key key1))
(check-false
  (syntax-property
    (syntax-property (quote-syntax sym) _key val)
    key1))


;; Writing replaces existing entries.
(check = 2
  (syntax-property
    (syntax-property (syntax-property (quote-syntax sym) key1 1) key1 2)
    key1))
