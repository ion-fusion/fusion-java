// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printQuotedSymbol;

/**
 * Indicates an import or definition of an identifier that is already bound
 * and cannot be redefined.
 */
@SuppressWarnings("serial")
final class AmbiguousBindingFailure
    extends SyntaxException
{
    private AmbiguousBindingFailure(String message)
    {
        super(message);
    }


    /**
     * @param location may be null.
     */
    static AmbiguousBindingFailure makeAmbiguousBindingError(String whatForm,
                                                             String identifier,
                                                             SyntaxValue location)
    {
        String details = "The identifier " + printQuotedSymbol(identifier) +
                         " is already defined or imported from elsewhere";
        String message = composeMessage(whatForm, details);

        AmbiguousBindingFailure e = new AmbiguousBindingFailure(message);
        if (location != null)
        {
            e.addContext(location.getLocation());
        }
        return e;
    }

    /**
     * @param location is not part of the error message, but supplies the
     * innermost stack location; may be null.
     */
    static AmbiguousBindingFailure makeAmbiguousBindingError(String whatForm,
                                                             SyntaxSymbol identifier,
                                                             SyntaxValue location)
    {
        return makeAmbiguousBindingError(whatForm,
                                         identifier.stringValue(),
                                         location);
    }
}
