# Creating DSLs with Fusion

Fusion is a general-purpose programming language with an emphasis on Ion data processing, and with
first-class features for creating Ion-oriented Domain-Specific Languages.
Fusion’s DSL support goes very deep, making it more like a family of related languages, its
*dialects*, that can interoperate smoothly atop a common runtime.

While the standard Fusion dialect is Turing-complete, it supports defining DSLs that are
non-Turing-complete subsets of that language, or that are composed of entirely bespoke syntax and
operators.
For example, Fusion DSLs will usually exclude the standard operator `require`, eliminating the
capability (of code in that DSL) to import arbitrary Fusion modules, so that the sandboxed code
cannot import, say, Fusion’s I/O module and start reading files or writing to the console.
This is in stark contrast to, say, Ruby DSLs that are really just libraries but that don't alter,
much less restrict, Ruby's syntax or capabilities.

## Key Concepts

Fusion uses the term *dialect* to refer to the features that can be accessed by a unit of code.
The most important such features are syntax forms, since you can’t write code without syntax.
Along with syntax, most dialects include the procedures, types, constants, *etc*. that the code
needs in order to do anything productive.

The dialect’s selection of features constrains the capabilities of the code.
For example, the dialect may (or may not) provide the capability to define procedures, to write to
an output stream, or to import additional features from library modules.
That last capability is critical: by omitting or controlling import features, a dialect can
eliminate or constrain access to the Fusion standard library, preventing code from accessing
otherwise-standard capabilities that the dialect doesn’t explicitly grant.

TODO dialect != runtime, not a runtime entity

Fusion developers can create dialects that are used as Domain-Specific Languages, built to purpose
for writing software that’s highly tuned for a particular domain, and that is prevented from
“escaping” into more general-purpose capabilities.
To some degree, it’s reasonable to think of a Fusion DSL as a lightweight, language-level container
mechanism.

From a technical perspective, a dialect is simply the set of bindings that are imported into a
namespace before evaluating code in it.
In most cases, the creator of a dialect defines it in the form of a module, and the user of the
dialect identifies that module when creating a top-level namespace or writing another module.
The former case uses the Java embedding API `FusionRuntime.makeTopLevel(String initialModulePath)`.
In the latter case, the dialect is identified at the start of the module definition, which takes the
overall form `(module *name dialect expressions* `` ...)` where *`dialect`* is a module identifier.

The “standard” Fusion language is such a module, named `/fusion`.
There's nothing special about this module except that its identity is the default value for
evaluation contexts where the desired dialect isn't explicit.
In fact, `/fusion` is itself written a lower-level dialect, and that's true for two or three layers
before hitting a very small bootstrap dialect that's implemented by the runtime system.
Each layer of this tower adds capabilities for the layers above to use, building up to the full
capabilities provided by `/fusion` and the standard library.

TODO Need a term for code that doesn’t declare its dialect.
TODO restricting imports, module path remapping, etc.

## A Trivial Example

* Adhoc calculator DSL in a TopLevel
* Modular DSL in a script
* Modular DSL in a module

## Use Cases

Evaluation context

* [TRUSTED] I want to run static, trusted code written or reviewed by my team, that’s considered
  part of my application.
* [STATIC-UNTRUSTED] I want to evaluate code provided by a customer at build- or deploy-time.
* [DYNAMIC-UNTRUSTED] I want to evaluate code provided by a customer at runtime.
    * Example: an online tool evaluating user-posted expressions.

## Limitations

* Cannot yet use non-Ion surface syntax, ie, custom lexer or parser.

## References

* [Martin Fowler’s DSL Guide](https://www.martinfowler.com/dsl.html)
* [Martin Fowler's DSL Patterns](https://www.martinfowler.com/dslCatalog/)

## Snippets

Fusion is a meta-language: it provides facilities for controlling what functions and syntax forms
you make available
to an evaluation context, and even for altering and replacing existing features.
