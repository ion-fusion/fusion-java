// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("buildlogic.fusion-common-conventions")
    id("buildlogic.java-library-conventions")
    id("buildlogic.jacoco-conventions")
}

dependencies {
    api("com.amazon.ion:ion-java:1.11.11")

    testImplementation(project(":testing"))
}

java {
    withJavadocJar()
    withSourcesJar()
}


val mainFusionRepo = layout.projectDirectory.dir("src/main/fusion")
val testFusionRepo = layout.projectDirectory.dir("src/test/fusion")


// Various resources refer to the current version label.
tasks.processResources {
    // Embed our version in the jar so the CLI can print it.
    expand("project_version" to project.version.toString())
}


// Bundle the Fusion bootstrap repository in our jar.
tasks.jar {
    // It might be better if these were modeled as resources in the main
    // sourceSet, so they are automatically added to the classpath when testing.
    // It's unclear how to change the parent directory that way.
    into("FUSION-REPO") {
        from(mainFusionRepo)
        includeEmptyDirs = true
    }
}


//=============================================================================
// Tests

tasks.test {
    // dev.ionfusion.fusion.ClassLoaderModuleRepositoryTest uses ftst-repo.jar.
    dependsOn(ftstRepo)

    inputs.dir(testFusionRepo)
}

val ftstRepo = tasks.register<Jar>("ftstRepo") {
    destinationDirectory = base.libsDirectory
    archiveFileName = "ftst-repo.jar"

    into("FUSION-REPO") {
        from(testFusionRepo)
        includeEmptyDirs = true
    }
}


//=============================================================================
// Java Code Coverage

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}


//=============================================================================
// Documentation

tasks.javadoc {
    exclude("**/_Private_*",
            "**/_private/**",
            "dev/ionfusion/fusion/util/hamt/**")

    title = "FusionJava API Reference"

    var overviewFile = "$projectDir/src/main/java/overview.html"
    inputs.file(overviewFile)

    options {
        // https://github.com/gradle/gradle/issues/7038
        this as StandardJavadocDocletOptions

        docEncoding = "UTF-8"
        overview = overviewFile

        header = "FusionJava API Reference<br />${project.version}"
        bottom = "<center>Copyright Ion Fusion contributors. All Rights Reserved.</center>"
        noTimestamp(true)
    }
}


//=============================================================================
// Distribution
