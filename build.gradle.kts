// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

version = "0.38a1-SNAPSHOT"
// Docs for how Maven compares version labels:
// https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification
// I find that nearly incomprehensible, while this blog post is more useful:
// https://octopus.com/blog/maven-versioning-explained
// Gradle's algorithm is different, and easier to understand:
// https://docs.gradle.org/8.10.2/userguide/single_versions.html#version_ordering
// https://docs.gradle.org/8.10.2/userguide/dependency_resolution.html#sec:base-version-comparison

// WARNING: replacing -SNAPSHOT with -a1 (or even -rc) gives an *earlier*
// version according to both algorithms.  Proactively including the `a1` preserves
// our ability to release an alpha that will properly sort *after* its snapshots.


plugins {
    java
    application
    jacoco
}

repositories {
    mavenCentral()
}

dependencies {
    // NOTICE: More dependencies are declared below for testDist

    implementation("com.amazon.ion:ion-java:1.11.11")

    // TODO These shouldn't be needed when consumers embed Fusion.
    //  It's a build-time dependency, but here b/c consumers use the CLI to
    //  generate their docs.  That should be handled by a plugin instead.
    implementation("org.markdownj:markdownj:0.3.0-1.0.2b4")
    implementation("com.github.spullara.mustache.java:compiler:0.9.14")

    // https://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle-bom
    testImplementation(platform("org.junit:junit-bom:5.14.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.hamcrest:hamcrest:3.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }

    withJavadocJar()
}

val mainFusionRepo = layout.projectDirectory.dir("src/main/fusion")
val testFusionRepo = layout.projectDirectory.dir("src/test/fusion")

// Default output paths, hard-coded because the DSL doesn't seem to expose them.
val docsDir    = layout.buildDirectory.dir("docs")
val libsDir    = layout.buildDirectory.dir("libs")
val reportsDir = layout.buildDirectory.dir("reports")

// New output paths for Fusion code coverage.
val fcovDataDir = layout.buildDirectory.dir("fcov")
val fcovReportDir = reportsDir.get().dir("fcov")


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

    useJUnitPlatform()

    // Collect Fusion coverage data IFF a report is being generated.
    mustRunAfter(fcovConfigure)

    inputs.dir(testFusionRepo)

    jvmArgumentProviders.add {
        if (fcovRunning) {
            logger.lifecycle("Enabling Fusion code coverage instrumentation")
            listOf("-Ddev.ionfusion.fusion.coverage.DataDir=" + fcovDataDir.get().asFile.path)
        }
        else {
            listOf()
        }
    }
}

val ftstRepo = tasks.register<Jar>("ftstRepo") {
    destinationDirectory = libsDir
    archiveFileName = "ftst-repo.jar"

    into("FUSION-REPO") {
        from(testFusionRepo)
        includeEmptyDirs = true
    }
}


// We rely on Gradle defaults to invoke unit tests in the `test` suite.
// Here, we define a separate test suite for checking the content of our
// distribution archives. This is effectively testing our Gradle build logic.

// This approach is incubating; expect changes.
// https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html

testing {
    suites {
        // This test suite ensures the distribution is functional.
        register<JvmTestSuite>("testDist") {
            dependencies {
                // Docs claim that Jupiter is configured by default, but these
                // are all necessary.
                implementation(platform("org.junit:junit-bom:5.14.1"))
                implementation("org.junit.jupiter:junit-jupiter")
                implementation("org.hamcrest:hamcrest:3.0")
                implementation("org.htmlunit:htmlunit:4.19.0")

                // Provide access to utility classes from src/test
                implementation(sourceSets.test.get().runtimeClasspath)
            }

            // "Future iterations of the plugin will allow defining multiple
            // targets based other attributes, such as a particular JDK runtime."
            targets {
                all {
                    testTask.configure {
                        // Install the tarball so we can test it.
                        dependsOn(tasks.installDist)
                        // If unit tests are running, complete them first.
                        shouldRunAfter(tasks.test)
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("testDist"))
}


//=============================================================================
// Code Coverage

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
    // We don't dependOn distTest; that tests the packaging not the product.
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


// TODO Clean fcovDataDir somewhere

val fcovConfigure = tasks.register<WriteProperties>("fcovConfigure") {
    destinationFile = fcovDataDir.get().file("config.properties")

    property("IncludedModules", "/fusion")

    // Enable this if you want to check that tests are being run.
    // TODO Figure out why this causes report generation to fail.
//    property("IncludedSources", "ftst")
}

// Name is ick but mirrors jacocoTestReport
val fcovTestReport = tasks.register<JavaExec>("fcovTestReport") {
    dependsOn(fcovConfigure, tasks.test)

    group = "verification"
    description = "Generates Fusion code coverage report"

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = java.toolchain.languageVersion
    }

    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "dev.ionfusion.fusion.cli.Cli"
    args = listOf("report_coverage",
                  fcovDataDir.get().asFile.path,
                  fcovReportDir.asFile.path)

    enableAssertions = true
}


// Signal to test task to collect coverage data.
var fcovRunning = false
gradle.taskGraph.whenReady {
    fcovRunning = hasTask(fcovTestReport.get())
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

val fusiondocDir = docsDir.get().dir("fusiondoc")

tasks.register<JavaExec>("fusiondoc") {
    group = "Documentation"
    description = "Generates Fusion language and library documentation."

    var articlesDir = layout.projectDirectory.dir("src/doc/articles")
    var assetsDir   = layout.projectDirectory.dir("src/doc/assets")

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = java.toolchain.languageVersion
    }

    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "dev.ionfusion.fusion.cli.Cli"
    args = listOf("document",
                  "--modules", mainFusionRepo.toString(),
                  "--articles", articlesDir.toString(),
                  "--assets", assetsDir.toString(),
                  fusiondocDir.asFile.path)

    enableAssertions = true

    // Docgen has Java code! Not sure if this is the best solution...
    dependsOn(tasks.compileJava)

    inputs.dir(mainFusionRepo)
    inputs.dir(articlesDir)
    inputs.dir(assetsDir)
    inputs.dir(layout.projectDirectory.dir("src/doc/templates"))
    outputs.dir(fusiondocDir)
}


//=============================================================================
// Distribution

// https://docs.gradle.org/current/userguide/application_plugin.html#sec:the_distribution
distributions {
    main {
        contents {
            tasks.distTar {
                dependsOn("fusiondoc", "javadoc")
            }
            tasks.distZip {
                dependsOn("fusiondoc", "javadoc")
            }
            tasks.installDist {
                dependsOn("fusiondoc", "javadoc")
            }

            // TODO JavaDocs should be beside, not inside, the Fusion docs.
            //  https://github.com/ion-fusion/fusion-java/issues/204
            into("docs") {
                from(fusiondocDir)
            }
            into("docs") {
                from(docsDir).include("javadoc/**")
            }
        }
    }
}


tasks {
    // It took a loooong time to figure out how to access APP_HOME.
    // https://discuss.gradle.org/t/42870/4
    // https://stackoverflow.com/questions/22153041?rq=3
    // It's still not entirely working: `gradle run` doesn't see the replaced script.
    startScripts {
        doLast {
            unixScript.writeText(unixScript.readText().replace("{{APP_HOME}}", "'\${APP_HOME}'"))
            windowsScript.writeText(windowsScript.readText().replace("{{APP_HOME}}", "%APP_HOME%"))
        }
    }
}


application {
    applicationName = "fusion"
    mainClass.set("dev.ionfusion.fusion.cli.Cli")
    // This is unused today, but is likely to be useful and it was hard to do.
    applicationDefaultJvmArgs = listOf("-Ddev.ionfusion.fusion.Home={{APP_HOME}}/fusion")
}


// Gradle doesn't seem to have an equivalent "do everything" task.
tasks.register("release") {
    group = "Build"
    description = "Build all artifacts and reports"

    dependsOn(tasks.build, tasks.jacocoTestReport, fcovTestReport)
}
