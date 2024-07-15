// Copyright (c) 2012-2024 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.util.IonTextUtils;

/**
 * Indicates a reference to an unbound identifier.
 */
@SuppressWarnings("serial")
public final class UnboundIdentifierException
    extends SyntaxException
{
    private final String myText;


    /**
     * @param identifier must not be null.
     */
    UnboundIdentifierException(SyntaxSymbol identifier)
    {
        super(null, "", identifier);
        myText = identifier.stringValue();
    }

    @Override
    String getBaseMessage()
    {
        return "unbound identifier. The symbol " + IonTextUtils.printQuotedSymbol(myText) +
                   " has no binding where it's used, so check for correct spelling and imports.";
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
