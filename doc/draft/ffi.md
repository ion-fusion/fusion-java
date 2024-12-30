# fusion-java FFI

The Foreign Function Interface is a mechanism for calling Java code from Fusion code. This page
documents the current unsupported/unstable FFI.

* * *

# **ENORMOUS WARNINGS THAT YOU IGNORE AT YOUR PERIL**

**WARNING**: The FFI is **UNSUPPORTED**, **UNSTABLE**, and generally **UNDOCUMENTED**!

## **UNSUPPORTED**

The FFI is unsupported. Your use of internal features does not imply any commitment on our part. Use
responsibly and plan accordingly.
If changes to internal features cause your package to fail to build in live, you should expect your
package to be pruned. We always do dry-run builds in live, and we'll try to give you warning if this
is going to happen. But we cannot make promises, and we cannot hold off on a build waiting for you
to patch your code.

## **UNSTABLE**

Use of the FFI require use of internal features (classes, methods, constants, etc.) of FusionJava,
and you'll have to write code in the `dev.ionfusion.fusion` package to gain access to them.
*All* such features must be considered unstable: you should *assume* that they *will* change in the
future. The runtime implementation *will* change (most notably in order to generate JVM bytecode for
improved performance) and the APIs will change accordingly. Until the runtime internals are stable,
the FFI will remain unstable.
We will make a best-effort attempt to buffer your code from changes. When APIs are replaced we'll
generally deprecate the old API and retain it for at least one release. We'll announce such changes
to fusion-dev@.
You must make your own best-effort to migrate promptly away from deprecated features. Failure to do
so may lead to your package being pruned from live.
That said: the same FFI features that you'll use are those that are used by the core implementation,
and therefore it's unlikely that there will be massive changes in a short period of time. It's much
more likely that there will be a small number of changes within each release. In other words, be
prepared for frequent small migrations.

## **UNDOCUMENTED**

The FusionJava documentation covers the stable, public features of the library. Anything that's not
part of that documentation is considered to be an internal API and is subject to change. You're
encouraged to read the internal documentation comments and to ask questions on fusion-dev@ where
things are unclear. That way we can get the docs improved (since they'll eventually be made public).

# **Getting Started**

The FFI consists of exactly one capability: installing a Fusion procedure that's implemented in
Java. There's two parts to that:

1. Write a class that extends `dev.ionfusion.fusion.Procedure`.
2. Bind an instance of that class to a name in a Fusion module.

Your best learning resource in this process is the FusionJava code itself. There are dozens of
built-in procedures that follow this exact process. However, be sure to observe the constraints
below, since your code should only use a more restricted set of features than what's used by the
core.
A reasonable place to explore is the Fusion code under
`/fusion/private`,
particularly
`/fusion/private/sexp`.
You'll see a bunch of Java class names and can then look at the corresponding Java code.

## **Implementing a Procedure**

In the vast majority of cases, you will write a `Procedure` class and then instantiate that class
exactly once. Your class will need to be in the `dev.ionfusion.fusion` Java package in order to have
access to the internal APIs.
Within your Procedure, you'll implement the `doApply` method.

### **Be Minimal**

Procedure implementations should be as minimal as they can be. Do as much as possible in Fusion. In
many cases your Java procedure can be private, wrapped by a public Fusion procedure that does work
before and after the Java is invoked. The more work that's done in Java, the more likely you're
going to have to modify or re-implement the procedure as the FFI or internal APIs evolve and
stabilize.

### **Be a Leaf**

The Fusion language relies heavily on [tail-call optimization][TCO], and that process cannot occur
across an FFI stack frame. Therefore, try not to call other Fusion procedures from your `Procedure`.
If you do, strive to make a tail-recursive call, using `Evaluator.bounceTailCall()`. If you need to
do more work in Java afterwards, put that logic in a second FFI procedure.

[TCO]: https://en.wikipedia.org/wiki/Tail_call

### **Arity Checking**

Unless you're writing an explicitly unsafe procedure, the very first thing your `doApply` should do
is an arity check, ensuring that the number of arguments the caller has passed is acceptable to your
procedure.
If your procedure is fixed-arity of zero, one, or two arguments, you may extend `Procedure0`..
`Procedure2` which will automatically verify the number of arguments passed. Otherwise, you'll need
to do the arity check yourself by calling one of the methods `checkArityExact`, `checkArityAtLeast`,
or `checkArityRange`.

### **Type-checking Arguments**

Most of the type-oriented utility classes (like `FusionList`) have helpers for checking argument
types (like `checkActualListArg()`). There are usually variants for nullable arguments versus "
actual" (non-null, non-void) arguments. Unfortunately the names and semantics here may not be
consistent at this point in time, so check the source (and cut issues where you find inconsistency
so it can be corrected).
These methods generally take the (zero-based) position of the argument to inspect, along with the
complete sequence of arguments. If the noted argument doesn't match the requested type, an
`ArgumentException` is thrown. The message will include all the argument values, which is very
helpful for debugging.

### **Don't touch the implementation of Fusion values**

You must assume that the implementation of any built-in type will change without warning, so don't
refer to their types. In particular, the entire `BaseValue` hierarchy is off limits. For this
reason, you must only use `Object` as the type of any Fusion value. To manipulate those values, use
the relevant static helper methods on classes of the form Fusion*TYPE*: `FusionValue`,
`FusionString`, `FusionList`, and so on. These are the same classes that provide similar helpers for
code outside the FFI, so they should be easy to spot. As a general rule, those methods are:

* package-protected
* static
* Require an `Evaluator` as the first argument
* Accept `Object` for Fusion values, often with preconditions of specific Fusion types
* Accept some Java types like `String` that will be auto-injected for convenience

Methods of any other form have a much higher likelihood of changing.
As always, **check the JavaDocs before using something!** Many things are labeled "Not for
application use" and that applies to the FFI as well. When in doubt, please ask on fusion-dev.

### **Be careful with exceptions**

The Fusion implementation assumes that all in-language exceptions (technically, any value that has
been `raise`d, which can include things that are not Fusion `exn` subtypes), are instances of
`FusionException` and that anything else is a fatal error/crash of the runtime. While evaluating
Fusion code, anything that is not a `FusionException` propagates all the way out of the `TopLevel`
entry point. There are some limited cases like `InterruptException` that are handled, but that's it.
This generally mirrors the behavior of Java passing-through arbitrary unchecked `Throwable`s.
It's therefore imperative that every `Procedure` must handle exceptions carefully! Unless you want
the entire evaluation to crash, you gotta wrap in `FusionException` or better yet translate to the
appropriate subtype of `exn`. The best way to accomplish this today is by bouncing a tail-call to
`raise_contract_exn` or similar.

### **Don't re-enter the FusionRuntime or TopLevel**

The evaluation engine is not re-entrant: any code called by a procedure **must not** use a
`FusionRuntime` or `TopLevel`, even the one that was originally invoked by the current thread. The
*only* correct way to return to Fusion code is by invoking another procedure using the `Evaluator`
argument, returning from the procedure, or throwing a FusionException.
This is a very deep assumption of the runtime, and failure to follow this rule will lead to any
number of problems. Sandbox constraints and security guards will be bypassed. Exceptions will not be
handled properly. Any stack traces issued by the evaluator will be incorrect. Thread interrupts will
not work properly. Important thread-local context variables may become corrupted.
Seriously, don't do this. It is highly likely that the engine will at some point take steps to
prevent re-entrant calls.
If you find yourself in a situation where your Java FFI code needs to call back into Fusion, the
recommended approach is to break up the procedures using continuation-passing style, coordinated
from Fusion code that invokes a sequence of Java procedures. Contact fusion-dev@ for assistance.

## **Using the Evaluator**

**Don't** store an `Evaluator` in a member for use later. You *must* consider the `Evaluator`
instance to be transient and *only* valid for the lifetime of the call in which it was received. The
`Evaluator` encapsulates the continuation (call stack) and important dynamic context, and its misuse
can have severe implications up to and including security vulnerabilities, crashes, and data
corruption. Think of it like the stack pointer in assembly language: you can't hold onto it and
expect it to be correct later.
You **do** need to pass the `Evaluator` around, pretty much everywhere. If you're looking at a
method that doesn't take an `Evaluator` as its first argument, then that method is probably not
intended for your use. Don't pass null where an `Evaluator` is accepted.
