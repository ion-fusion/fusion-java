// Copyright (c) 2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;


/**
 * Utilities for working with Fusion {@code blob} and {@code clob} values.
 *
 * @see FusionValue
 */
public final class FusionLob
{
    private FusionLob() {}


    //========================================================================
    // Representation Classes


    abstract static class BaseLob
        extends BaseValue
    {
        BaseLob() {}
    }


    //========================================================================
    // Predicates


    /**
     * Determines whether a Fusion value has type {@code blob} or {@code clob}.
     */
    public static boolean isLob(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseLob);
    }


    static boolean isLob(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseLob);
    }


    //========================================================================
    // Conversions


    //========================================================================
    // Procedure Helpers

}
