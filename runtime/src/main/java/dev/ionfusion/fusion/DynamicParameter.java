// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionString.stringToJavaString;


/**
 *
 */
class DynamicParameter
    extends Procedure0
{
    private final Object myInitialValue;


    public DynamicParameter(Object initialValue)
    {
        myInitialValue = initialValue;
    }


    Object getInitialValue()
    {
        return myInitialValue;
    }


    @SuppressWarnings("unchecked")
    <T> T currentValue(Evaluator eval)
    {
        Object result = eval.firstContinuationMark(this);
        return (T) (result == null ? myInitialValue : result);
    }


    String asString(Evaluator eval)
        throws FusionException
    {
        Object value = currentValue(eval);
        return stringToJavaString(eval, value);
    }


    @Override
    Object doApply(Evaluator eval)
        throws FusionException
    {
        return currentValue(eval);
    }


    static final class CurrentMarkSexp
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object key)
            throws FusionException
        {
            return eval.continuationMarkSexp(key);
        }
    }
}
