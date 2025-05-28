<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Building Ion Fusion

This guide shows how to get a working copy of the Ion Fusion source code, and
how to build the Ion Fusion software development kit (SDK) and container images.

- [Getting the source code](#getting-the-source-code)
- [Building the SDK with Gradle](#building-the-sdk-with-gradle)
- [Building the container images](#building-the-container-images)


## Getting the source code

Assuming [Git][] is installed, run:

    git clone https://github.com/ion-fusion/fusion-java.git
    cd fusion-java

Alternatively, you can download an archive of the repository's code from:
<https://github.com/ion-fusion/fusion-java/archive/refs/heads/main.zip>

[Git]: https://git-scm.com/


## Building the SDK with Gradle

With a Java 8 runtime installed, you can build the Ion Fusion SDK natively
from the source directory, with:

    ./gradlew release

After a successful release build, you'll have a basic Ion Fusion SDK under
`build/install/fusion`, encompassing:

* `bin/fusion`, the Ion Fusion command-line interface (CLI) utility.
* `docs/fusiondoc/fusion.html`, the documentation for the Ion Fusion language.
* `docs/javadoc/index.html`, the documentation for the Ion Fusion library
  interface, for integrating Ion Fusion into your application.
* `lib/`, with the Ion Fusion JAR files required for integration.

To experiment with the CLI, you can add the `bin` directory to your PATH:

    PATH=$PATH:$PWD/build/install/fusion/bin
    fusion help

That will give you an overview of the CLI's subcommands.


## Building the container images

With a container CLI installed (such as [docker][], [podman][], [nerdctl][],
[finch][] or [container][]) capable of building `linux/amd64` or `linux/arm64`
containers, you can build the Ion Fusion container images from the source
directory. Per your preferences, you can do this instead of, or as well as,
building Ion Fusion via Gradle as described above.

[docker]: https://www.docker.com/products/cli/
[podman]: https://podman.io/
[nerdctl]: https://github.com/containerd/nerdctl
[finch]: https://github.com/runfinch/finch
[container]: https://github.com/apple/container

### Runtime image

The Ion Fusion runtime image is ideal for using the `fusion` CLI, and for
runtime application deployments; it is based atop an OpenJDK Java Runtime
Environment (JRE) installation, *not* a complete OpenJDK distribution.

You can build the Ion Fusion runtime image with:

    docker build -t fusion .

You can then run the Ion Fusion runtime image with:

    docker run --rm fusion help

Thus, an alternative way to "install" the `fusion` CLI is simply:

    alias fusion='docker run --rm fusion`

You can alternatively run a shell from the Ion Fusion runtime image with:

    docker run --rm -it --entrypoint sh fusion

### SDK image

The Ion Fusion SDK image is ideal for development and build purposes; it is
based atop a complete OpenJDK installation.

You can build the Ion Fusion SDK image with:

    docker build -t fusion-sdk --target sdk .

The SDK image supports building applications that integrate Ion Fusion:

    docker run --rm --entrypoint sh fusion-sdk -c '
    echo "public class Example {
        public static void main(String[] args) {
            dev.ionfusion.fusion.cli.Cli.main(args);
            System.out.println(\"Hello, Fusion integration!\");
        }
    }" >Example.java
    cp="$(echo /opt/fusion/lib/*.jar | tr " " :)"
    javac -cp "$cp" Example.java
    java -cp ".:$cp" Example version'

### Varying the OpenJDK

By default, the Ion Fusion container images are built atop the official
[Amazon Corretto][] container images.

You can alternatively build the Ion Fusion container images atop other OpenJDK
distributions, with, for example:

    docker build -t fusion --build-arg BASE=alpine-openjdk-8

The Ion Fusion `Dockerfile` supports a range of possible `BASE` build arguments
for controlling the underlying OpenJDK distrubution:

- [Amazon Corretto][]: `corretto-8`
- [Eclipse Temurin](https://hub.docker.com/_/eclipse-temurin): `temurin-8`
- [Azul Zulu](https://hub.docker.com/r/azul/zulu-openjdk): `zulu-8`
- [Alpinux Linux](https://hub.docker.com/_/alpine): `alpine-openjdk-8`
- [Ubuntu Linux](https://hub.docker.com/_/ubuntu): `ubuntu-openjdk-8`
- [Red Hat Enterprise Linux 8](https://hub.docker.com/r/redhat/ubi8):
  `rhel-openjdk-8`

You can even build the Ion Fusion container images atop entirely custom JDK and
JRE base images, like:

    docker build -t fusion \
        --build-arg BASE_JDK=amazoncorretto:8-alpine-jdk \
        --build-arg BASE_JRE=amazoncorretto:8-alpine-jre \
        .

For more details on how you can control the Ion Fusion container image build
process, see the [Dockerfile](../../Dockerfile) source code.

[Amazon Corretto]: https://hub.docker.com/_/amazoncorretto


## What's Next?

With the `fusion` CLI ready to go, you can follow the
[CLI tutorial](tutorial_cli.html) and run some code!
