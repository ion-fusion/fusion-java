<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Building Ion Fusion

While we work toward a public distro, you'll need to build from source to run Fusion. 
This should be straightforward:

    git clone https://github.com/ion-fusion/fusion-java.git
    cd fusion-java
    ./gradlew release

After a successful release build, you'll have a basic SDK under `build/install/fusion`. The notable
artifacts within that directory are:

* `bin/fusion` is the `fusion` CLI
* `docs/fusiondoc/fusion.html` is the documentation for the Ion Fusion language
* `docs/javadoc/index.html` is the documentation embedding Ion Fusion in your Java application
* `lib` holds the jars needed for embedding

To experiment with the CLI, add the `bin` to your path:

    PATH=$PATH:$PWD/build/install/fusion/bin
    fusion help

That should give you an overview of the CLI's subcommands.


## What's Next?

With the `fusion` CLI ready to go, you can follow the [CLI tutorial](tutorial_cli.html) to use 
it to run code!
