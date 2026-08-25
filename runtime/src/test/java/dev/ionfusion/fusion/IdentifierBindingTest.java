// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.isEof;
import static dev.ionfusion.fusion.FusionSymbol.unsafeSymbolToJavaString;
import static dev.ionfusion.fusion.FusionSyntax.isIdentifier;
import static dev.ionfusion.fusion.FusionSyntax.unsafeIdentifierBinding;
import static dev.ionfusion.fusion.FusionSyntax.unsafeSyntaxUnwrap;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ResourcePosition;
import dev.ionfusion.runtime.base.SourceName;
import org.junit.jupiter.api.Test;


/**
 * Tests the unsafeIdentifierBinding Function. It's mainly being used to
 * travel to the binding site of a reference.
 */
public class IdentifierBindingTest
    extends CoreTestCase
{
    private static final class BasicTraversalProc
        extends Procedure1
    {
        Object topLevelReference;
        Object moduleReference;
        Object renamedModuleReference;

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            if (isEof(eval, arg))
            {
                return voidValue(eval);
            }

            if (isIdentifier(eval, arg))
            {
                Object sym = unsafeSyntaxUnwrap(eval, arg);
                String name = unsafeSymbolToJavaString(eval, sym);
                switch (name)
                {
                    case "topBinding":
                    {
                        topLevelReference = arg;
                        break;
                    }
                    case "bar":
                    {
                        renamedModuleReference = arg;
                        break;
                    }
                    case "foo":
                    {
                        moduleReference = arg;
                    }
                }
            }

            return voidValue(eval);
        }
    }

    @Test
    public void testBasicAndRenameOutRequireBindings()
        throws Exception
    {
        Evaluator eval = evaluator();

        BasicTraversalProc traversal = new BasicTraversalProc();

        String module =
            "(module test_module '/fusion' \n" +
            "  (define (foo x)             \n" +
            "    10)                       \n" +
            "  (provide foo                \n" +
            "    (rename_out (foo bar))))    ";

        String source =
            "(require test_module)    \n" +
            "bar                      \n" + // <- renamedModuleReference
            "(define topBinding 10)   \n" +
            "(let [(topBinding 5700)] \n" + // a shadow
            "  topBinding)            \n" +
            "topBinding               \n" + // <- topLevelReference
            "foo";                          // <- moduleReference

        eval(topLevel(), module);
        SourceName sourceName = SourceName.forDisplay("TestFile");
        FusionEval.expandProgram(topLevel(), source, sourceName, traversal);


        BindingSite topLevelSite =
            unsafeIdentifierBinding(eval,
                                    traversal.topLevelReference);
        assertBindingAt(3, 9, topLevelSite);
        assertTrue(topLevelSite.isDefinitionSite());
        assertNull(topLevelSite.nextSite());
        assertSame(sourceName, topLevelSite.getPosition().getResourceDesc());


        // We expect the BindingSite's SourceLocation to be null because
        // there is no local reference for the binding to go to. Thus, we'll jump
        // to the export site using getModuleBindingInformation after confirming
        // that this is a required binding.
        BindingSite moduleProvidedBinding =
            unsafeIdentifierBinding(eval,
                                    traversal.moduleReference);
        assertNull(moduleProvidedBinding.getPosition());
        assertTrue(moduleProvidedBinding.isImportSite());
        assertBindingAt(4, 12,
                        moduleProvidedBinding.nextSite());


        BindingSite renameProvidedBinding =
            unsafeIdentifierBinding(eval,
                                    traversal.renamedModuleReference);
        assertNull(renameProvidedBinding.getPosition());
        assertTrue(renameProvidedBinding.isImportSite());
        assertBindingAt(5, 22, renameProvidedBinding.nextSite());


        BindingSite moduleDefinedBinding = moduleProvidedBinding.target();
        assertBindingAt(2, 12, moduleDefinedBinding);


        BindingSite renamedDefinedBinding = renameProvidedBinding.target();
        assertEquals(moduleDefinedBinding.getPosition(),
                     renamedDefinedBinding.getPosition());

        // TODO test chained renames
        // TODO test local bindings
    }


    @Test
    public void testOnlyInAndAllDefinedOutBindings()
        throws Exception
    {
        BasicTraversalProc traversal = new BasicTraversalProc();

        String onlyInModule =
            "(module only_in_module \"/fusion\"\n" +
            "  (define foo 75)\n" +
            "  (provide (all_defined_out)))";

        String enclosingModule =
            "(module enclosing \"/fusion\"\n" +
            "  (require \"only_in_module\")\n" +
            "  (provide foo))";

        String source =
            "(require (only_in \"enclosing\" foo))\n" +
            "foo";

        eval(topLevel(), onlyInModule);
        eval(topLevel(), enclosingModule);
        FusionEval.expandProgram(topLevel(), source, null, traversal);


        BindingSite onlyInBinding =
            unsafeIdentifierBinding(evaluator(),
                                    traversal.moduleReference);
        assertBindingAt(1, 31, onlyInBinding);


        BindingSite enclosingProvide =
            onlyInBinding.nextSite();
        assertBindingAt(3, 12, enclosingProvide);


        BindingSite definition = onlyInBinding.target();
        assertBindingAt(2, 11, definition);
        assertNotEquals(enclosingProvide.getPosition(),
                        definition.getPosition());
    }


    private void assertBindingAt(int         expectedSourceLine,
                                 int         expectedSourceColumn,
                                 BindingSite bindingInfo)
    {
        ResourcePosition locn = bindingInfo.getPosition();
        assertEquals(expectedSourceLine,   locn.getLine());
        assertEquals(expectedSourceColumn, locn.getColumn());
    }
}
