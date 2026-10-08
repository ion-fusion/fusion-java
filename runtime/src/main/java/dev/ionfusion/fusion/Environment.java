// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * Maps identifiers to {@link Binding}s.
 * Environments are compile-time entities, arranged in a hierarchy of lexical
 * scopes.
 */
interface Environment
{
    Namespace namespace();
}
