// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionSyntax.isIdentifier;
import static dev.ionfusion.fusion.FusionSyntax.isSyntax;
import static dev.ionfusion.fusion.FusionSyntax.unsafeSyntaxLocation;
import static dev.ionfusion.fusion.Syntax.datumToSyntax;


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
            // TODO This should accept arbitrary syntax objects.
            //  https://github.com/ion-fusion/fusion-java/issues/68
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
