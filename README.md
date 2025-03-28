<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

**Ion Fusion** is a programmable programming language for working with JSON and [Amazon Ion][ion]
data. Its goal is to simplify data processing by eliminating impedance mismatch and enabling
domain-specific custom syntax. Among its interesting characteristics:

* Fusion source code is Ion data, and the Fusion type system is a superset of the
  [Ion data model][data]. This reduces glue code such as object binding, and eliminates lossy type
  conversions.
* While Fusion facilitates general-purpose use, you can use syntactic abstractions to create custom
  syntax forms and Domain-Specific Languages, even those that are not as general as the core.

Linguistically, Fusion is a dialect of [Racket][], adapted to the Ion notation. Here's a quick 
sample:

```
(for [(value (in_port))]                 // Read each Ion value from stdin into local var `value`
  (writeln (. value "marketplace_id")))  //   and write its marketplace_id field to stdout
```

Fusion has an interactive command console for experimentation, and a "batch" mode for running
scripts and pipelining data. It can also be embedded into Java applications and services, with
some support for sandboxing untrusted code.

Ion Fusion started inside Amazon in 2012, and has been used in production for over a
decade, driving numerous data processing, workflow management, and analytics systems.
It is now an independent Apache-licensed project led by current and former Amazonians.

> [!IMPORTANT]
> This repository is under active development, preparing for a 1.0 open source release. The language
> itself is largely stable, but expect significant changes to Java APIs and packaging as we
> renovate everything for public development and use.


# Getting Started

We don't yet have a website or public distro, so you'll need to build from source to run Fusion or
browse its documentation. This should be straightforward:

```shell
git clone https://github.com/ion-fusion/fusion-java.git
cd fusion-java
./gradlew release
```

After a successful release build, you'll have a basic SDK under `build/install/fusion`. The notable
artifacts within that directory are:

* `bin/fusion` is the `fusion` CLI
* `docs/fusiondoc/fusion.html` is the documentation for the Ion Fusion language
* `docs/javadoc/index.html` is the documentation embedding Ion Fusion in your Java application
* `lib` holds the jars needed for embedding

To experiment with the CLI, add the `bin` to your path:

```shell
PATH=$PATH:$PWD/build/install/fusion/bin
fusion help
```
That should give you an overview of the CLI. 
It has a few modes of operation: an interactive REPL, script execution, and direct 
evaluation of expressions. We'll start with the latter:

```shell
fusion eval '(+ 1 2)'
```

That should print `3`.  What's not obvious is that the evaluator is printing the result in 
Amazon Ion format.  We can use a more interesting expression to demonstrate:

```shell
fusion eval '[null, (timestamp_at_day 2025-03-28T02:09-07:00)]'
```

That should print `[null, 2025-03-28]`, using Ion's notation for a timestamp. You can see that the
list in the input expression evaluates to list, and each element of the list is likewise evaluated
as an expression. Fusion makes it easy to construct data very directly. This applies to Ion structs
(JSON objects) as well:

```shell
fusion eval '{ name: "John Doe", date: 2001-03-27, score: (* 7 3) }'
```

That should print `{name:"John Doe", date:2001-03-27, score:21}` (though the whitespace and field 
order may differ).

> [!TIP]
> Ion Fusion uses the Amazon Ion data format as its concrete syntax, leveraging Ion's [symbol][] 
> and [S-expression][sexp] types in a Lisp-like style. Put another way, Fusion source code _is_
> Ion data. In general, when a data element isn't an Ion S-expression or symbol, it evaluates to 
> itself!

As our last example for now, we'll demonstrate a nontrivial Fusion script: generate a list of 
authors to this repository, in chronological order. This real-world example was used to generate the
list in our [CONTRIBUTORS page](CONTRIBUTORS.md).

First, copy this content into a file named `authors.fusion`:

```
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
```

Now, run the script over the output of `git log`:

```shell
git log --pretty=format:'"%an"' | fusion load authors.fusion
```

You'll see the deduplicated names, one per line, in order of their first commit. This isn't 
necessarily the easiest way to accomplish this task, but it demonstrates the use of Fusion for 
ad-hoc data processing.

With these basic examples at hand, we recommend browsing the language's documentation tree:

```shell
open build/install/fusion/docs/fusiondoc/fusion.html
```

Thanks for trying out Ion Fusion!  Have fun, and don't be afraid to reach out on [Slack][slack] if
you have questions.


# Support

This product is not owned or supported by Amazon or AWS, but by a small group of volunteers.
At this time we cannot promise any particular SLA for responding to issues.

Please submit bug reports and feature requests via [GitHub issues][issues].
If you have questions, feel free to reach out on our [Slack workspace][slack].


# Contributing

Contributions of all kinds are welcome! Our [Contributor's Guide](CONTRIBUTING.md) can help you get 
going.  We are deeply grateful to our many [contributors](CONTRIBUTORS.md).


# License

Ion Fusion components are licensed under the [Apache License 2.0](LICENSE). Unless required by
applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
[License](LICENSE) for the specific language governing permissions and limitations under the
License.


[data]:   https://amazon-ion.github.io/ion-docs/docs/spec.html
[ion]:    https://amazon-ion.github.io/ion-docs/index.html
[issues]: https://github.com/ion-fusion/fusion-java/issues
[Racket]: https://racket-lang.org/
[sexp]:   https://amazon-ion.github.io/ion-docs/docs/spec.html#sexp
[slack]:  https://join.slack.com/t/ion-fusion/shared_invite/zt-2y0jr8vh2-bZLa66hdyZ3ykHcgOcYkcA
[symbol]: https://amazon-ion.github.io/ion-docs/docs/spec.html#symbol
