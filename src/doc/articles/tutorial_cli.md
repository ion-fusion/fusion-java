<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Tutorial: Exploring the Fusion CLI

Here we'll walk through some basic use cases with Ion Fusion's command-line interface.

> **Prerequisites:** 
>
> * Install a Java runtime, version 8 or later. We recommend [Amazon Corretto][].
> * Ensure that `java` is on your shell's `PATH`.
> * Download the [Ion Fusion SDK][SDK], unpack it somewhere, and add its
>   `bin` directory to your `PATH`.
>
> Alternatively, you can [build the CLI from source](howto_build.html).

The `fusion` CLI has three modes of operation: an interactive REPL, script execution, and direct
evaluation of expressions. We'll start with the latter:

    fusion eval '(+ 1 2)'

That should print `3`.  What's not obvious is that the evaluator is printing the result in
Amazon Ion format.  We can use a more interesting expression to demonstrate:

    fusion eval '[null, (timestamp_at_day 2025-03-28T02:09-07:00)]'

That should print `[null, 2025-03-28]`, using Ion's notation for a timestamp. You can see that the
list in the input expression evaluates to list, and each element of the list is likewise evaluated
as an expression. Fusion makes it easy to construct data very directly. This applies to Ion structs
(JSON objects) as well:

    fusion eval '{ name: "John Doe", date: 2001-03-27, score: (* 7 3) }'

That should print `{name:"John Doe", date:2001-03-27, score:21}` (though the whitespace and field
order may differ).

> **TIP:**
> Ion Fusion uses the Amazon Ion data format as its concrete syntax, leveraging Ion's [symbol][]
> and [S-expression][sexp] types in a Lisp-like style. Put another way, Fusion source code _is_
> Ion data. In general, when a data element isn't an Ion S-expression or symbol, it evaluates to
> itself!

As our last example for now, we'll demonstrate a nontrivial Fusion script: generate a list of
authors to this repository, in chronological order. This real-world example was used to generate the
list in our [CONTRIBUTORS][] page.

First, copy this content into a file named `authors.fusion`:


    (define all_names
      '''
    A sexp (linked list) containing all the values on the
    current input stream.
      '''
      (series_to_sexp (in_port)))
    
    (define (deduplicate s)
      '''
    Remove duplicate values from sexp `s`, keeping the _last_
    copy of any duplicates.
      '''
      (if (is_empty s) s
        // Decompose the sexp into its head/tail (aka car/cdr).
        (let [(name   (head s)),
              (others (tail s))]
          (if (any (|n| (== n name)) others)
            // The name is in the tail, so ignore this copy.
            (deduplicate others)
            // The name is not in the tail, so keep it and
            // dedup the tail.
            (pair name (deduplicate others))))))
    
    // Print the deduplicated names in chrono order, one per line.
    (for [(name (reverse (deduplicate all_names)))]
      (displayln name))


Now, run the script over the output of `git log`:

    git log --pretty=format:'"%an"' | fusion load authors.fusion

You'll see the deduplicated names, one per line, in order of their first commit. This isn't
necessarily the easiest way to accomplish this task, but it demonstrates the use of Fusion for
ad-hoc data processing.


## What's Next?

With these basic examples at hand, we recommend browsing the
[Ion Fusion Language Reference](fusion.html) to learn more about the operators used in the 
preceding examples.

[About the `fusion` CLI](about_cli.html) explains the interface in more detail.


[Amazon Corretto]: https://aws.amazon.com/corretto
[CONTRIBUTORS]: https://github.com/ion-fusion/fusion-java/blob/main/CONTRIBUTORS.md
[SDK]:          https://github.com/ion-fusion/fusion-java/releases
[sexp]:   https://amazon-ion.github.io/ion-docs/docs/spec.html#sexp
[symbol]: https://amazon-ion.github.io/ion-docs/docs/spec.html#symbol
