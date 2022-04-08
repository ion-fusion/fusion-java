// Copyright (c) 2022 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

/**
 * Constructs {@link TopLevel} namespaces for evaluating code with limited
 * access to resources.
 * <p>
 * By default, sandboxed evaluation is blocked from accessing the file system
 * and network.
 * </p>
 * <p>
 * Each sandbox namespace has its own module registry that only shares select
 * module instances with other namespaces.  This prevents pollution of the
 * runtime's default registry, which is particularly useful when sandboxed code
 * can create new modules (for example, by evaluating a module declaration).
 * </p>
 * <p>
 * On the other hand, this means that there can be unnecessary instances of
 * common code, even from the standard libraries, resulting in increased latency
 * (to load and instantiate them) and memory use.  More subtly, values and types
 * created within sandboxes can be incompatible with or unrecognizable to those
 * created elsewhere.  This is similar to Java's behavior with respect to
 * multiple Classloaders, where the same class can be loaded twice, leading to
 * incompatible results.
 * </p>
 * <p>
 * The defaults here align with a common case where the application creates
 * numerous transient sandboxes that all use the same language. As such, the
 * configured language is first instantiated in the runtime's default registry,
 * then copied into the sandbox's fresh registry, along with all modules it
 * depends on. This results in the language's exported features being shared
 * across all sandboxes. Any subsidiary modules will be instantiated on-demand
 * and not shared; this includes local {@code (module ...)} declarations.
 * </p>
 */
public interface SandboxBuilder
{
    /**
     * Declares the language used to bootstrap the sandbox namespace.
     * This must be provided before calling {@link #build()}.
     *
     * @param absoluteModulePath identifies the language; must not be null.
     */
    void setLanguage(String absoluteModulePath);

    // Implies the builder can't be reused
    // Perhaps let user run this code after build()?
    //   Some uses of make-evaluator use this to construct a module
//  void addInputProgram(IonReader source, SourceName name);

    // By default, exceptions during sandbox evaluation propagate to the caller.
//  void setPropagateExceptions(boolean propagate);

    // By default, or when given null, use an input stream that's at EOF.
//  void setInputPort(InputStream out);

    // By default, or when given null, use an output stream that discards data.
//  void setOutputPort(OutputStream out);


    /**
     * Builds a new sandbox based on the current configuration of this builder.
     *
     * @return a new sandbox instance.
     *
     * @throws IllegalStateException if the builder's configuration is
     * incomplete, inconsistent, or otherwise unusable.
     * @throws FusionException if there's a problem bootstrapping the sandbox.
     */
    TopLevel build()
        throws IllegalStateException, FusionException;
}


/*

Notes on Racket's sandbox features.

https://docs.racket-lang.org/reference/Sandboxed_Evaluation.html


Creating Sandboxes
==================

The core API is `make-evaluator` which has a handful of modes.

* (make-evaluator '(begin expr...) program... #:require '(mod...))

This, by default, uses make-base-namespace, which requires racket/base; there's
no enclosing module for the resulting namespace.

The bootstrap program is effectively:

  (begin expr... (require mod)... program...)

Note that definitions in both the "language" and `program` can be redefined,
since they are not module-level bindings.

Namespaces from make-base-namespace have a module registry that "contains only
mappings for some internal, predefined modules, such as '#%kernel", and has
racket/base attached (along with its transitive imports) and required into the
environment.


* (make-evaluator module-path program... #:require '(mod...))

This instantiates a fresh module and uses its namespace for the sandbox.

The bootstrap program is effectively:

    (module M module-path (require mod)... program...)

Note that definitions in `program` cannot be redefined, but bindings from the
required modules can be shadowed by `define`.


* (make-module-evaluator program #:language lang)

This is similar, but requires `program` to be a module declaration of the form
`(module lang ...)` or, I suppose, `#lang lang ...`.
The #:language argument is useful for forcing tenant modules to use your language.

From the perspective of `eval` using the namespace, a non-module namespace
allows redefinition, while a module namespace does not.


Sandbox Restrictions
====================

Each sandbox uses a new custodian, namespace, and module registry.

The default sandbox-security-guard disables file system access, except to read
precompiled and -installed modules in collection libraries. This implies access
to all the standard Racket libraries.  In Fusion terms, this would imply access
to the FusionRuntime's bootstrap and user repositories.

These defaults can be adjusted in various ways.

* `#:allow-for-require` and `#:allow-for-load` specify additional file-system
  paths that can be read via `require` and `load`.   At present, Fusion doesn't
  have in-language facilities for loading a module from a file-system path akin
  to Racket's `(require "file.rkt")` or `(require (file <PATH>))`, so the former
  is not meaningful here.
* `#:allow-for-require` supports module paths accepted by `require`,
  that is, data matching the syntax of `require`.  Since Fusion only supports
  the symbolic paths in a manner similar to Racket collection-library references,
  the other forms aren't really relevant today.
* `#:allow-for-load` support, on the other hand, would be relevant.


Protected Exports
=================

Racket modules can "protect" exported bindings to prevent access from untrusted
modules.

Ref
* https://docs.racket-lang.org/guide/code-inspectors_protect.html
* https://docs.racket-lang.org/guide/protect-out.html

The relationship between tainting and inspectors received significant updates at
Racket 8.2.0.4.
* https://docs.racket-lang.org/guide/stx-certs.html
  * Vs https://download.racket-lang.org/releases/7.3/doc/guide/stx-certs.html
* https://docs.racket-lang.org/reference/stxcerts.html

 */
