// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ion_fusion.fusion;

import static dev.ion_fusion.fusion.FusionString.makeString;
import static dev.ion_fusion.fusion.FusionVoid.voidValue;


class SyntaxSourceProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        SyntaxValue stx = checkSyntaxArg(0, arg);
        SourceLocation location = stx.getLocation();
        if (location != null)
        {
            SourceName name = location.getSourceName();
            if (name != null)
            {
                return makeString(eval, name.display());
            }
        }
        return voidValue(eval);
    }
}
