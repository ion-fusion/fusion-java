// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionSyntax.isSyntax;
import static dev.ionfusion.fusion.FusionSyntax.unsafeSyntaxPosition;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.runtime.base.FusionException;


class DatumToSyntaxProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityRange(eval, 1, 3, args);

        Object           datum    = args[0];
        SyntaxValue      context  = null;
        ResourcePosition position = null;

        if (args.length > 1)
        {
            // TODO #68 This should accept arbitrary syntax objects.
            context = validateContext(eval, args);

            if (args.length > 2)
            {
                if (! isSyntax(eval, args[2]))
                {
                    throw argError(eval, "syntax object", 2, args);
                }
                position = unsafeSyntaxPosition(eval, args[2]);
            }
        }

        return Syntax.datumToSyntax(eval, datum, context, position);
    }


    private SyntaxValue validateContext(Evaluator eval, Object[] args)
        throws FusionException
    {
        var arg1 = args[1];
        if (isSyntax(eval, arg1))
        {
            Object content = FusionSyntax.unsafeSyntaxUnwrap(eval, arg1);
            if (! FusionCollection.isCollection(eval, content))
            {
                return (SyntaxValue) arg1;
            }
        }

        throw argError(eval, "non-collection syntax object", 1, args);
    }
}
