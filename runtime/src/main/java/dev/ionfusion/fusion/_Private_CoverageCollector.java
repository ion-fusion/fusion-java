// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.runtime.base.SourceLocation;

/**
 * EXPERIMENTAL extension point for collecting code-coverage statistics.
 * <p>
 * At compile time, {@link #locationIsRecordable(SourceLocation)} is called
 * for each (runtime) code point for which a location is known.  The collector
 * may constrain the extent of coverage metrics by filtering based on location:
 * a {@code false} result indicates that the code point should not be
 * instrumented.  If the code is instrumented, it must be recorded with
 * {@link #locationInstrumented}.
 * <p>
 * At run time, {@link #locationEvaluated(SourceLocation)} is called each time a
 * coverable code point is (about to be) evaluated.
 */
public interface _Private_CoverageCollector
{
    /**
     * Determines if the code at some location should be instrumented.
     * A true result is not an obligation to instrument the code.
     *
     * @return whether the compiler should record coverage for the location.
     * If false, then {@link #locationInstrumented} and {@link #locationEvaluated}
     * must not be called with an equivalent location.
     */
    boolean locationIsRecordable(SourceLocation loc);

    /**
     * Records that the code at some location has been instrumented.
     * <p>
     * This method is called during compilation, so the code point hasn't been
     * evaluated yet (and may never be evaluated).
     * Implementations must be idempotent.
     *
     * @param loc must be {@linkplain #locationIsRecordable recordable}.
     */
    void locationInstrumented(SourceLocation loc);

    /**
     * Records that the code at some location is about to be evaluated.
     * This method will be called once for each evaluation associated with the
     * location.
     *
     * @param loc must have been {@linkplain #locationInstrumented instrumented}.
     */
    void locationEvaluated(SourceLocation loc);


    // TODO Think this through. I'm not really convinced this is correct.
    //      Perhaps close() would be better?  save()?
    // This appears to be unused.
    void flushMetrics()
        throws FusionException;
}
