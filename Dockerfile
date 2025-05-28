# syntax=docker/dockerfile:1.16-labs

# This is the official Ion Fusion container file (aka Dockerfile).
#
# You can build an Ion Fusion runtime image with:
#
#     docker build -t fusion .
#
# Then you can run that with:
#
#     docker run --rm -it fusion repl
#
# You can also build an Ion Fusion SDK image with:
#
#     docker build -t fusion-sdk --target sdk .
#
# For more details, see: ./fusion/src/howto_build.md

ARG BASE="corretto-8"
# Available BASE values:
# corretto-8 temurin-8 zulu-8 alpine-openjdk-8 ubuntu-openjdk-8 rhel-openjdk-8

ARG BASE_JDK="base-jdk-${BASE}"
ARG BASE_JRE="base-jre-${BASE}"


# Base images
# -----------

# Amazon Corretto OpenJDK: https://hub.docker.com/_/amazoncorretto
FROM amazoncorretto:8-al2-native-jdk@sha256:04aaf328807dfea094048b98b6cc8fcb248017c044082460248cb165c8c8dcf9 AS base-jdk-corretto-8

FROM amazoncorretto:8-al2-native-jre@sha256:3b9d585c1cc14d88589bee02fff1e52bf032e3af8545166fee074b6f61ad1723 AS base-jre-corretto-8

# Eclipse Temurin OpenJDK: https://hub.docker.com/_/eclipse-temurin
FROM eclipse-temurin:8-jdk@sha256:26eef5df6131e5da7d556f1bd62fa118571ff00c0eac6d76ae30f5c7e7ce8b49 AS base-jdk-temurin-8

FROM eclipse-temurin:8-jre@sha256:eb4cc550df86a3534356839ca37c5894e8dae83b29f84d7ca0684898d4057b2d AS base-jre-temurin-8

# Azul Zulu OpenJDK: https://hub.docker.com/r/azul/zulu-openjdk
FROM azul/zulu-openjdk:8@sha256:92f73f035d60fc0053570130c3bceba9926fb4deeecb227b45e839bd14fc0265 AS base-jdk-zulu-8

FROM azul/zulu-openjdk:8-jre@sha256:dbfdbd36db44b29f3b9580bec87cd41fd6962962299f3a601a314478a2f03663 AS base-jre-zulu-8

# Alpine Linux: https://hub.docker.com/_/alpine
FROM alpine:latest@sha256:8a1f59ffb675680d47db6337b49d22281a139e9d709335b492be023728e11715 AS base-jdk-alpine-openjdk-8
RUN apk add --no-cache openjdk8-jdk

FROM alpine:latest@sha256:8a1f59ffb675680d47db6337b49d22281a139e9d709335b492be023728e11715 AS base-jre-alpine-openjdk-8
RUN apk add --no-cache openjdk8-jre

# Ubuntu Linux: https://hub.docker.com/_/ubuntu
FROM ubuntu:latest@sha256:b59d21599a2b151e23eea5f6602f4af4d7d31c4e236d22bf0b62b86d2e386b8f AS base-jdk-ubuntu-openjdk-8
RUN apt-get update && \
    apt-get --assume-yes --no-install-recommends install openjdk-8-jdk-headless

FROM ubuntu:latest@sha256:b59d21599a2b151e23eea5f6602f4af4d7d31c4e236d22bf0b62b86d2e386b8f AS base-jre-ubuntu-openjdk-8
RUN apt-get update && \
    apt-get --assume-yes --no-install-recommends install openjdk-8-jre-headless

# Red Hat Enterprise Linux 8: https://hub.docker.com/r/redhat/ubi8
FROM redhat/ubi8:latest@sha256:0c1757c4526cfd7fdfedc54fadf4940e7f453201de65c0fefd454f3dde117273 AS base-jdk-rhel-openjdk-8
RUN dnf --assumeyes install java-1.8.0-openjdk-devel

FROM redhat/ubi8:latest@sha256:0c1757c4526cfd7fdfedc54fadf4940e7f453201de65c0fefd454f3dde117273 AS base-jre-rhel-openjdk-8
RUN dnf --assumeyes install java-1.8.0-openjdk-headless


# Ion Fusion build image
# ----------------------

FROM ${BASE_JDK} AS build
WORKDIR /opt/fusion
# install gradle via the wrapper:
COPY --parents ./gradle ./gradlew ./build.gradle.kts ./settings.gradle.kts .
RUN ./gradlew --version
# run the gradle build, then clean up build dependencies:
COPY . .
RUN ./gradlew --no-daemon --console=plain --stacktrace release && \
    rm -rf ~/.gradle


# Ion Fusion SDK image
# --------------------

FROM ${BASE_JDK} AS sdk
COPY --from=build /opt/fusion/build/install/fusion /opt/fusion
ENV PATH="/opt/fusion/bin:$PATH"
ENTRYPOINT ["fusion"]


# Ion Fusion runtime image
# ------------------------

FROM ${BASE_JRE}
COPY --from=build /opt/fusion/build/install/fusion/bin /opt/fusion/bin
COPY --from=build /opt/fusion/build/install/fusion/lib /opt/fusion/lib
ENV PATH="/opt/fusion/bin:$PATH"
ENTRYPOINT ["fusion"]

