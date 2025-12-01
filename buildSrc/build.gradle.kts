// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

plugins {
    // Support convention plugins written in Kotlin.
    // Convention plugins are build scripts in 'src/main' that automatically
    // become available as plugins in the main build.
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to build our convention plugins.
    gradlePluginPortal()
}
