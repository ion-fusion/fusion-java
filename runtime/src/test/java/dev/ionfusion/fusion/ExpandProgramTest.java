// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionEval.expandProgram;
import static dev.ionfusion.fusion.FusionIo.isEof;
import static dev.ionfusion.fusion.FusionSexp.isSexp;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafeSexpSize;
import static dev.ionfusion.fusion.FusionSymbol.unsafeSymbolToJavaString;
import static dev.ionfusion.fusion.FusionSyntax.isIdentifier;
import static dev.ionfusion.fusion.FusionSyntax.isSyntax;
import static dev.ionfusion.fusion.FusionSyntax.unsafeFreeIdentifierEqual;
import static dev.ionfusion.fusion.FusionSyntax.unsafeSyntaxUnwrap;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ionfusion.commons.resources.ResourceDescriptor;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.embed.TopLevel;
import org.junit.jupiter.api.Test;


/**
 * Tests features used by the Eclipse plugin to traverse an expanded program.
 * In particular, it uses free_identifier_equal to compare bindings while
 * walking the expanded syntax tree.
 */
public class ExpandProgramTest
    extends CoreTestCase
{
    private static final class CoreFormCollector
        extends Procedure1
    {
        boolean receivedEof;
        Object lambdaId;
        Object moduleId;
        Object defValuesId;

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            if (isEof(eval, arg))
            {
                receivedEof = true;
            }
            else
            {
                // Once you go EOF, you can't go back.
                assertFalse(receivedEof);

                assertTrue(isSyntax(eval, arg));
                Object sexp = unsafeSyntaxUnwrap(eval, arg);
                assertTrue(isSexp(eval, sexp));
                assertTrue(unsafeSexpSize(eval, sexp) != 0);

                Object first = unsafePairHead(eval, sexp);
                assertTrue(isIdentifier(eval, first));
                Object sym  = unsafeSyntaxUnwrap(eval, first);
                String name = unsafeSymbolToJavaString(eval, sym);

                // In general one can't assume that an identifier named "lambda"
                // refers to the kernel's "lambda". But in this test case we
                // are processing source code that already has those core
                // forms. Actually this is still somewhat brittle since the
                // lambda exported from /fusion could be a macro, but it's okay
                // for our purposes here.

                switch (name)
                {
                    case "define_values":
                    {
                        defValuesId = first;
                        break;
                    }
                    case "lambda":
                    {
                        lambdaId = first;
                        break;
                    }
                    case "module":
                    {
                        moduleId = first;
                        break;
                    }
                }
            }

            return voidValue(eval);
        }
    }

    @Test
    public void testFindingCoreForms()
        throws Exception
    {
        TopLevel kernel = runtime().makeTopLevel("/fusion/private/kernel");

        Object kernelLambdaId = kernel.eval("(quote_syntax lambda)");
        Object kernelModuleId = kernel.eval("(quote_syntax module)");
        Object kernelDefValuesId = kernel.eval("(quote_syntax define_values)");

        CoreFormCollector collector = new CoreFormCollector();

        String source =
            "(lambda () 1) " +
            "(module M '/fusion' 1) " +
            "(define_values (s t) (values 1 2))";

        expandProgram(topLevel(), source, ResourceDescriptor.unknown(), collector);
        assertTrue(collector.receivedEof);


        Evaluator eval = evaluator();
        assertTrue(unsafeFreeIdentifierEqual(eval,
                                             collector.lambdaId,
                                             kernelLambdaId));
        assertTrue(unsafeFreeIdentifierEqual(eval,
                                             collector.moduleId,
                                             kernelModuleId));
        assertTrue(unsafeFreeIdentifierEqual(eval,
                                             collector.defValuesId,
                                             kernelDefValuesId));
    }
}
