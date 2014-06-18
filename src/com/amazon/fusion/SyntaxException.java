// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionIo.safeWrite;
import com.amazon.ion.util.IonTextUtils;
import java.io.IOException;

/**
 * Indicates a compile-time syntax error.
 */
@SuppressWarnings("serial")
public class SyntaxException
    extends FusionException
{
    private final String myName;
    /** May be null. */
    private final SyntaxValue mySource;


    /**
     * @param whatForm may be null.
     * @param message must not be null.
     */
    SyntaxException(String whatForm, String message)
    {
        super(message);
        myName = whatForm;
        mySource = null;
    }

    /**
     * @param whatForm may be null.
     * @param message must not be null.
     * @param source the innermost continuation location; may be null.
     */
    SyntaxException(String whatForm, String message, SyntaxValue source)
    {
        super(message);
        myName = whatForm;
        mySource = source;

        if (mySource != null)
        {
            addContext(source.getLocation());
        }
    }


    @Override
    void displayMessage(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        out.append("Bad syntax");
        if (myName != null)
        {
            out.append(" for ");
            IonTextUtils.printQuotedSymbol(out, myName);
        }
        out.append(": ");
        super.displayMessage(eval, out);

        if (mySource != null)
        {
            out.append("\nSource: ");
            safeWrite(eval, out, mySource);
        }
    }
}
