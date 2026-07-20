<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Building Ion Fusion

> **Prerequisites:**
>
> * Install a Java runtime, version 17 or later. We recommend [Amazon Corretto][].
>   * This project builds for Java 8, but its Gradle tooling requires 17.
> * Install [Git][].
> * Ensure that `java` and `git` are on your shell's `PATH`.

Building from source should be straightforward:

    git clone https://github.com/ion-fusion/fusion-java.git
    cd fusion-java
    ./gradlew build

After a successful build, you'll have a basic SDK under `sdk/build/install/ion-fusion-sdk`.
The notable artifacts within that directory are:

* `bin/fusion` is the `fusion` CLI
* `docs/fusion.html` is the documentation for the Ion Fusion language
* `docs/javadoc/index.html` is the documentation embedding Ion Fusion in your Java application
* `lib` holds the jars needed for embedding

To experiment with the CLI, add the `bin` to your path:

    PATH=$PATH:$PWD/sdk/build/install/ion-fusion-sdk/bin
    fusion help

That should give you an overview of the CLI's subcommands.


## About JAVA_HOME

In general, we recommend that you avoid setting `JAVA_HOME` in your shell while working
with this project (or, frankly, any time). Since different projects have different JDK
requirements, it's quite likely that any setting will be wrong _somewhere_.

This project compiles with and runs on a relatively old JDK version, while requiring a
relatively new JDK version for its Gradle build tooling.
Gradle's startup process will use the JDK from `JAVA_HOME` if that is set, otherwise
the `java` command on your `PATH`.  We recommend that the `java` on `PATH` be as new
as possible.

In some contexts, `JAVA_HOME` and/or the `java` on `PATH` might be injected by your
tools.
For example, recent releases of IntelliJ IDEs will do this by default for its built-in
terminal, setting `JAVA_HOME` to the project's runtime JDK, not the one required to
build.
You should disable this in **Settings | Tools | Terminal | Add project JDK to `PATH`**
to avoid interference.


## What's Next?

With the `fusion` CLI ready to go, you can follow the [CLI tutorial](tutorial_cli.html) and run
some code!


[Amazon Corretto]: https://aws.amazon.com/corretto
[Git]: https://git-scm.com/
