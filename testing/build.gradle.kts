// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("buildlogic.java-library-conventions")
    id("buildlogic.jacoco-conventions")
}

dependencies {
    // TODO DRY the versioning
    implementation(platform("org.junit:junit-bom:5.14.1"))
    implementation("org.junit.jupiter:junit-jupiter")
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
