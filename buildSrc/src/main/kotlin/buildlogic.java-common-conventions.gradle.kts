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

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
