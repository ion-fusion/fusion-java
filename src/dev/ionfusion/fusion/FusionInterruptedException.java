// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * Thrown to indicate that a Fusion computation was interrupted. When thrown,
 * the current thread's interrupt status will have been set.
 * <p>
 * This is distinct from the standard {@link InterruptedException} because
 * that is checked, which would force its declaration everywhere.
 * Instead, we throw this alternative, which is easier for applications to
 * handle since they must already be handling {@link FusionException}.
 */
@SuppressWarnings("serial")
public final class FusionInterruptedException
    extends FusionException
{
    FusionInterruptedException(FusionInterrupt cause)
    {
        super("The Fusion evaluation thread was interrupted.", cause);
        assert Thread.currentThread().isInterrupted();
    }
}
