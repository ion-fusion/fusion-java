// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.runtime.base.FusionException;

/**
 * The results of the syntax preparation phase, ready for execution.
 */
interface CompiledForm
{
    /** A zero-length array. */
    CompiledForm[] EMPTY_ARRAY = new CompiledForm[0];


    /**
     * Evaluates a compiled form using the given dynamic context.
     * <p>
     * <em>Do not call this directly! Go through the evaluator.</em>
     */
    Object doEval(Evaluator eval, Store store)
        throws FusionException;
}
