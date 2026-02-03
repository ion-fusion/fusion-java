// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printQuotedSymbol;

/**
 * Indicates a reference to an unbound identifier.
 */
@SuppressWarnings("serial")
public final class UnboundIdentifierException
    extends SyntaxException
{
    private final String myText;


    /**
     * @param name must not be null.
     */
    private UnboundIdentifierException(String message, String name)
    {
        super(message);
        myText = name;
    }


    static UnboundIdentifierException makeUnboundError(SyntaxSymbol identifier)
    {
        String name = identifier.stringValue();
        String details =
            "unbound identifier. The symbol " + printQuotedSymbol(name) +
            " has no binding where it's used, so check for correct spelling and imports.";
        String message = composeMessage(details);

        UnboundIdentifierException e = new UnboundIdentifierException(message, name);
        e.addContext(identifier.getLocation());
        return e;
    }

    /**
     * Gets the text of the unbound identifier.
     *
     * @return the variable name
     */
    public String getIdentifierString()
    {
        return myText;
    }
}
