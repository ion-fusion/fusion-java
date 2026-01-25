// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printString;
import static dev.ionfusion.fusion.FusionIo.isEof;
import static dev.ionfusion.fusion.FusionIo.read;
import static dev.ionfusion.fusion.FusionStruct.isImmutableStruct;
import static dev.ionfusion.fusion.FusionStruct.unsafeStructSize;
import static dev.ionfusion.fusion.TestSetup.testDataDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.ion.IonList;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import dev.ionfusion.runtime.embed.TopLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class FusionIoTest
    extends CoreTestCase
{
    @BeforeEach
    public void requires()
        throws Exception
    {
        useTstRepo();
        topLevel().requireModule("/fusion/eval");
        topLevel().requireModule("/fusion/io");
        topLevel().requireModule("/testutils");
    }


    @Test
    public void testReadMoreArgs()
    {
        assertEvalThrows(ArityFailure.class,"(read \"hi\")");
    }


    @Test
    public void testCurrentDirectory()
        throws Exception
    {
        String userDir = System.getProperty("user.dir");
        assertEval(printString(userDir), "(current_directory)");

        String newDir = testDataDirectory().toString();
        assertEval("\"hello\"",
                   "(parameterize" +
                   "  ((current_directory " + printString(newDir) + "))" +
                   "  (load \"hello.ion\"))");
    }


    @Test
    public void testIonizeGoesToStdout()
        throws Exception
    {
        eval("(ionize 1)");
        assertEquals("\n1", stdoutToString());
    }

    @Test
    public void testWriteGoesToStdout()
        throws Exception
    {
        eval("(write 1)");
        assertEquals("1", stdoutToString());
    }

    @Test
    public void testDisplayGoesToStdout()
        throws Exception
    {
        eval("(display 1)");
        assertEquals("1", stdoutToString());
    }



    @Test
    public void testLoadCurrentNamespace()
        throws Exception
    {
        eval("(load (test_data_file \"trivialDefine.fusion\"))");
        assertEval(3328, "x");
    }

    @Test
    public void testBadLoadCalls()
        throws Exception
    {
        expectArityExn("(load)");
        expectArityExn("(load \"x\" \"y\")");

        expectContractExn("(load 12)");
    }


    @Test
    public void testFfiRead()
        throws Exception
    {
        TopLevel  top  = topLevel();
        Evaluator eval = evaluator();

        IonReader reader = system().newReader("{}");
        Object fv = read(top, reader);
        assertTrue(isImmutableStruct(eval, fv));
        assertEquals(0, unsafeStructSize(eval, fv));
        fv = read(top, reader);
        assertTrue(isEof(top, fv));

        reader = system().newReader("{f:9} 10");
        reader.next();
        fv = FusionIo.read(top, reader);
        assertTrue(isImmutableStruct(eval, fv));
        assertEquals(1, unsafeStructSize(eval, fv));
        fv = read(top, reader);
        checkLong(10, fv);
        fv = read(top, reader);
        assertTrue(isEof(top, fv));
        fv = read(top, reader);
        assertTrue(isEof(top, fv));  // EOF "sticks"
    }


    /**
     * This attempts to have interesting combinations of data to trigger
     * various injection cases.
     */
    private static final String INJECTION_SAMPLE =
        "[   [],    (),    {},  " +
        " a::[], b::(), c::{},  " +
        " d::[ 1,               " +
        "      { f:null },      " +
        "      e::{ g:(sym) },  " +
        "      ('''str'''),     " +
        "    ],                 " +
        "]";


    private void writeInjectedDom(String ion)
        throws Exception
    {
        TopLevel  top  = topLevel();

        IonValue iv = system().singleValue(INJECTION_SAMPLE);
        Object fv = top.call("identity", iv);  // inject the value

        StringBuilder sb = new StringBuilder();

        FusionIo.write(top, fv, sb);

        assertEquals(iv, system().singleValue(sb.toString()));
    }

    @Test
    public void testWriteInjectedDom()
        throws Exception
    {
        writeInjectedDom(INJECTION_SAMPLE);
        writeInjectedDom("a::" + INJECTION_SAMPLE);
    }


    private void ionizeInjectedDom(String ion)
        throws Exception
    {
        TopLevel  top  = topLevel();

        IonValue iv = system().singleValue(ion);
        Object fv = top.call("identity", iv);  // inject the value

        IonList container = system().newEmptyList();
        IonWriter iw = system().newWriter(container);

        FusionIo.ionize(top, fv, iw);

        assertEquals(iv, container.get(0));
    }

    @Test
    public void testIonizeInjectedDom()
        throws Exception
    {
        ionizeInjectedDom(INJECTION_SAMPLE);
        ionizeInjectedDom("a::" + INJECTION_SAMPLE);
    }
}
