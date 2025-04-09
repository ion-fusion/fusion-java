<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<!-- MarkdownJ can't handle backticks in headers -->
# About the <code>fusion</code> CLI

The `fusion` command-line interface is intended to support evaluation of Fusion code across a 
variety of use cases.  It provides a number of commands that can be composed in a sequence of 
steps within a single execution of the CLI.

> ⚠️ This document is a work in progress! We'd love to hear your feedback in 
> [this Discussion thread](https://github.com/orgs/ion-fusion/discussions/213).

The `fusion` executable requires at least one argument: a command to perform.

    % fusion
    No command given.
    Type 'fusion help' for more information.

To see the possible commands, use the `help` command:

    % fusion help
    
    Usage: fusion [OPTIONS ...] <command> [ARGS ...] [; <command> [ARGS ...]] ...

    Type 'fusion help <command>' for help on a specific command.
    
    Available commands:
      repl             Enter the interactive Read-Eval-Print Loop.
      load             Load and evaluate a script.
      eval             Evaluate an inline script.
      require          Import bindings from a Fusion module.
      report_coverage  Generate a code coverage report.
      -----            -------------------------
      help             Describe the usage of this program or its commands.
      version          Writes version information about this program.

...and so on.


## Passing Options

As shown above, the CLI follows the command/subcommand pattern common in modern projects. To tune
behavior, it supports options following both the `fusion` executable name (so-called "global
options"), affecting the entire execution, and following an individual command, affecting just that
segment.

Options start with `"--"`, and come in three forms:

    --key=value
    --key value
    --key
 
The first two forms are used for all non-boolean option types. The third form is used for boolean 
options, which are always set to `true` when present on the command line.

When using the first form above, the entire form must be a single word (from the perspective of your
shell), without whitespace around the `"="`.

No sanity checking is performed on the value, so if you type...

    --key = value

...the option named `"key"` will get the value `"="`.


## Running a Script

The most common use of the CLI is to evaluate a script on the local file system. The command 
for this is `load`, mirroring [the Fusion procedure of the same name](fusion/eval.html#load). 
Suppose you have a script in the current directory called `script.fusion`:

    % cat script.fusion
    (define (add1 num)
      (+ 1 num))
    (add1 252)

You can run this script with the following command:

    % fusion load script.fusion
    253

> ℹ️ The script doesn't write the result seen above; that's done by the CLI-level `load` command.

Of course, you're likely to be processing some input, so the CLI ties its standard input stream
to the [_current input port_](fusion/io.html#input) used during evaluation, and you can consume
that stream as Ion data using the [`read`](fusion/io.html#read) procedure to parse one value at 
a time: 

    % cat add1.fusion
    (define (add1 num)
      (+ num 1))
    (add1 (read))

    % echo 41 | fusion load add1.fusion
    42


## Evaluating an Inline Expression

[TODO](https://github.com/orgs/ion-fusion/discussions/213)


## Running Multiple Commands

So far, each invocation of the CLI runs a single command, evaluating a single script or program
fragment. However, in many scenarios it's helpful to evaluate several program fragments in sequence.
The CLI accommodates this by breaking its arguments into _segments_, each containing one command.

In general, an invocation of the `fusion` CLI:

* extracts any global options from the front of the command line;
* partitions the remaining words into segments separated by the `;` word;
* parses each segment into its own command, options, and arguments;
* constructs a fresh top-level namespace, bootstrapped with the [`/fusion`](fusion.html) dialect;
* executes each command in order within that namespace, [writing](fusion/io.html#output) any
  results (from each command) to standard output stream.

Because all commands share the same namespace, the side effects of each command are visible to those
that follow. For example, suppose that you want to invoke a library procedure that's not provided
by [`/fusion`](fusion.html) (the module whose exports are used to bootstrap the namespace):

    % fusion eval '(unsafe_list_add [] "new")'
    Bad syntax: unbound identifier. The symbol 'unsafe_list_add' has no binding
    where it's used, so check for correct spelling and imports.

To access that function, you need to [`require`](fusion/module.html#require) it into the namespace. 
You can accomplish this by evaluating more than one expression:

    % fusion eval '(require "/fusion/unsafe/list") (unsafe_list_add [] "new")'
    ["new"]

That can be awkward, though, so you can instead use two `eval` commands:

    % fusion eval '(require "/fusion/unsafe/list")' ';' eval '(unsafe_list_add [] "new")'
    ["new"]

> ℹ️ The quotes around the semicolon prevent it from being interpreted by your shell (like
> Bash).

To better support this pattern, the CLI has a `require` command that's easier to use from a 
shell script:

    % fusion require '/fusion/unsafe/list' ';' eval '(unsafe_list_add [] "new")'
    ["new"]

The `load` command also shares the common namespace, so you can do things before or after 
running a script. The next example redefines `+` within the namespace to perform subtraction; 
the new definition takes precedence when `+` is referenced in the definition of `add1`:

    % echo 100 | fusion eval '(define + -)' ';' load add1.fusion
    99

> ℹ️ The primary difference between `eval` and `load` is whether the source code comes from the
> command line or the file system.


## Using an Ion Catalog

One of the flagship features of [the Amazon Ion data format][Ion] is its ability to extract common
[symbols][I/symbol] into separate data structures called [_shared symbol tables_][I/SST]. To use a
shared symbol table when decoding data, it must be available to the Ion implementation in a
[_catalog_][I/catalog], which is simply an abstraction for a set of shared symbol tables indexed 
by name and [version][I/SST/vn].

To accommodate this, the Fusion CLI provides a simple, static catalog interface via the file system.
The global `--catalogs` option gives the CLI file paths that contain shared symbol tables. If a
given path is a file, it must contain (only) shared symbol tables in their
[canonical serialized form][I/SST]. When a path is a directory, it is traversed recursively and all
files are added to the catalog.

To declare multiple catalog files/directories, the `--catalogs` option can be given more than 
once, and/or one `--catalogs` option can declare multiple paths using the OS-specific path 
separator:

    $ fusion --catalogs catalog.ion --catalogs /Users/susan/.ion/catalog/ ...
    $ fusion --catalogs catalog.ion:/Users/susan/.ion/catalog/ ...


## The REPL

[TODO](https://github.com/orgs/ion-fusion/discussions/213)


[Ion]:       https://amazon-ion.github.io/ion-docs/
[I/catalog]: https://amazon-ion.github.io/ion-docs/docs/symbols.html#the-catalog
[I/symbol]:  https://amazon-ion.github.io/ion-docs/docs/spec.html#symbol
[I/SST]:     https://amazon-ion.github.io/ion-docs/docs/symbols.html#shared-symbol-tables
[I/SST/vn]:  https://amazon-ion.github.io/ion-docs/docs/symbols.html#versioning
