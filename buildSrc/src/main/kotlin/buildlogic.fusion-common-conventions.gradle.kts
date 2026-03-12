// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

// Conventions for Fusion projects.

plugins {
    base
    java // This is too much, but I can't find `reporting` and `check` otherwise.
}


// Paths for Fusion code coverage.
val fcovConfig = layout.projectDirectory.file("fcov.properties")
val fcovDataDir = layout.buildDirectory.dir("fcov")
val fcovReportDir = reporting.baseDirectory.dir("fcov")


//=============================================================================
// Code Coverage Data Collection

tasks.test {
    // Rerun tests if the coverage config changes; we might record different things.
    inputs.file(fcovConfig)
    outputs.dir(fcovDataDir)

    jvmArgumentProviders.add(::instrumentationArguments)
}

private fun instrumentationArguments(): List<String> {
    // Only collect coverage data when the `fcovTestCollect` task is needed.
    if (!gradle.taskGraph.hasTask(fcovTestCollect.get())) {
        return emptyList()
    }

    logger.lifecycle("Enabling Fusion code coverage instrumentation")
    return listOf(
        "-Ddev.ionfusion.fusion.coverage.DataDir=${fcovDataDir.get().asFile.path}",
        "-Ddev.ionfusion.fusion.coverage.Config=${fcovConfig.asFile.path}"
    )
}


// Signal the test task to collect Fusion coverage data.
val fcovTestCollect by tasks.registering {
    dependsOn(tasks.test)
    outputs.dir(fcovDataDir)
    doLast {
        logger.lifecycle("Collected Fusion coverage data into ${fcovDataDir.get().asFile.path}")
    }
}

tasks.check {
    // To speed up the dev cycle, only instrument for coverage when running full checks.
    dependsOn(fcovTestCollect)
}


//=============================================================================
// Code Coverage Reporting

// Consumable configuration for providing access to coverage data.
val fcovData by configurations.creating {
    description = "Fusion code coverage data"
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add("fcovData", fcovDataDir) {
        builtBy(fcovTestCollect)
        // ...indirectly: `test` make the artifacts when `fcovTestCollect` is enabled.
    }
}


// Resolvable configuration for reporting to gather all the coverage data.
val fcovReportData by configurations.creating {
    description = "Fusion code coverage data to report"
    isCanBeConsumed = false
    isCanBeResolved = true
}

// The reporting task is in the root project, since it needs the CLI classpath.
