// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionBool.trueBool;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import java.io.IOException;


/**
 * Utilities for working with Fusion's singular {@code void} value.
 *
 * @see FusionValue
 */
public final class FusionVoid
{
    private FusionVoid() {}


    /** The singular {@code void} value. */
    private final static BaseValue VOID =
        new BaseValue()
        {
            @Override
            BaseBool isTruthy(Evaluator eval)
            {
                return falseBool(eval);
            }

            @Override
            BaseBool not(Evaluator eval)
            {
                return trueBool(eval);
            }

            @Override
            BaseBool looseEquals(Evaluator eval, Object right)
                throws FusionException
            {
                // Object comparison has already been performed, so we know
                // we've not been given void.
                return falseBool(eval);
            }

            @Override
            void write(Evaluator eval, Appendable out) throws IOException
            {
                out.append("{{{void}}}");
            }
        };


    //========================================================================
    // Constructors

    /**
     * Returns the Fusion {@code void} value.
     * <b>It is an error to make any assumptions about whether this value is
     * null or not!</b>
     */
    static Object voidValue(Evaluator eval)
    {
        return VOID;
    }


    //========================================================================
    // Predicates

    /**
     * Determines whether a Fusion value is void.
     *
     * @param top the top-level that was the source of the value.
     * @param value the value to test.
     *
     * @return {@code true} if the value is void, otherwise {@code false}
     */
    public static boolean isVoid(TopLevel top, Object value)
    {
        assert top != null;
        return (value == VOID);
    }

    static boolean isVoid(Evaluator eval, Object value)
    {
        assert eval != null;
        return (value == VOID);
    }


    //========================================================================


    static final class VoidProc extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
        {
            return VOID;
        }
    }


    static final class IsVoidProc extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
        {
            return makeBool(eval, arg == VOID);
        }
    }
}
