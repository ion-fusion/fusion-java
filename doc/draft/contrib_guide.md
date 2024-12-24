# Fusion Contributor’s Guide

Want to contribute new features to the Ion Fusion language or ecosystem? Here’s how.

> This document is very much work-in-progress. Please ask questions to help shape its content!

# Overview

## How do I get started?

First, sign up for notifications on this repository. Reading PRs is a good way to get a feel for the
internals. Ask questions! We’re happy to explain what’s going on and why.

## What can I contribute?

Contributions to the core language and libraries are welcome and encouraged! Please be aware that
there is a high bar in terms of design, terminology, consistency, style, documentation, and testing.
Not everything will "fit" into the core, and most features can reside elsewhere without problems.

Documentation is the easiest thing to contribute, and offers a good route to getting to know the
code base and the language’s library and internals.

Standard-library procedures are usually straightforward, especially those that can be implemented in
Fusion with reasonable efficiency. If Java code is required, it’s somewhat more complicated (and
poorly-documented at present).

New syntactic forms are harder to accomplish, since they require understanding Fusion’s macro system
pretty thoroughly.

## Which language should I use?

## What’s the process?

1. Raise the topic via a GitHub issue. This is a quick way to ensure that there’s not already a
   reasonable solution, that the feature fits within the scope and aims of the language and library,
   and to gather general feedback on the idea.
2. Get a design review. Draft documentation for the proposed feature and post it as a PR. In most
   cases, this should basically be the documentation for the procedure you’re adding. This allows
   experts to ensure the proposed behavior is well-designed and well-defined, to suggest test cases,
   and so on, before you invest time in implementation.
    * This step isn’t required, but recommended in order to use your time wisely! We all feel bad
      if your efforts go down the wrong path. Writing docs
      first is a lightweight way for you to get early feedback. It's best to get the high-level
      details right -- particularly the terminology and contract -- before digging into details.
3. Code it! Don’t forget the unit tests.
4. Post a pull review, and iterate as necessary. **Review early, review often!**
5. Your PR will be merged by a team member, and in most cases the change will be part of the next
   release.
    * Note that Fusion releases go through a number of testing rounds, so your feature won’t be
      available immediately. It may also need to wait for other in-flight changes to be completed.

# Design

## Follow Racket

Fusion’s overall architecture is based on Racket, and most of its features are intentionally aligned
with Racket in both name and behavior. Since Fusion is still very small, most of the features that
users want are already available in Racket, and so contributors should look to Racket for design
guidance and attempt to align with that prior art as much as possible.

## Selecting the right module(s)

# Documentation

We recommend as best practice (not just in Fusion but in any software development) that you draft
the documentation for the new features before you start writing code. This is a Working Backwards
technique, ensuring that you can explain things clearly to your future customers before you embark
on building things. If its awkward to explain, its probably going to be awkward to use.

Especially if you’re a newcomer to designing Fusion features, it’s a good idea to get a review of
your documentation *first*, to get feedback on what you’re planning to build before you invest a lot
of effort in the wrong direction. This can be as simple as adding a method stub into an existing
Fusion library module that does nothing but includes the proposed documentation.

## Syntax

Markdown, basically.

## Style

* In prose paragraphs, refer to procedure arguments using their names in back-ticks, so they are
  rendered in monospace font.
* TODO how to write links to other bindings

## Check the rendering

You can avoid a bunch of PR feedback by checking the rendering of the docs. The fastest way to do
this is to run something like:

```shell
./gradlew fusiondoc && open build/docs/fusiondoc/fusion.html
```

Browser keyword-bookmarks can allow you to quickly access locally-built docs.

# Implementation

## Testing

### Test the Unhappy Cases

Ensure there’s test coverage for failure modes, and that the new procedure behaves as intended.

* Check arity problems if the procedure is variadic and/or intrinsic. Ensure that invocations with too
  few or too many arguments raises `arity_exn`.
* Check argument precondition violations. Ensure that `argument_exn` is raised when called
  with inappropriate types, or with `null` or void as appropriate. There’s currently no testing
  mechanism to ensure that good error messages are given in such cases, so its advisable to test
  that manually.

## Writing `Procedures`

* New `Procedure` subclasses should generally be nested within the Java class that mirrors the
  Fusion module where the procedure is `provide`d. For example, numeric procedures are generally
  provided by `/fusion/number` and underlying Java code should be in the
  `com.amazon.fusion.FusionNumber` class.

### Use the fixed-arity base classes

Fixed-arity procedures should extend `Procedure0`, `Procedure1`, or `Procedure2` as appropriate.
This results in a bit less code and allows for slightly more-efficient argument passing. If you’ve
got a three- or four-argument procedure in the works, feel free to add a new base class to support
it.

### Checking argument types

# Understanding the Implementation

## What can I read to understand the implementation of Fusion/Racket/Scheme?

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
