// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("buildlogic.fusion-common-conventions")
    id("buildlogic.java-library-conventions")
    jacoco
}

dependencies {
    api("com.amazon.ion:ion-java:1.11.11")

    // TODO These shouldn't be needed when consumers embed Fusion.
    //  It's a build-time dependency, but here b/c consumers use the CLI to
    //  generate their docs.  That should be handled by a plugin instead.
    implementation("org.markdownj:markdownj:0.3.0-1.0.2b4")
    implementation("com.github.spullara.mustache.java:compiler:0.9.14")

    testImplementation(project(":testing"))
}

base {
    archivesName = "ion-fusion-runtime"
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

// https://docs.gradle.org/current/userguide/jacoco_plugin.html

jacoco {
    // Do this to keep JaCoCo version stable when updating Gradle.
    // As of Gradle 8.10, the default is:
//    toolVersion = "0.8.12"
}

tasks.test {
    configure<JacocoTaskExtension> {
        // When running in IDEA, JaCoCo instruments HTMLUnit (and fails).
        // I don't know why it's instrumenting libraries, but this avoids it.
        includes = listOf("dev.ionfusion.*")
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.75".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}


//=============================================================================
// Documentation

tasks.javadoc {
    exclude("**/_Private_*",
            "**/_private/**",
            "dev/ionfusion/fusion/cli",
            "dev/ionfusion/fusion/util/hamt/**")

    title = "FusionJava API Reference"

    options {
        // https://github.com/gradle/gradle/issues/7038
        this as StandardJavadocDocletOptions

        docEncoding = "UTF-8"
        overview = "$projectDir/src/main/java/overview.html"

        header = "FusionJava API Reference<br />${project.version}"
        bottom = "<center>Copyright Ion Fusion contributors. All Rights Reserved.</center>"
        noTimestamp(true)
    }
}


//=============================================================================
// Distribution

tasks.build {
    dependsOn(tasks.jacocoTestReport)
}
