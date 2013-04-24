// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

/**
 * Indicates a reference to an unbound identifier.
 */
@SuppressWarnings("serial")
final class UnboundIdentifierFailure
    extends SyntaxFailure
{
    UnboundIdentifierFailure(String name, SyntaxValue... exprs)
    {
        super(name, "unbound identifier", exprs);
    }
}
