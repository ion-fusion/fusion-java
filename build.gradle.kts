// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("buildlogic.fusion-common-conventions")
    id("buildlogic.java-common-conventions")
    id("jacoco-report-aggregation")
    id("test-report-aggregation")
}

// https://docs.gradle.org/current/userguide/jacoco_report_aggregation_plugin.html
// https://docs.gradle.org/current/userguide/test_report_aggregation_plugin.html

dependencies {
    // These include transitive project dependencies
    jacocoAggregation(project(":sdk"))
    jacocoAggregation(project(":testing"))

    testReportAggregation(project(":sdk"))
    testReportAggregation(project(":testing"))
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
    dependsOn(tasks.named<TestReport>("testAggregateTestReport"))
    dependsOn(fcovAggregateReport)
}

tasks.named<JacocoReport>("testCodeCoverageReport") {
    reports {
        // Simplify the output
        html.outputLocation = reporting.baseDirectory.dir("jacoco")
        xml.required = false
    }
}

tasks.named<TestReport>("testAggregateTestReport") {
    destinationDirectory = reporting.baseDirectory.dir("tests")
}


// TODO Apply aggregate JaCoCo coverage rules?


//======================================================================================
// Fusion coverage aggregate report

// Declare the dataDirs for aggregate reporting.
dependencies {
    "fcovReportData"(project(path = ":runtime", configuration = "fcovData"))
    "fcovReportData"(project(path = ":fusioncli", configuration = "fcovData"))
}


// The task is here instead of the conventions file because it is tied to the CLI's
// classpath, which can't be used from every subproject (notably `runtime`).

val fcovReportData by configurations.getting
val fcovReportDir = reporting.baseDirectory.dir("fcov")

val fcovAggregateReport = tasks.register<JavaExec>("fcovAggregateReport") {
    group = "verification"
    description = "Generates Fusion code coverage report"

    dependsOn(fcovReportData)
    outputs.dir(fcovReportDir)

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = java.toolchain.languageVersion
    }

    classpath = project(":fusioncli").sourceSets["main"].runtimeClasspath
    mainClass = "dev.ionfusion.fusioncli.Cli"
    args = listOf("report_coverage",
                  "--configFile", "fcov.properties",
                  "--htmlDir", fcovReportDir.get().asFile.path) +
            fcovReportData.files.map { it.toString() }

    enableAssertions = true

    doFirst {
        // Print the fcovReportData artifacts for easy debugging.
        logger.lifecycle("Reporting Fusion coverage data from: ${fcovReportData.files}")
        // TODO This information should appear in the report itself.
    }
}
