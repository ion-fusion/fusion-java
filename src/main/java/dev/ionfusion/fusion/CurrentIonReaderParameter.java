// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * A derived parameter that delays initialization until first use.
 * This avoids opening an IonReader over stdin unless we really need to.
 * <p>
 * TODO This should be built as a derived parameter.
 *   https://github.com/ion-fusion/fusion-java/issues/69
 */
final class CurrentIonReaderParameter
    extends DynamicParameter
{
    private Object myDefaultValue;

    CurrentIonReaderParameter()
    {
        super(null);
    }

    @Override
    <T> T currentValue(Evaluator eval)
    {
        Object result = super.currentValue(eval);
        if (result == null)
        {
            // There's no parameterization!  Determine the default value.
            synchronized (this)
            {
                if (myDefaultValue == null)
                {
                    // TODO use current-input-port?
                    // That may be hard to predict the result.
                    myDefaultValue = eval.getIonReaderBuilder().build(System.in);
                }
                result = myDefaultValue;
            }
        }

        return (T) result;
    }
}
