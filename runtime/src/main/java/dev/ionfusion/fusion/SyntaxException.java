// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printQuotedSymbol;
import static dev.ionfusion.fusion.FusionIo.safeWrite;

/**
 * Indicates a compile-time syntax error.
 */
@SuppressWarnings("serial")
public class SyntaxException
    extends FusionErrorException
{
    SyntaxException(String message)
    {
        super(message);
    }


    static String composeMessage(Evaluator eval,
                                 String whatForm,
                                 String details,
                                 SyntaxValue source)
    {
        StringBuilder out = new StringBuilder();
        out.append("Bad syntax");

        if (whatForm != null)
        {
            out.append(" for ");
            out.append(printQuotedSymbol(whatForm));
        }
        out.append(": ");
        out.append(details);

        if (source != null)
        {
            out.append("\nSource: ");
            safeWrite(eval, out, source);
        }

        return out.toString();
    }

    static String composeMessage(String formName, String details)
    {
        return composeMessage(null, formName, details, null);
    }


    static String composeMessage(String details)
    {
        return composeMessage(null, null, details, null);
    }


    /**
     * @param eval may be null IFF {@code source} is null.
     * @param whatForm may be null.
     * @param details must not be null.
     * @param source is written in the error message and supplies the innermost
     * stack location; may be null.
     */
    static SyntaxException makeSyntaxError(Evaluator eval,
                                           String whatForm,
                                           String details,
                                           SyntaxValue source)
    {
        String message = composeMessage(eval, whatForm, details, source);
        SyntaxException e = new SyntaxException(message);
        if (source != null)
        {
            e.addContext(source.getLocation());
        }
        return e;
    }


    /**
     * @param details must not be null.
     */
    static SyntaxException makeSyntaxError(String details)
    {
        String message = composeMessage(details);
        return new SyntaxException(message);
    }
}
