// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


import dev.ionfusion.runtime.base.FusionException;

final class CompiledConstant
    implements CompiledForm
{
    private final Object myValue;

    CompiledConstant(Object value)
    {
        myValue = value;
    }

    Object getValue()
    {
        return myValue;
    }

    @Override
    public Object doEval(Evaluator eval, Store store)
        throws FusionException
    {
        return myValue;
    }
}
