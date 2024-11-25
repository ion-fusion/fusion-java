;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit "fusion.rkt")


(define-syntax if1head3
  (lambda (stx)
    #'(if 1 head 3)))


(load "ftst/syntax_original.rkt.fusion")
