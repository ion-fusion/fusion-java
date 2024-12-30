// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionVoid.isVoid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.amazon.ion.IonInt;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.SymbolTable;
import com.amazon.ion.system.IonBinaryWriterBuilder;
import com.amazon.ion.system.SimpleCatalog;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

public class RuntimeTest
    extends CoreTestCase
{
    @Test
    public void testModuleInUserRepository()
        throws Exception
    {
        useTstRepo();
        eval("(require '''/grain''')");
        assertString("soup", "barley");
    }


    @Test
    public void testLoadFile()
        throws Exception
    {
        checkString("hello", loadFile("tst-data/hello.ion"));

        // Test that eval'd define affects the visible namespace
        loadFile("tst-data/trivialDefine.fusion");
        assertEval(3328, "x");

        // Test loading a script with top-level modules
        loadFile(ftstScriptDirectory().resolve("topmodules.test.fusion"));
    }



    @Test
    public void testVoidReturn()
        throws Exception
    {
        Object fv = eval("(void)");
        assertTrue(isVoid(topLevel(), fv));
    }


    @Test
    public void testIonize()
        throws Exception
    {
        Object fv = eval("12");
        IonValue iv = runtime().ionizeMaybe(fv, system());
        assertEquals(12, ((IonInt)iv).intValue());
        iv = runtime().ionize(fv, system());
        assertEquals(12, ((IonInt)iv).intValue());

        fv = eval("(lambda () 12)");
        assertSame(null, runtime().ionizeMaybe(fv, system()));
    }

    @Test
    public void testBadIonizeProcedure()
        throws Exception
    {
        Object fv = eval("(lambda () 12)");
        assertThrows(FusionException.class,
                     () -> runtime().ionize(fv, system()));
    }

    @Test
    public void testBadIonizeVoid()
        throws Exception
    {
        Object fv = eval("(void)");
        assertThrows(FusionException.class,
                     () -> runtime().ionize(fv, system()));
    }


    @Test
    public void testModuleRegistration()
        throws Exception
    {
        final ModuleIdentity id = ModuleIdentity.forAbsolutePath("/tst/dummy");
        assertSame(id, ModuleIdentity.forAbsolutePath("/tst/dummy"));

        ModuleBuilder builder = runtime().makeModuleBuilder("/tst/dummy");
        builder.instantiate();

        topLevel().define("callback", new Procedure0()
        {
            @Override
            Object doApply(Evaluator eval)
                throws FusionException
            {
                ModuleRegistry registry =
                    eval.findCurrentNamespace().getRegistry();
                ModuleInstance mod = registry.lookup(id);
                assertNotNull(mod);

                return null;
            }
        });

        eval("(callback)");


        // Test registering two instances w/ same identity
        builder = runtime().makeModuleBuilder("/tst/dummy");
        try {
            builder.instantiate();
            fail("expected exception");
        }
        catch (ContractException e) { }


        try {
            runtime().makeModuleBuilder("dummy");
            fail("expected exception");
        }
        catch (IllegalArgumentException e) { }

        try {
            runtime().makeModuleBuilder("dum/my");
            fail("expected exception");
        }
        catch (IllegalArgumentException e) { }
    }

    @Test
    public void testTopLevel()
        throws Exception
    {
        useTstRepo();
        runtime().makeTopLevel("/let");
    }


    @Test
    public void testEvalUsesCurrentIonReaderValue()
        throws Exception
    {
        IonReader r = system().newReader("(define a 338) a");
        Object result = topLevel().eval(r);
        checkLong(338, result);
    }


    //========================================================================
    // loadModule()

    private static final String GOOD_MODULE =
        "(module m '/fusion' (define x 1115) (provide x))";

    @Test
    public void testLoadModule()
        throws Exception
    {
        topLevel().loadModule("/local/manual",
                              system().newReader(GOOD_MODULE),
                              SourceName.forDisplay("manual source"));

        topLevel().requireModule("/local/manual");
        assertEval(1115, "x");
    }

    @Test
    public void testLoadModuleOnCurrentValue()
        throws Exception
    {
        IonReader reader = system().newReader(GOOD_MODULE);
        reader.next();

        topLevel().loadModule("/local/manual",
                              reader,
                              SourceName.forDisplay("manual source"));

        topLevel().requireModule("/local/manual");
        assertEval(1115, "x");
    }

    @Test
    public void testLoadModuleNoName()
        throws Exception
    {
        topLevel().loadModule("/local/manual",
                              system().newReader(GOOD_MODULE),
                              null);
        topLevel().requireModule("/local/manual");
        assertEval(1115, "x");
    }

    @Test
    public void testLoadModuleNullPath()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> topLevel().loadModule(null,
                                                 system().newReader(GOOD_MODULE),
                                                 SourceName.forDisplay("manual source")));
    }

    @Test
    public void testLoadModuleBadPath()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> topLevel().loadModule("/a bad path",
                                                 system().newReader(GOOD_MODULE),
                                                 SourceName.forDisplay("manual source")));
    }

    @Test
    public void testLoadModuleThatsAlreadyRegistered()
        throws Exception
    {
        topLevel().loadModule("/local/manual",
                              system().newReader(GOOD_MODULE),
                              SourceName.forDisplay("manual source"));
        try
        {
            topLevel().loadModule("/local/manual",
                                  system().newReader(GOOD_MODULE),
                                  SourceName.forDisplay("manual source"));
            fail("Expected exception");
        }
        catch (FusionException e) { }
    }

    @Test
    public void testLoadModuleNoContent()
        throws Exception
    {
        String modulePath = "/local/manual";
        SourceName source = SourceName.forDisplay("/path/to/blah");
        String moduleContent = "/* nothing */";

        Exception e =
            assertThrows(SyntaxException.class,
                         () -> topLevel().loadModule(modulePath,
                                                     system().newReader(moduleContent),
                                                     source));

        assertThat(e.getMessage(),
                   allOf(containsString("no top-level forms"),
                         containsString(source.display())));
    }

    @Test
    public void testLoadModuleExtraContent()
        throws Exception
    {
        String modulePath = "/local/manual";
        SourceName source = SourceName.forDisplay("/path/to/blah");
        String moduleContent = "(module m '/fusion' true) extra_data";

        Exception e =
            assertThrows(SyntaxException.class,
                         () -> topLevel().loadModule(modulePath,
                                                     system().newReader(moduleContent),
                                                     source));

        assertThat(e.getMessage(),
                   allOf(containsString("more than one top-level form"),
                         containsString(source.display())));
    }


    @Test
    public void testLoadModuleWrongForm()
        throws Exception
    {
        String modulePath = "/local/manual";
        SourceName source = SourceName.forDisplay("/path/to/blah");
        String moduleContent = " (if true 1 2)";

        Exception e =
            assertThrows(SyntaxException.class,
                         () -> topLevel().loadModule(modulePath,
                                                     system().newReader(moduleContent),
                                                     source));

        assertThat(e.getMessage(),
                   allOf(containsString("Top-level form isn't (module ...)"),
                         containsString("1st line, 2nd column"),
                         containsString(source.display())));
    }


    //========================================================================
    // Default Ion Catalog

    private SymbolTable newSharedSymbolTable(String name,
                                             int version,
                                             String... symbols)
    {
        Iterator<String> symIter = Arrays.asList(symbols).iterator();
        return system().newSharedSymbolTable(name, version, symIter);
    }

    private byte[] encode(SymbolTable symtab, IonValue data)
        throws IOException
    {
        IonBinaryWriterBuilder bwb =
            IonBinaryWriterBuilder.standard().withImports(symtab);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (IonWriter writer = bwb.build(out))
        {
            data.writeTo(writer);
        }
        return out.toByteArray();
    }

    @Test
    public void testReadingIonWithSharedSymtabs()
        throws Exception
    {
        SimpleCatalog catalog = new SimpleCatalog();
        runtimeBuilder().setDefaultIonCatalog(catalog);

        SymbolTable symtab = newSharedSymbolTable("flatware", 1,
                                                  "fork", "spoon", "knife");
        catalog.putTable(symtab);


        IonValue data = system().singleValue("{ spoon: knife::fork }");
        byte[] encodedData = encode(symtab, data);


        Object readProc = topLevel().lookup("read");
        Object decoded = topLevel().call("with_ion_from_lob", encodedData, readProc);
        checkIon(data, decoded);


        // Same data should fail w/o symtab
        assertSame(symtab, catalog.removeTable("flatware", 1));
        Throwable e =
            assertThrows(FusionException.class,
                         () -> topLevel().call("with_ion_from_lob",
                                               encodedData,
                                               readProc));
        assertTrue(e.getMessage().contains("$11"));
    }
}
