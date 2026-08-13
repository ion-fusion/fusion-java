// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionString.makeString;
import static dev.ionfusion.fusion.FusionSyntax.checkSyntaxArg;
import static dev.ionfusion.fusion.FusionVoid.voidValue;

import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ResourceDescriptor;
import dev.ionfusion.runtime.base.ResourcePosition;


class SyntaxSourceProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        SyntaxValue      stx = checkSyntaxArg(eval, this, 0, arg);
        ResourcePosition pos = stx.getPosition();
        if (pos != null)
        {
            ResourceDescriptor rsrc = pos.getResourceDesc();
            return makeString(eval, rsrc.display());
        }
        return voidValue(eval);
    }
}
