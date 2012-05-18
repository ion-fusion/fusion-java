// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.IonList;
import com.amazon.ion.IonSequence;
import com.amazon.ion.IonSexp;
import com.amazon.ion.IonSymbol;
import com.amazon.ion.IonValue;
import java.util.Iterator;

/**
 *
 */
public class ForListKeyword
    extends KeywordValue
{
    ForListKeyword()
    {
        //    "                                                                               |
        super("((IDENT SEQ-EXPR)) BODY ...+",
              "Iterates the SEQ-EXPR, binding IDENT to each element in turn and evaluating\n" +
              "BODY. Returns a new list of the results.");
    }

    @Override
    FusionValue invoke(Evaluator eval, Environment env, IonSexp forStx)
        throws FusionException
    {
        SyntaxChecker check = new SyntaxChecker(getInferredName(), forStx);
        check.arityAtLeast(2);

        IonSequence bindingForms =
            check.requiredSequence("sequence of bindings", 1);

        final int numBindings = bindingForms.size();

        IonList result = eval.getSystem().newEmptyList();

        if (numBindings != 0)
        {
            String[] boundNames = new String[numBindings];

            @SuppressWarnings("unchecked")
            Iterator<FusionValue>[] boundIters = new Iterator[numBindings];

            for (int i = 0; i < numBindings; i++)
            {
                IonSexp binding =
                    requiredSexp("name/value binding", i, bindingForms);
                IonSymbol name =
                    requiredSymbol("name/value binding", 0, binding);
                boundNames[i] = name.stringValue();

                IonValue boundExpr =
                    requiredForm("name/value binding", 1, binding);
                FusionValue boundValue = eval.eval(env, boundExpr);
                boundIters[i] = Sequences.iteratorFor(boundValue);
            }

            FusionValue[] boundValues = new FusionValue[numBindings];
            Environment bodyEnv =
                new LocalEnvironment(env, boundNames, boundValues);

            while (allItersHaveNext(boundIters))
            {
                // Determine the next round of bound values
                for (int i = 0; i < numBindings; i++)
                {
                    Iterator<FusionValue> iter = boundIters[i];
                    boundValues[i] = iter.next();
                }

                // Evaluate the body.
                FusionValue nextResult = null;
                for (int i = 2; i < forStx.size(); i++)
                {
                    IonValue bodyStx = forStx.get(i);
                    nextResult = eval.eval(bodyEnv, bodyStx);
                }

                if (nextResult instanceof DomValue)
                {
                    IonValue value = nextResult.getDom();
                    AddProc.invoke(result, value);
                }
                else
                {
                    throw new ContractFailure("body of " + getInferredName()
                                              + "returned non-Ion value: "
                                              + nextResult);
                }
            }
        }
        return new DomValue(result);
    }

    private boolean allItersHaveNext(Iterator<FusionValue>[] boundIters)
    {
        for (Iterator<FusionValue> iter : boundIters)
        {
            if (! iter.hasNext()) return false;
        }
        return true;
    }
}
