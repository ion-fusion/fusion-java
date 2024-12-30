// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionString.checkNonEmptyStringArg;


final class LoadProc
    extends Procedure1
{
    private final LoadHandler myLoadHandler;

    LoadProc(LoadHandler loadHandler)
    {
        myLoadHandler = loadHandler;
    }


    @Override
    public Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        String fileName = checkNonEmptyStringArg(eval, this, 0, arg);

        return myLoadHandler.loadTopLevel(eval, null, fileName);
    }
}
