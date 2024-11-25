// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

/**
 * The results of the syntax preparation phase, ready for execution.
 */
interface CompiledForm
{
    /** A zero-length array. */
    CompiledForm[] EMPTY_ARRAY = new CompiledForm[0];


    /** Don't call directly! Go through the evaluator. */
    Object doEval(Evaluator eval, Store store)
        throws FusionException;
}
