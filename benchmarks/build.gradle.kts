// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {
    implementation(project(":runtime"))
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}


val jmhReportDir = reporting.baseDirectory.dir("jmh")

tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Runs the JMH benchmarks in this project."

    dependsOn(tasks.classes)
    outputs.dir(jmhReportDir)

    // Using the default GC caused allocation-heavy benchmarks to OOM, presumably
    // because the GC could not keep up with the allocation rate.
    // Shenandoah keeps up and performs better than ZGC; both were added in JDK 15.
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
    jvmArgs = listOf("-XX:+UseShenandoahGC")

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")

    // Send output to a JSON file
    args("-rf", "json", "-rff", jmhReportDir.get().file("jmh.json"))

    // Always fail on error
    args("-foe", "true")

    args("-prof", "gc")

    args(providers.gradleProperty("jmhArgs").orNull
        ?.split(Regex("\\s+"))
        ?.filter { it.isNotEmpty() }
        ?: emptyList<String>())
}
