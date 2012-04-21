// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionUtils.writeFriendlyIndex;
import com.amazon.ion.IonContainer;
import com.amazon.ion.IonSequence;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonValue;
import java.io.IOException;

/**
 *
 */
class DotFunction
    extends FunctionValue
{
    DotFunction()
    {
        //    "                                                                               |
        super("Traverses down through an Ion data structure.\n" +
              "CONTAINER must be Ion container (struct, list, or sexp).\n" +
              "Each PART must be a string, symbol, or int, to denote either a struct's\n" +
              "field-name or a sequence's index.",
              "container", "part", DOTDOTDOT);
    }

    @Override
    FusionValue invoke(Evaluator eval, FusionValue[] args)
        throws FusionException
    {
        checkArityAtLeast(1, args);
        IonContainer c = checkContainerArg(0, args);
        IonValue value = c;

        final int lastArg = args.length - 1;
        for (int i = 1; i <= lastArg; i++)
        {
            switch (c.getType())
            {
                case LIST:
                case SEXP:
                {
                    long index = checkLongArg(i, args);
                    if (c.size() <= index)
                    {
                        return UNDEF;
                    }
                    IonSequence s = (IonSequence) c;
                    value = s.get((int) index);
                    break;
                }
                case STRUCT:
                {
                    String field = checkTextArg(i, args);
                    IonStruct s = (IonStruct) c;
                    value = s.get(field);
                    break;
                }
                default:
                {
                    throw new IllegalStateException();
                }
            }

            if (value == null) return UNDEF;

            if (i < lastArg)
            {
                try
                {
                    c = (IonContainer) value;
                }
                catch (ClassCastException cce)
                {
                    StringBuilder out = new StringBuilder();
                    try {
                        identify(out);
                        out.append(" expects container before traversing ");
                        writeFriendlyIndex(out, i + 1);
                        out.append(" argument, had: ");
                        FusionUtils.writeIon(out, value);
                        out.append("\nArguments were:");
                        for (FusionValue arg : args)
                        {
                            out.append("\n  ");
                            arg.write(out);
                        }
                    }
                    catch (IOException ioe) {}
                    String message = out.toString();
                    throw new ContractFailure(message);
                }
            }
        }

        return new DomValue(value);
    }

}
