;; Copyright Ion Fusion contributors. All rights reserved.
;; SPDX-License-Identifier: Apache-2.0

#lang racket

(require rackunit)

(define f
  ;; Just getting some distance between the `define` and `lambda`.
  (let [(g 1)]
    (lambda () g)))  
(check-equal? 'f (object-name f))


; The object-names for structs are based on the given symbolic name.
; They are not "inferred" from surrounding definitions, like with procedures.

(let-values [((struct:U make-U U? U-ref U-set!)
              (make-struct-type 'U-arg #f 1 0))]
  (check-equal?   '(     U-arg    make-U-arg U-arg? U-arg-ref U-arg-set!)
  (map object-name (list struct:U make-U     U?     U-ref     U-set!    ))))


(define-values (struct:V make-V V? V-ref V-set!)
  (make-struct-type 'V-arg #f 1 0))

(check-equal?    '(     V-arg    make-V-arg V-arg? V-arg-ref V-arg-set!)
 (map object-name (list struct:V make-V     V?     V-ref     V-set!    )))


; The constructor name can be customized.
; Presumably, this is how `struct` names the ctor after the type.

(define-values (struct:W make-W W? W-ref W-set!)
  (make-struct-type 'W-arg #f 1 0 #f '() #f #f '() #f 'custom))

(check-equal?    '(     W-arg    custom W-arg? W-arg-ref W-arg-set!)
 (map object-name (list struct:W make-W W?     W-ref     W-set!    )))


; The `struct` form doesn't expose the X-ref and X-set! procedures that are
; created by the underlying `make-struct-type` call.

; TODO the above isn't tested here.

(struct X ([f #:mutable]))  ; Use default constructor-id

(check-equal?    '(     X        X X? X-f set-X-f!)
 (map object-name (list struct:X X X? X-f set-X-f!)))


; `struct` also allows ctor customization

(struct Y () #:name Y-name #:constructor-name Y-custom)

(check-equal?    '(     Y        Y-custom Y?)
 (map object-name (list struct:Y Y-custom Y?)))


"SUCCESS"
