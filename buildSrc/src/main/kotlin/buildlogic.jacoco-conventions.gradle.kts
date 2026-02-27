// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0


// https://docs.gradle.org/current/userguide/jacoco_plugin.html

plugins {
    jacoco
    `java-library` // Brings in `test` task
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

// TODO Build an easier way to declare this limit in subprojects.
//   I think it's worth raising the limit progressively, per subproject.
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
//                minimum = "0.75".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.build {
    dependsOn(tasks.jacocoTestReport)
}
