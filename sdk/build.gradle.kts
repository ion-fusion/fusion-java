// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

plugins {
    // TODO The `application` plugins seems too heavy, but I can't figure out how to
    //  get the right libs without it.
    id("buildlogic.java-application-conventions")
    distribution
}

// Enabling JaCoCo doesn't add value, since our tests only run production code
// by executing the CLI.

dependencies {
    implementation(project(":fusioncli"))

    testImplementation(project(":testing"))
    testImplementation("org.htmlunit:htmlunit:4.19.0")
}


// This subproject currently doesn't have any Java code
tasks.jar {
    enabled = false
}

// We will copy these from the `fusioncli` project.
tasks.startScripts {
    enabled = false
}


//=============================================================================
// Tests

tasks.test {
    // Install the tarball so we can test it.
    dependsOn(tasks.installDist)
}


//=============================================================================
// Documentation

val fusiondoc by tasks.registering(JavaExec::class) {
    group = "Documentation"
    description = "Generates Fusion language and library documentation."

    // Locate the repository from which we'll generate Fusion docs.
    // TODO Define consumable and resolvable configurations for Fusion repos.
    val runtimeRepo = project(":runtime").file("src/main/fusion")
    inputs.dir(runtimeRepo)

    var docSrcDir   = layout.projectDirectory.dir("src/doc")
    inputs.dir(docSrcDir)

    var articlesDir = docSrcDir.dir("articles")
    var assetsDir   = docSrcDir.dir("assets")

    val outputDir = java.docsDir.dir("fusiondoc")
    outputs.dir(outputDir)


    javaLauncher = javaToolchains.launcherFor {
        languageVersion = java.toolchain.languageVersion
    }

    classpath = project(":fusioncli").sourceSets["main"].runtimeClasspath
    mainClass = "dev.ionfusion.fusioncli.Main"
    args = listOf("document",
                  "--modules",  runtimeRepo.toString(),
                  "--articles", articlesDir.toString(),
                  "--assets",   assetsDir.toString(),
                  outputDir.get().asFile.path)

    enableAssertions = true
}


//=============================================================================
// Distribution
// https://docs.gradle.org/current/userguide/application_plugin.html#sec:the_distribution


distributions {
    main {
        distributionBaseName = "ion-fusion-sdk"

        contents {
            into("bin") {
                from(project(":fusioncli").tasks.startScripts)
            }

            // TODO JavaDocs should be beside, not inside, the Fusion docs.
            //  https://github.com/ion-fusion/fusion-java/issues/204
            into("docs") {
                from(fusiondoc)
            }
            into("docs/javadoc") {
                from(project(":runtime").tasks.javadoc)
            }

            from(rootDir.resolve("LICENSE"))

            filesMatching("**/README.md") {
                expand("project_version" to project.version)
            }
        }
    }
}
