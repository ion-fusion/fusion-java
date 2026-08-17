// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.cover;

import dev.ionfusion.runtime.base.CodePosition;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EXPERIMENTAL extension point for collecting code-coverage statistics.
 * <p>
 * At compile time, {@link #locationIsRecordable} is called
 * for each (runtime) code point for which a location is known.  The collector
 * may constrain the extent of coverage metrics by filtering based on location:
 * a {@code false} result indicates that the code point should not be
 * instrumented.  If the code is instrumented, it must be recorded with
 * {@link #locationInstrumented}. The resulting counter should be incremented each time
 * the instrumented location is (about to be) evaluated.
 */
public interface CoverageCollector
{
    /**
     * Determines if the code at some location should be instrumented.
     * A true result is not an obligation to instrument the code.
     *
     * @return whether the compiler should record coverage for the location.
     * If false, then {@link #locationInstrumented} must not be called with an
     * equivalent location.
     */
    boolean locationIsRecordable(CodePosition loc);

    /**
     * Records that the code at some location has been instrumented.
     * <p>
     * This method is called during compilation, so the code point hasn't been evaluated
     * yet (and may never be evaluated). Implementations must be idempotent.
     *
     * @param loc must be {@linkplain #locationIsRecordable recordable}.
     *
     * @return the counter for the location, to be incremented for each evaluation.
     */
    AtomicInteger locationInstrumented(CodePosition loc);
}
