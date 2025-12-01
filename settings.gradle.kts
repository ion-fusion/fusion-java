// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

/*
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be
 * found in the user manual at:
 *   https://docs.gradle.org/current/userguide/multi_project_builds.html
 */

// Among other things, this becomes the module name in IDEA.
rootProject.name = "fusion-java"

include("testing", "runtime", "sdk")
