# Regexp API

The `/fusioncontrib/regexp` module is broken up into two classes of procedures - Java specific
procedures for interfacing with the Java regular expression classes and generalized regular
expression procedures for using with any regular expression implementation.

* `jregexp`
  Creates a new regular expression object.
* `jregexp_quote`
  Escapes a string in such a way that it can be safely used in a `jregexp`.

While there is only a Java implementation of a `regexp` object, the following methods should be safe
to use with any other regular expression objects going forward:

* `is_regexp`
  Determines whether the input is a `regexp`.
* `regexp_match`
  Finds the first match within a string.
* `regexp_match_g`
  Finds all matches within a string.
* `regexp_match_positions`
  Finds the first match position within a string.
* `regexp_match_positions_g`
  Finds all match positions within a string.
* `regexp_matches`
  Determines whether a pattern matches any part of an input string.
* `regexp_matches_exact`
  Determines whether a pattern matches an entire input string.
* `regexp_replace`
  Replaces a match with a replacement pattern or the result of a procedure.
* `regexp_replace_g`
  Replaces all matches globally.
* `regexp_replace_quote`
  Creates an escaped string which is safe for using as a replacement in the `regexp_replace*`
  procedures.
* `regexp_replaces`
  Performs multiple replacements on a string.
* `regexp_split`
  Splits a string on a `regexp` match.
* `regexp_to_string`
  Generates a string which represents the `regexp` pattern.
