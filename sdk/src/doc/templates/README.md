<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Mustache Quick Reference

The `mustache.java` library does not handle whitespace per spec, which hampers using whitespace to
improve template legibility.

* https://github.com/spullara/mustache.java/issues/211
* https://github.com/spullara/mustache.java/pull/215


## Context references

### Simple names

A simple reference (just a name) is resolved by examining each value in the context stack, from
the top, to find a matching property with that name.

Property matching dispatches on the value's concrete type:
  * Given a `Map`, if it contains an entry keyed by the name, that value is returned.
  * Otherwise, the name is matched against the value's members, in this order:
    * non-private, no-argument method with exactly the given name.
    * non-private JavaBeans-style property getter for the given name.
        * If the name is `foo` then the accessor is `getFoo()`.
    * non-private JavaBeans-style *boolean* property accessor for the given name.
        * If the name is `foo` then the accessor is `isFoo()`.
    * non-private field of the given name.

Note: by default, non-public types have their interfaces and superclasses introspected.
Note: as a special case, `String.getValue` is ignored.

### Dotted names

A name withs dots like `foo.bar` denotes chained lookup. The first key is resolved against the
context stack to find an initial value. The remaining keys are then resolved against the result
of the previous lookup.


## Interpolation

The form `{{reference}}` denotes interpolation: the reference is resolved against the context
stack, and the resulting value is converted to a string and inserted into the output.


## Sections

The form `{{#reference}}...{{/reference}}` denotes iteration over the result of the reference,
which is resolved as described above.

Rendering depends on the type of the result:

  * For `List`, `Iterable`, `Iterator`, or array, then the section is rendered for each
    element in the collection.  In case of an empty collection, the section is omitted.
  * For booleans, the section is rendered if the value is `true`.
  * For `String`s, the section is rendered if the value is not empty.
  * Any other non-null value is rendered as a singleton.

In all cases, the element is pushed onto the context stack before the section is rendered;
it can be interpolated via `{{.}}`.