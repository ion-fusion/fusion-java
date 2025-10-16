<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# About Fusion Security Mechanisms

Fusion has two primary security mechanisms for constraining the capabilities of evaluated code.
First and foremost, it allows the evaluation invoker to control the set of visible bindings, which
constrains what language and library features can be accessed from a block of code. Second, every
thread of evaluation occurs within the context of a Security Guard controlling access to critical
resources. Together, these provide both static and dynamic controls over untrusted code.


## Background

At a high level, we use the term *evaluation* to mean the act of running Fusion code. That term
encompasses three finer-grained parts:

* _Expansion_ is the process of taking some Fusion source code and transforming all uses of macros
  into intrinsic syntax forms.
* _Compilation_ is the transformation of fully-expanded abstract syntax trees into some executable form
  like bytecode.
* _Execution_ is the invocation of an executable form given some inputs and other context.

Evaluation is often "reentrant" in the sense that the parts above can invoke each other or the
entire evaluation process. For example, evaluating the source code `(eval (quote (+ 1 2)))`
starts by macro-expanding that expression, compiling it, and executing it; during execution,
applying the `eval` procedure triggers recursive evaluation of the source code `(+ 1 2)`. Further,
macro expansion involves execution of the macro transformers introduced via `define_syntax`.

The Fusion runtime evaluates code provided to it by an *invoker*. The invoker is often outside the
language, using the Java embedding APIs or the `fusion` CLI, but it can also be inside the language,
using the `eval` procedure and similar features.


## Binding Control

Each unit of code is evaluated within the context of a *namespace*, which provides a set of
*bindings* available to the code. Each binding maps a local name to its meaning within that
namespace. Fundamental syntax forms like `require` allow bindings to be renamed, remapped, or even
excluded within a namespace.

When evaluating code, the invoker supplies the namespace in which to expand and compile it.
Instantiation of a namespace involves populating its initial set of bindings, which we refer to as
the namespace's *dialect* since it determines the syntax and semantics of code evaluated within it.

Fusion has no hard-coded keywords: **all** syntax forms and procedures are accessed via bindings,
whether they are intrinsic to the runtime, provided by a library, or defined by local code. Even the
most fundamental forms like `lambda` can therefore be renamed, remapped, or excluded. The bindings
in a namespace therefore determine the features accessible to code compiled within it.

Simplifying a bit, if a namespace doesn’t included bindings for, say, arithmetic operations, then
code in that namespace cannot do math. More to the point: if a namespace does not include a binding
for `require`, Fusion’s core import declaration, then code in that namespace cannot import new
bindings from other modules and is statically, compile-time limited to only the language features
already in the namespace.

The simplification here is that we are assuming that the invoker has not provided *indirect* access
to those features via some other syntax form or procedure in the namespace. It is the invoker's
responsibility to constrain the dialect's set of bindings, generally by allow-listing a set of
carefully curated bindings appropriate for the use case.

The mechanism for doing this is straightforward. When a namespace is created, the invoker provides
the identity of a single module representing the desired dialect. The new namespace is initialized
with exactly the set of bindings exported by that module. The invoker can then add more bindings as
necessary before evaluating code. The same thing happens when declaring a module. Every module
declares it’s dialect explicitly: in the canonical prologue `(module M "/fusion" ...)` the new
module `M` declares its dialect to be the `/fusion` module. That dialect is generally the default
for non-module scripts and ad-hoc evaluation from a programmatic invoker, but all such facilities
allow explicit dialect control.

The invoker’s ability to restrict, control, and customize access to syntactic forms and library
procedures—collectively, all named entities comprising Fusion code—provides strong, static
guardrails around the capabilities available to untrusted code.

Again, the caveat to this control is that the dialect designer must be careful with what features
they include in the namespace. The next section provides some highlights.

### Dialect Design Considerations

TODO

* syntax object manipulation
* eval, load
* namespace creation?
* file system stuff
* ffi

## Guarded Sandboxes

Whereas dialects constrain Fusion code at expansion-time, sandboxes apply _execution_-time
constraints. A _sandbox_ is a namespace coupled with additional guards around any Fusion code
evaluated within it. At present, sandboxed evaluation is blocked from accessing the file system and
network. They are not, however, blocked from loading modules from the runtime's preconfigured
repositories, so access to the standard libraries (or other deployed libraries) is not impeded. On
the other hand, modules instantiated from within a sandbox are isolated to it, and their state
(and compiled code) is kept separate from other parts of the runtime.

These are minimalist implementations of Racket’s [Security Guards][] and [Sandboxed Evaluation][]
features, a design which allows significant room for growth. In the future, sandboxes can be
extended to apply filters to the accessible modules, for example to limit `require` forms to access
only allow-listed modules or module hierarchies.

Currently accessible only via the Java embedding APIs, sandboxed evaluation is invoked via instances
of the [`TopLevel`][TopLevel] interface produced by a `SandboxBuilder`. In a typical use case, this
embedding code:

```java
TopLevel top = myFusionRuntime.makeTopLevel();
```

is replaced with this:

```java
SandboxBuilder b = myFusionRuntime.makeSandboxBuilder();
b.setLanguage("/fusion");
TopLevel top = b.build();
```

> [!WARNING]
> The runtime currently has no guards around module loading, which would be needed to block access
> to the [FFI][] and to private modules in the standard library or other repositories.

[Sandboxed Evaluation]: https://docs.racket-lang.org/reference/Sandboxed_Evaluation.html
[Security Guards]:      https://docs.racket-lang.org/reference/securityguards.html

[FFI]:      https://docs.ion-fusion.dev/latest/fusion/ffi.html
[TopLevel]: https://docs.ion-fusion.dev/latest/javadoc/dev/ionfusion/fusion/TopLevel.html
