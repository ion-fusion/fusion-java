// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.trueBool;
import dev.ionfusion.fusion.FusionBool.BaseBool;


/**
 *
 */
final class FusionText
{
    private FusionText() {}


    abstract static class BaseText
        extends BaseValue
    {
        BaseText() {}

        @Override
        final boolean isAnnotatable()
        {
            return true;
        }

        abstract String stringValue();

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseText)
            {
                String r = ((BaseText) right).stringValue();
                if (r != null)
                {
                    String l = this.stringValue(); // not null
                    if (l.equals(r))
                    {
                        return trueBool(eval);
                    }
                }
            }

            return falseBool(eval);
        }
    }


    //========================================================================
    // Predicates


    public static boolean isText(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseText);
    }


    static boolean isText(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseText);
    }


    //========================================================================
    // Conversions


    /**
     * @param stringOrSymbol must be a Fusion string or symbol.
     *
     * @return null if given {@code null.string} or {@code null.symbol}.
     */
    static String unsafeTextToJavaString(Evaluator eval, Object stringOrSymbol)
        throws FusionException
    {
        return ((BaseText) stringOrSymbol).stringValue();
    }


    /**
     * Converts a Fusion text value to a {@link String}.
     *
     * @return null if the value isn't a Fusion string or symbol.
     */
    static String textToJavaString(Evaluator eval, Object value)
        throws FusionException
    {
        if (isText(eval, value))
        {
            return unsafeTextToJavaString(eval, value);
        }
        return null;
    }


    //========================================================================
    // Procedure Helpers


    /**
     * @param expectation must not be null.
     * @return may be null
     */
    static String checkTextArg(Evaluator eval,
                               Procedure who,
                               String    expectation,
                               int       argNum,
                               Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseText)
        {
            return ((BaseText) arg).stringValue();
        }
        throw who.argFailure(expectation, argNum, args);
    }


    /**
     * @return not null.
     */
    static String checkRequiredTextArg(Evaluator eval,
                                       Procedure who,
                                       int       argNum,
                                       Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-null string or symbol";
        String result = checkTextArg(eval, who, expectation, argNum, args);
        if (result == null)
        {
            throw who.argFailure(expectation, argNum, args);
        }
        return result;
    }


    /**
     * Similar to {@link #checkRequiredTextArg} but also expects non-empty
     * content.
     * @return not null or empty
     */
    static String checkNonEmptyTextArg(Evaluator eval,
                                       Procedure who,
                                       int       argNum,
                                       Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-empty string or symbol";
        String result = checkTextArg(eval, who, expectation, argNum, args);
        if (result == null || result.isEmpty())
        {
            throw who.argFailure(expectation, argNum, args);
        }
        return result;
    }
}
