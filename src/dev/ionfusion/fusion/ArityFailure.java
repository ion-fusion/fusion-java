// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.writeMany;
import java.io.IOException;


/**
 * Indicates a failure applying a procedure with the wrong number of arguments.
 */
@SuppressWarnings("serial")
final class ArityFailure
    extends ContractException
{
    private final String myProcIdentity;
    private final int myMinArity;
    private final int myMaxArity;
    private final Object[] myActuals;


    ArityFailure(String procIdentity, int minArity, int maxArity,
                 Object... actuals)
    {
        super("arity failure");
        assert procIdentity != null && actuals != null;
        assert minArity <= maxArity;
        myProcIdentity = procIdentity;
        myMinArity = minArity;
        myMaxArity = maxArity;
        myActuals = actuals;
    }

    /**
     * @param proc must not be null
     */
    ArityFailure(Procedure proc, int minArity, int maxArity,
                 Object... actuals)
    {
        this(proc.identify(), minArity, maxArity, actuals);
    }


    protected void displayArityExpectation(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        int base = myMinArity;
        if (myMaxArity == Integer.MAX_VALUE)
        {
            out.append("at least ");
            out.append(Integer.toString(myMinArity));
        }
        else
        {
            out.append(Integer.toString(myMinArity));

            if (myMinArity != myMaxArity)
            {
                out.append(" to ");
                out.append(Integer.toString(myMaxArity));
                base = myMaxArity;
            }
        }
        out.append(" argument");
        if (base != 1) out.append("s");
    }


    @Override
    void displayMessage(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        out.append(myProcIdentity);
        out.append(" expects ");
        displayArityExpectation(eval, out);
        out.append(", given ");
        out.append(Integer.toString(myActuals.length));
        if (myActuals.length != 0)
        {
            out.append(":\n  ");
            writeMany(eval, out, myActuals, "\n  ");
        }
    }
}
