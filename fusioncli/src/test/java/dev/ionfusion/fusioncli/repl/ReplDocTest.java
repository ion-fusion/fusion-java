// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import org.junit.jupiter.api.Test;

public class ReplDocTest
    extends ReplTestCase
{
    @Test
    public void docWithoutArg()
        throws Exception
    {
        supplyInput(",doc\n");
        runRepl();

        expectError("Expected an identifier");
    }


    @Test
    public void docForUnbound()
        throws Exception
    {
        supplyInput(",doc unbound_id\n");
        runRepl();

        // TODO Distinguish between unbound vars and undocumented bindings.
        expectError("No documentation available.");
    }


    @Test
    public void docForBuiltin()
        throws Exception
    {
        supplyInput(",doc  * \n");  // Extra space is intentional.
        runRepl();

        expectResponse("Returns the product");
    }


    @Test
    public void docForBadSyntax()
        throws Exception
    {
        supplyInput(",doc [\n");  // Extra space is intentional.
        runRepl();

        expectError("Expected an identifier");
    }


    @Test
    public void docForNonIdentifier()
        throws Exception
    {
        supplyInput(",doc 12\n");  // Extra space is intentional.
        runRepl();

        expectError("Expected an identifier");
    }
}
