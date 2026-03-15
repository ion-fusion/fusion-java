// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import dev.ionfusion.runtime.base.FusionException;

/**
 * Signals that the evaluation thread invoked the {@code exit} procedure.
 */
@SuppressWarnings("serial")
public final class ExitException
    extends FusionException
{
    public ExitException() { super("Exit requested"); }
}
