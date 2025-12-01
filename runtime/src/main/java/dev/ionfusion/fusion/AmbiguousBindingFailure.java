// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printQuotedSymbol;

/**
 * Indicates an import of an identifier that is already bound.
 */
@SuppressWarnings("serial")
final class AmbiguousBindingFailure
    extends SyntaxException
{
    public AmbiguousBindingFailure(String whatForm, String identifier)
    {
        super(whatForm,
              "The identifier " + printQuotedSymbol(identifier) +
              " is already defined or imported from elsewhere");
    }

    public AmbiguousBindingFailure(String whatForm, String identifier,
                                   SyntaxValue expr)
    {
        this(whatForm, identifier);
        addContext(expr);
    }
}
