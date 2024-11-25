// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

/**
 * Thown to force the exit of an evaluation.
 */
@SuppressWarnings("serial")
public final class ExitException
    extends FusionException
{
    ExitException() { super("Exit requested"); }
}
