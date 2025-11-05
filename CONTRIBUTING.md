# Ion Fusion Contributor’s Guide

Thank you for considering a contribution!  We love contributions of all kinds to the Ion Fusion
ecosystem and community, including the language, runtime, tools, documentation, and infrastructure.
This project would not exist without its [contributors](CONTRIBUTORS.md)!

> [!IMPORTANT]
> This document is a work in progress. If you have a question that's not addressed here,
> please open an issue so we know what needs improvement.

<!-- TODO Add issue-creation link to the above. -->
<!-- TODO Style guides (Java code; Fusion code; commit messages; documentation) -->


## Code of Conduct

Please read our [Code of Conduct](https://github.com/ion-fusion/fusion-java#coc-ov-file) and help us
build a welcoming and respectful community. By participating, you are expected to uphold this code.


## What can I contribute?

> [!TIP]
> Check out the issues labeled [`good first issue`][good] or [`help wanted`][help] for suggestions!

[good]: https://github.com/ion-fusion/fusion-java/labels/good%20first%20issue
[help]: https://github.com/ion-fusion/fusion-java/labels/help%20wanted

At present, we are focusing on building out our infrastructure, QA tooling, release processes,
distribution packaging, and so on, all functions that historically were provided by Amazon's
internal developer toolchain. Additionally, we are refreshing our documentation and launching a
public website. Assistance and expertise with the above would be most helpful.

Of course, contributions to the core language and libraries are also welcome and encouraged!
Please be aware that there is a high bar in terms of design, terminology, consistency, style,
documentation, and testing. Not everything will "fit" into the language or standard library, and
many features can reside in user modules without problems.

Documentation is the easiest thing to contribute, and offers a good route to getting to know the
code base and the language’s library and internals.
<!-- TODO Link to info focused on contributing documentation (as opposed to code). -->

Standard-library procedures are usually straightforward, especially those that can be implemented in
Fusion with reasonable efficiency. If Java code is required, it’s somewhat more complicated (and
poorly-documented at present).

New syntactic forms are more advanced, since they require understanding Fusion’s macro system pretty
thoroughly.


## What’s the process?

1. If a relevant GitHub issue exists, comment on it to indicate your interest. We'll help get you
   going by ensuring the issue is up to date. If there's no appropriate issue, create a new one to
   raise the topic. This is a quick way to ensure that there’s not already a reasonable solution,
   that the feature fits within the scope and aims of the language and library, and to gather
   general feedback on the idea.
2. Create a GitHub fork of this repository. You'll push changes to your fork in order to create
   pull requests to the official repository.
3. Get a design review. Draft documentation for the proposed feature and post it as a PR. In most
   cases, this should basically be the documentation for the procedure you’re adding. This allows
   experts to ensure the proposed behavior is well-designed and well-defined, to suggest test cases,
   and so on, before you invest time in implementation.
    * While not required, this step is recommended in order to use your time wisely! We all feel bad
      if your efforts go down the wrong path. Writing docs first is a lightweight way for you to get
      early feedback. It's best to get the high-level details right—particularly the terminology and
      contract—before digging into details.
4. Code it! Don’t forget the unit tests.
5. Post a pull request, and iterate as necessary. **Review early, review often!**
6. Upon approval, your PR will be merged by a code owner, and in most cases the change will be part
   of the next release.

### Issue Management

We track all work items using GitHub issues, and sketch our roadmap using [milestones][]. Code
owners are responsible for maintaining [types][] and labels on issues.

Within a milestone, issues are prioritized with the following labels:

* `Showstopper` (P1)
* `Expected`
* `Important`
* `Nice to Have` (P4)

Most other labels indicate an applicable subsystem or problem space (like `data` or `dsl`), or the
general kind of work required (like `infrastructure` or `documentation`).

[labels]:     https://github.com/ion-fusion/fusion-java/labels
[milestones]: https://github.com/ion-fusion/fusion-java/milestones?direction=asc&sort=due_date&state=open
[types]:      https://github.com/organizations/ion-fusion/settings/issue-types

<!--
TODO Expand on this.
See https://github.com/atom/atom/blob/master/CONTRIBUTING.md#issue-and-pull-request-labels
-->


# Contributing to the Language and Libraries

Most contributors start by enhancing the Ion Fusion language or its standard library. Here's how to
approach that process.

## Design Considerations

### Follow Racket

Fusion’s overall architecture is based on Racket, and most of its features are intentionally aligned
with Racket in both name and behavior. Since Fusion is still very small, most of the features that
users want are already available in Racket, and so contributors should look to Racket for design
guidance and attempt to align with that prior art as much as possible.

<!-- TODO Selecting the right module(s) -->


## Documentation

We recommend as best practice (not just in Fusion but in any software development) that you draft
the documentation for the new features before you start writing code. This is a Working Backwards
technique, ensuring that you can explain things clearly to your future customers before you embark
on building things. If it's awkward to explain, it's probably going to be awkward to use.

Especially if you’re a newcomer to designing Fusion features, it’s a good idea to get a review of
your documentation *first*, to get feedback on what you’re planning to build before you invest a lot
of effort in the wrong direction. This can be as simple as adding a method stub into an existing
Fusion library module that includes the proposed documentation.

### Syntax

The documentation of the Fusion language and libraries is primarily expressed as
code inside the production modules in the `fusion` directory.  HTML is generated at build time
in a manner very similar to JavaDoc; we refer to this as "fdoc" or "fusiondoc".

> [!WARNING]
> The specifics of this mechanism are at present unstable and undocumented from the
> perspective of code outside this repository.

Similar to JavaDoc, prose can be attached to each module overall, and to individual bindings
provided by the module.  The former is accomplished by inserting a string literal immediately after
the module's dialect declaration:

```ion
(module M "/fusion"
  '''
This is the overview documentation for module `M`.  Using Ion's triple-quoted long strings makes it
easy to write long passeges of prose with reasonable formatting.
  '''

  (define something ...
```

To document a binding, insert a string literal into `define` just after the name or signature
subform:

```ion
  (define pi
    "The approximate value of π."
    3.14159)

  (define (square n)
    '''
Returns the square of number `n`.
    '''
    (* n n))
```

When the `define` uses implicit-`lambda` syntax as in the latter example, the signature is
automatically copied into the front of the documentation as a code block.  In other words, that
example is equivalent to:

```ion
  (define square
    '''
    (square n)

Returns the square of number `n`.
    '''
    (lambda (n)
      * n n))
```

Within a documentation string, the syntax is Markdown, basically.

### Style

* In prose paragraphs, refer to procedure arguments using their names in back-ticks, so they are
  rendered in monospace font.
* TODO how to write links to other bindings

### Check the rendering

You can avoid a bunch of PR feedback by checking the rendering of the docs. The fastest way to do
this is to run something like:

```shell
./gradlew fusiondoc && open build/docs/fusiondoc/fusion.html
```

Browser keyword-bookmarks can allow you to quickly access locally-built docs.


## Implementation

### Which language should I use?

The fundamental guideline in this project is to build in Fusion itself whenever possible, falling
back to Java only when Fusion cannot express the feature, or when a performance degradation would
affect existing code.

### Testing

Fusion language/library features are tested via Fusion code in `src/test/fusion/scripts`, using an
assertion framework provided by the `/fusion/experimental/check` module.

#### Test the Unhappy Cases

Ensure there’s test coverage for failure modes, and that the new procedure behaves as intended.

* Check arity problems if the procedure is variadic and/or intrinsic. Ensure that invocations with
  too few or too many arguments raises `arity_exn`.
* Check argument precondition violations. Ensure that `argument_exn` is raised when called with
  inappropriate types, or with `null` or void as appropriate. There’s currently no testing mechanism
  to ensure that good error messages are given in such cases, so we advise testing that manually.

### Writing `Procedure`s

* New `Procedure` subclasses should generally be nested within the Java class that mirrors the
  Fusion module where the procedure is `provide`d. For example, numeric procedures are generally
  provided by `/fusion/number` and underlying Java code should be in the
  `dev.ionfusion.fusion.FusionNumber` class.

#### Use the fixed-arity base classes

Fixed-arity procedures should extend `Procedure0`, `Procedure1`, or `Procedure2` as appropriate.
This results in a bit less code and allows for slightly more-efficient argument passing. If you’ve
got a three- or four-argument procedure in the works, feel free to add a new base class to support
it.

#### TODO Checking argument types


## Understanding the Implementation

### What can I read to understand the implementation of Fusion/Racket/Scheme?

The book I most recommend is [Lisp in Small Pieces][] by Christian Queinnec. It is essentially a
graduate-level textbook on implementation of Lisp-family languages. I studied it deeply while
building Fusion, and more recently while building a formal semantic model for Ion. The book covers a
broad scope of programming language features, exploring various implementation techniques for each.
It starts with a near trivial language and gradually builds up more and more functionality, while
also iteratively evolving the implementation approach all they way to C code generation. The Fusion
implementation is effectively the “Fast Interpretation” approach in chapter 6.

[Lisp in Small Pieces]: https://www.amazon.com/Lisp-Small-Pieces-Christian-Queinnec/dp/0521545668

To be fair, the book is dense, and sometimes forces the reader to connect the dots a bit. I found it
required attentive and thoughtful reading, but I was rewarded with a deep understanding of the
problem space.

The book is also a good repository of high-quality, fairly complex Scheme code.
