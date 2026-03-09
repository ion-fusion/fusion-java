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


// Locate the repository from which we'll generate Fusion docs.
// TODO Define appropriate consumable and resolvable configurations for Fusion repos.
val mainFusionRepo = layout.projectDirectory.dir("../runtime/src/main/fusion")

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

val fusiondoc = tasks.register<JavaExec>("fusiondoc") {
    group = "Documentation"
    description = "Generates Fusion language and library documentation."

    val fusiondocDir = java.docsDir.dir("fusiondoc")

    var docSrcDir   = layout.projectDirectory.dir("src/doc")
    var articlesDir = docSrcDir.dir("articles")
    var assetsDir   = docSrcDir.dir("assets")

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = java.toolchain.languageVersion
    }

    classpath = project(":fusioncli").sourceSets["main"].runtimeClasspath
    mainClass = "dev.ionfusion.fusioncli.Cli"
    args = listOf("document",
                  "--modules",  mainFusionRepo.toString(),
                  "--articles", articlesDir.toString(),
                  "--assets",   assetsDir.toString(),
                  fusiondocDir.get().asFile.path)

    enableAssertions = true

    // Docgen has Java code! Not sure if this is the best solution...
    dependsOn(tasks.compileJava)

    inputs.dir(mainFusionRepo)
    inputs.dir(docSrcDir)
    outputs.dir(fusiondocDir)
}


//=============================================================================
// Distribution
// https://docs.gradle.org/current/userguide/application_plugin.html#sec:the_distribution


distributions {
    main {
        distributionBaseName = "ion-fusion-sdk"

        contents {
            val javadoc = project(":runtime").tasks.javadoc

            into("bin") {
                from(project(":fusioncli").tasks.named("startScripts"))
            }

            // TODO JavaDocs should be beside, not inside, the Fusion docs.
            //  https://github.com/ion-fusion/fusion-java/issues/204
            into("docs") {
                from(fusiondoc)
            }
            into("docs/javadoc") {
                from(javadoc)
            }

            from(rootDir.resolve("LICENSE"))

            filesMatching("**/README.md") {
                expand("project_version" to project.version)
            }
        }
    }
}
