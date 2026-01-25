// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.makeBool;

import dev.ionfusion.runtime.embed.TopLevel;

/**
 * Utilities for Fusion procedures.
 */
public final class FusionProcedure
{
    private FusionProcedure() {}


    /**
     * Determines whether the given Fusion value is a procedure.
     *
     * @param top the top-level that was the source of the value.
     * @param value the value to test.
     *
     * @return {@code true} if the value is a Fusion procedure,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isProcedure(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof Procedure);
    }


    /**
     * Determines whether the given Fusion value is a procedure.
     *
     * @param eval must not be null.
     */
    static boolean isProcedure(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof Procedure);
    }


    static final class IsProcedureProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            return makeBool(eval, arg instanceof Procedure);
        }
    }
}
