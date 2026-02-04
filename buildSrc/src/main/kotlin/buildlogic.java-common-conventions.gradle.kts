// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

// Conventions for Java libraries and applications.

version = project.properties.get("projectVersion")!!

plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    // https://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle-bom
    testImplementation(platform("org.junit:junit-bom:5.14.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.hamcrest:hamcrest:3.0")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        // Keep in sync with `.readthedocs.yaml`
        languageVersion = JavaLanguageVersion.of(8)
    }
}

// Enable some linting.  TODO Work toward -Xlint:all
tasks.withType<JavaCompile>() {
    options.compilerArgumentProviders.add {
        listOf("-Xlint:serial")
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

// Temporary alias until we migrate tooling
// TODO https://github.com/ion-fusion/fusion-java/issues/429
tasks.register("release") {
    dependsOn(tasks.build)  // build depends on assemble & check
}
