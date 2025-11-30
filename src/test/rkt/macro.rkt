;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

(require rackunit)


;;============================================================================
;; Breaking hygiene via datum_to_syntax

(define-syntax capture  ; capture use of 'c'
  (lambda (stx)
    (let* ((arg (cadr (syntax-e stx)))
           (c   (datum->syntax arg (quote c))))
      (quasisyntax
        (let (((unsyntax c) 2))
          (unsyntax (datum->syntax (quote-syntax here) arg)))))))

(check-eq?
  (let ((mult *) (c 5))
    (capture (mult 10 c)))
  20)
