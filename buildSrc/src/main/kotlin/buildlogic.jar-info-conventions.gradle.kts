// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

// Conventions for embedding build information in our artifacts.

import buildlogic.GenerateJarInfo
import buildlogic.GitInfoValueSource

plugins {
    java
}


/**
 * Path, within every one of our jars, of the generated build-information resource.
 * The name is derived from our Java package namespace, so it can't collide with a
 * similar file from another organization's jars.
 *
 * See [GenerateJarInfo] for why all artifacts share one path.
 */
val jarInfoResourcePath = "META-INF/dev.ionfusion.versions.properties"


// One `git` interrogation per build: the parameters are identical across subprojects,
// so Gradle reuses the result.
val gitInfo = providers.of(GitInfoValueSource::class.java) {
    parameters {
        repositoryDirectory = rootProject.layout.projectDirectory
    }
}


val generateJarInfo by tasks.registering(GenerateJarInfo::class) {
    group = "build"
    description = "Generates the build-information resource describing this artifact."

    artifactId      = base.archivesName
    artifactVersion = provider { project.version.toString() }

    shortCommitHash = gitInfo.map { it.shortCommitHash }
    longCommitHash  = gitInfo.map { it.longCommitHash }
    commitDate      = gitInfo.map { it.commitDate }
    repoStatus      = gitInfo.map { it.repoStatus }

    resourcePath    = jarInfoResourcePath
    outputDirectory = layout.buildDirectory.dir("generated/resources/jar-info")
}


// Registering the output as a resource directory gets the file into the jar, and onto
// the classpath of our own tests and IDE run configurations.
sourceSets.main {
    resources.srcDir(generateJarInfo)
}
