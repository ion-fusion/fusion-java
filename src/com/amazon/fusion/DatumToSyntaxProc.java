// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionSyntax.isIdentifier;
import static com.amazon.fusion.FusionSyntax.isSyntax;
import static com.amazon.fusion.FusionSyntax.unsafeSyntaxLocation;
import static com.amazon.fusion.Syntax.datumToSyntax;


class DatumToSyntaxProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityRange(1, 3, args);

        Object         datum    = args[0];
        SyntaxSymbol   context  = null;
        SourceLocation location = null;

        if (args.length > 1)
        {
            // TODO FUSION-329 This should accept arbitrary syntax objects.
            if (! isIdentifier(eval, args[1]))
            {
                throw argFailure("syntax identifier", 1, args);
            }
            context = (SyntaxSymbol) args[1];

            if (args.length > 2)
            {
                if (! isSyntax(eval, args[2]))
                {
                    throw argFailure("syntax object", 2, args);
                }
                location = unsafeSyntaxLocation(eval, args[2]);
            }
        }

        return datumToSyntax(eval, datum, context, location);
    }
}
