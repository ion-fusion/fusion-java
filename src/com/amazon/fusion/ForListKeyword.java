// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.IonList;
import com.amazon.ion.IonValue;

/**
 *
 */
final class ForListKeyword
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
    SyntaxValue prepare(Evaluator eval, Environment env, SyntaxSexp forStx)
        throws SyntaxFailure
    {
        SyntaxChecker check = new SyntaxChecker(getInferredName(), forStx);
        check.arityAtLeast(2);

        SyntaxSequence bindingForms =
            check.requiredSequence("sequence of bindings", 1);

        final int numBindings = bindingForms.size();
        if (numBindings != 0)
        {
            String[] boundNames = new String[numBindings];

            for (int i = 0; i < numBindings; i++)
            {
                SyntaxSexp binding =
                    requiredSexp("name/value binding", i, bindingForms);
                SyntaxSymbol name =
                    requiredSymbol("name/value binding", 0, binding);
                boundNames[i] = name.stringValue();

                SyntaxValue boundExpr =
                    requiredForm("name/value binding", 1, binding);
                SyntaxValue expanded = boundExpr.prepare(eval, env);
                if (expanded != boundExpr) binding.set(1, expanded);
            }

            FusionValue[] boundValues = new FusionValue[numBindings];
            Environment bodyEnv =
                new LocalEnvironment(env, boundNames, boundValues);

            // Prepare the body.
            for (int i = 2; i < forStx.size(); i++)
            {
                SyntaxValue bodyStx = forStx.get(i);
                SyntaxValue expanded = bodyStx.prepare(eval, bodyEnv);
                if (expanded != bodyStx) forStx.set(i, expanded);
            }
        }

        return forStx;
    }


    @Override
    FusionValue invoke(Evaluator eval, Environment env, SyntaxSexp forStx)
        throws FusionException
    {
        SyntaxSequence bindingForms = (SyntaxSequence) forStx.get(1);

        final int numBindings = bindingForms.size();

        IonList result = eval.getSystem().newEmptyList();

        if (numBindings != 0)
        {
            String[] boundNames = new String[numBindings];
            Stream[] streams    = new Stream[numBindings];

            for (int i = 0; i < numBindings; i++)
            {
                SyntaxSexp binding = (SyntaxSexp) bindingForms.get(i);
                SyntaxSymbol name = (SyntaxSymbol) binding.get(0);
                boundNames[i] = name.stringValue();

                SyntaxValue boundExpr = binding.get(1);

                FusionValue boundValue = eval.eval(env, boundExpr);
                streams[i] = Sequences.streamFor(boundValue);
            }

            FusionValue[] boundValues = new FusionValue[numBindings];
            Environment bodyEnv =
                new LocalEnvironment(env, boundNames, boundValues);

            while (Sequences.allHaveNext(streams))
            {
                // Determine the next round of bound values
                for (int i = 0; i < numBindings; i++)
                {
                    Stream s = streams[i];
                    boundValues[i] = s.next();
                }

                // Evaluate the body.
                Object nextResult = null;
                for (int i = 2; i < forStx.size(); i++)
                {
                    SyntaxValue bodyStx = forStx.get(i);
                    nextResult = eval.eval(bodyEnv, bodyStx);
                }

                IonValue value = toIonValue(nextResult);
                if (value != null)
                {
                    AddProc.invoke(result, value);
                }
                else
                {
                    throw contractFailure("body returned non-Ion value: "
                                          + writeToString(nextResult));
                }
            }
        }
        return new DomValue(result);
    }
}
