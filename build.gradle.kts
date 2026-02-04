// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("buildlogic.java-common-conventions")
    id("jacoco-report-aggregation")
    id("test-report-aggregation")
}

// https://docs.gradle.org/current/userguide/jacoco_report_aggregation_plugin.html
// https://docs.gradle.org/current/userguide/test_report_aggregation_plugin.html

dependencies {
    jacocoAggregation(project(":sdk"))
    jacocoAggregation(project(":testing"))
    testReportAggregation(project(":sdk"))
    testReportAggregation(project(":testing"))
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
    dependsOn(tasks.named<TestReport>("testAggregateTestReport"))
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
