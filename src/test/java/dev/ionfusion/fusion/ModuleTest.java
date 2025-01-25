// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class ModuleTest
    extends CoreTestCase
{
    @BeforeEach
    public void requires()
        throws FusionException
    {
        useTstRepo();
    }

    @Test
    public void testBadModuleSyntax()
        throws Exception
    {
        expectSyntaxExn("(module)");
        expectSyntaxExn("(module foo)");
        expectSyntaxExn("(module foo 12)");
        expectSyntaxExn("(module foo \"/fusion\")");  // No body

        expectSyntaxExn("(module \"foo\" \"/fusion\" true)");  // Bad name
        expectSyntaxExn("(module 1       \"/fusion\" true)");  // Bad name
    }



    @Test
    public void testBadLanguageSymbolInTopLevelModule()
    {
        assertEvalThrows(ModuleNotFoundException.class,
                         "(module m '/no_such_module' (define x 1))");
    }

    @Test
    public void testBadQuotedLanguageSymbolInTopLevelModule()
        throws Exception
    {
        expectSyntaxExn("(module m (quote 'no_such_module') (define x 1))");
    }

    @Test
    public void testBadLanguageStringInTopLevelModule()
    {
        assertEvalThrows(ModuleNotFoundException.class,
                         "(module m \"no_such_module\" (define x 1))");
    }

    @Test
    public void testRelativeLanguageStringInTopLevelModule()
    {
        // TODO Allow relative path to dialect?
        //  https://github.com/ion-fusion/fusion-java/issues/88
        assertEvalThrows(ModuleNotFoundException.class,
                         "(module m \"fusion\" (define x 1))");
    }

    @Test
    public void testTopLevelLanguageInTopLevelModule()
        throws Exception
    {
        eval("(module lang '/fusion/base' true)");

        // TODO Allow relative path to dialect?
        //  https://github.com/ion-fusion/fusion-java/issues/88
        assertEvalThrows(ModuleNotFoundException.class,
                        "(module m \"lang\" (define x 1))");
    }



    @Test
    public void testBadLanguageSymbolInRepoModule()
    {
        // TODO Root cause is ModuleNotFoundException; find and check it.
        assertEvalThrows(FusionException.class,
                         "(require '/module/bad_lang_symbol')");
    }

    @Test
    public void testBadLanguageSymbolInRepoModule2()
    {
        // TODO Root cause is ModuleNotFoundException; find and check it.
        assertEvalThrows(FusionException.class,
                         "(require \"/module/bad_lang_symbol\")");
    }

    @Test
    public void testBadQuotedLanguageSymbolInRepoModule()
    {
        // TODO Root cause is ModuleNotFoundException; find and check it.
        assertEvalThrows(FusionException.class,
                         "(require '/module/bad_quoted_lang_symbol')");
    }


    @Test
    public void testUseModuleWithNoProvides()
        throws Exception
    {
        eval("(require '/NoProvides')");
        expectUnboundIdentifierExn("X");
    }


    @Test
    public void testDuplicateDefinedName()
    {
        assertEvalThrows(FusionException.class,
                         "(require '/module/duplicate_defined_name')");
    }

    @Test
    public void testDuplicateImportedName()
    {
        assertEvalThrows(FusionException.class,
                         "(require \"/module/duplicate_imported_name\")");
    }

    @Test
    public void testDefineImportedName() {
        assertEvalThrows(AmbiguousBindingFailure.class,
                         "(require '/module/define_imported_name')");
    }

    @Test
    public void testImportDefinedName()
    {
        assertEvalThrows(FusionException.class,
                         "(require '/module/import_defined_name')");
    }


    @Test
    public void testInitialModuleImportsWithNoProvides()
        throws Exception
    {
        expectUnboundIdentifierExn("(module M '/NoProvides' X)");
    }


    @Test
    public void testBadRequireSyntax()
        throws Exception
    {
        expectSyntaxExn("(require)");
        expectSyntaxExn("(require {})");
        expectSyntaxExn("(require ())");
        expectSyntaxExn("(require (lib))");
        expectSyntaxExn("(require (lib \"/fusion/list\"))");
    }

    @Test
    public void testRequireNonModule()
    {
        assertEvalThrows(FusionException.class,
                         "(require \"/nonmodule/trivialDefine\")");
    }

    @Test
    public void testRequireUnderTop()
        throws Exception
    {
        String code =
            "(module m '/fusion'" +
            "  (if true (require '/fusion/list') false))";
        expectSyntaxExn(code);
    }

    /** Traps an infinite-loop bug in partial expansion. */
    @Test
    public void testBadSexpAtModuleLevel()
        throws Exception
    {
        eval("(module m '/fusion' (1))");
        assertThrows(FusionException.class,
                     () -> topLevel().requireModule("m"));
    }

    @Test
    public void testModuleAtTopLevel()
        throws Exception
    {
        eval("(module mod '/fusion'" +
             "  (define M 1054)" +
            "   (define N (lambda () (+ 1 M)))" +
             "  (provide M N))");
        expectSyntaxExn("M");

        eval("(require mod)");
        assertEval(1054, "M");
    }

    /**
     * We shouldn't fail `provide` at expand-time, since that may hide the
     * compile-time errors that caused provide to fail.
     */
    @Test
    public void testProvideFailsLate()
        throws Exception
    {
        expectUnboundIdentifierExn("(module x '/fusion'" +
                                   "  (require '/fusion/experimental/syntax')" +
                                   "  (define_syntax broken (lambda (s) x))" +
                                   "  (provide broken))");
    }


    @Test
    public void testModuleCircularity()
    {
        FusionException e =
            assertThrows(FusionException.class,
                         () -> topLevel().requireModule("/cycle0"));
        assertEquals("Module dependency cycle detected: /cycle1 -> /cycle2 -> /cycle3 -> /cycle1",
                     e.getBaseMessage());
    }


    @Test
    public void testTopLevelLocalModuleIsolation()
        throws Exception
    {
        TopLevel top1 = topLevel();
        TopLevel top2 = runtime().makeTopLevel();

        top1.eval("(module M '/fusion' (define v 1) (provide v))");
        top2.eval("(module M '/fusion' (define v 2) (provide v))");

        top1.eval("(require M)");
        top2.eval("(require M)");

        assertEval(top1, 1, "v");
        assertEval(top2, 2, "v");
    }
}
