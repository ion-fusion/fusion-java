// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * See parameter.test.fusion
 */
public class ParameterTest
    extends CoreTestCase
{
    @BeforeEach
    public void requires()
        throws Exception
    {
        topLevel().requireModule("/fusion/parameter");
    }

    @Test
    public void testParameterArity()
        throws Exception
    {
        expectArityExn("(make_parameter)");
        expectArityExn("(make_parameter 1 2)");
    }

    @Test
    public void testParameterizeSyntax()
        throws Exception
    {
        eval("(define p (make_parameter 1))");

        expectSyntaxExn("(parameterize)");
        expectSyntaxExn("(parameterize ((p 1)))");
        expectSyntaxExn("(parameterize 12 13)");
        expectSyntaxExn("(parameterize null.sexp 13)");
        expectSyntaxExn("(parameterize (12) 13)");
        expectSyntaxExn("(parameterize (1 2) 13)");
        expectSyntaxExn("(parameterize (()) 13)");
        expectSyntaxExn("(parameterize ((12)) 13)");
        expectSyntaxExn("(parameterize ((p)) 13)");
        expectSyntaxExn("(parameterize ((p 1 2)) 13)");
        expectSyntaxExn("(parameterize ((p 1) ()) 13)");
        expectSyntaxExn("(parameterize ((p 1) (p2)) 13)");

        expectContractExn("(parameterize ((1 2)) 13)");
    }
}
