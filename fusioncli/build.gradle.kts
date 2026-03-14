// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("buildlogic.fusion-common-conventions")
    id("buildlogic.java-application-conventions")
    id("buildlogic.jacoco-conventions")
}

dependencies {
    implementation(project(":runtime"))

    implementation("org.markdownj:markdownj:0.3.0-1.0.2b4")
    implementation("com.github.spullara.mustache.java:compiler:0.9.14")

    testImplementation(project(":testing"))
    testImplementation("org.htmlunit:htmlunit:4.19.0")
}


val mainFusionRepo = layout.projectDirectory.dir("src/main/fusion")

// Bundle the Fusion repository in our jar.
// TODO DRY this WRT runtime
tasks.jar {
    into("FUSION-REPO") {
        from(mainFusionRepo)
        includeEmptyDirs = true
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.40".toBigDecimal()
            }
        }
    }
}


//=============================================================================
// Application

// It took a loooong time to figure out how to access APP_HOME.
// https://discuss.gradle.org/t/42870/4
// https://stackoverflow.com/questions/22153041?rq=3
// It's still not entirely working: `gradle run` doesn't see the replaced script.
tasks.startScripts {
    doLast {
        unixScript.writeText(unixScript.readText().replace("{{APP_HOME}}", "'\${APP_HOME}'"))
        windowsScript.writeText(windowsScript.readText().replace("{{APP_HOME}}", "%APP_HOME%"))
    }
}

application {
    applicationName = "fusion"
    mainClass.set("dev.ionfusion.fusioncli.Cli")
    // This is unused today, but is likely to be useful and it was hard to do.
    applicationDefaultJvmArgs = listOf("-Ddev.ionfusion.fusion.Home={{APP_HOME}}/fusion")
}
