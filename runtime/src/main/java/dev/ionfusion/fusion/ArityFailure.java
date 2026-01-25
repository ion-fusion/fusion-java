// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWriteMany;


/**
 * Indicates a failure applying a procedure with the wrong number of arguments.
 */
@SuppressWarnings("serial")
final class ArityFailure
    extends ContractException
{
    private ArityFailure(String message)
    {
        super(message);
    }


    private static void displayExpectation(StringBuilder out, int minArity, int maxArity)
    {
        int base = minArity;
        if (maxArity == Integer.MAX_VALUE)
        {
            out.append("at least ");
            out.append(minArity);
        }
        else
        {
            out.append(minArity);

            if (minArity != maxArity)
            {
                out.append(" to ");
                out.append(maxArity);
                base = maxArity;
            }
        }
        out.append(" argument");
        if (base != 1) out.append("s");
    }

    static FusionException makeArityError(Evaluator eval, String name,
                                          int minArity, int maxArity,
                                          Object... actuals)
    {
        assert name != null && actuals != null;
        assert minArity <= maxArity;

        StringBuilder out = new StringBuilder();
        out.append(name);
        out.append(" expects ");
        displayExpectation(out, minArity, maxArity);
        out.append(", given ");
        out.append(actuals.length);
        if (actuals.length != 0)
        {
            out.append(":\n  ");
            safeWriteMany(eval, out, actuals, "\n  ");
        }

        return new ArityFailure(out.toString());
    }
}
