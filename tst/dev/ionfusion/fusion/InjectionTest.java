// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.isTrue;
import static dev.ionfusion.fusion.FusionValue.isAnyNull;
import static dev.ionfusion.fusion.FusionVoid.isVoid;
import static dev.ionfusion.fusion.NumericsTest.VERY_BIG_INTEGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazon.ion.Decimal;
import com.amazon.ion.IonBlob;
import com.amazon.ion.IonBool;
import com.amazon.ion.IonClob;
import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonDecimal;
import com.amazon.ion.IonFloat;
import com.amazon.ion.IonInt;
import com.amazon.ion.IonList;
import com.amazon.ion.IonNull;
import com.amazon.ion.IonSexp;
import com.amazon.ion.IonString;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSymbol;
import com.amazon.ion.IonTimestamp;
import com.amazon.ion.IonValue;
import com.amazon.ion.Timestamp;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class InjectionTest
    extends CoreTestCase
{
    private static final byte[] BYTES = { 0, 1, 2, 3 };


    @Test
    public void testNullInjection()
        throws Exception
    {
        Object fv = topLevel().call("identity", (Object) null);
        assertTrue(isVoid(topLevel(), fv));

        // Check that define() injects the given value.
        topLevel().define("v", null);
        fv = topLevel().eval("(identity v)");
        assertTrue(isVoid(topLevel(), fv));
    }


    @Test
    public void testBoolInjection()
        throws Exception
    {
        Object fv = topLevel().call("=", true, Boolean.FALSE);
        assertFalse(isTrue(topLevel(), fv));

        topLevel().define("v", true);
        fv = topLevel().lookup("v");
        assertTrue(isTrue(topLevel(), fv));
    }


    @Test
    public void testIntInjection()
        throws Exception
    {
        TopLevel top = topLevel();

        // For each case we inject the Java type, make sure it round-trips,
        // and make sure it can be used in an expression.

        top.define("v", 22);
        Object fv = top.call("<", 22, BigInteger.valueOf(23));
        assertTrue(isTrue(top, fv));

        // Inject Byte
        top.define("v", Byte.MAX_VALUE);
        assertEval(Byte.MAX_VALUE, "v");
        assertEval(Byte.MAX_VALUE * 3, "(* 3 v)");

        // Inject Short
        top.define("v", Short.MAX_VALUE);
        assertEval(Short.MAX_VALUE, "v");
        assertEval(Short.MAX_VALUE * 2, "(* 2 v)");

        // Inject Integer
        top.define("v", 22);
        assertEval(22, "v");
        assertEval(44, "(* 2 v)");

        // Inject Long
        top.define("v", Long.MAX_VALUE);
        assertEval(Long.MAX_VALUE, "v");
        assertEval(Long.MAX_VALUE - 5, "(- v 5)");

        // Inject BigInteger
        top.define("v", VERY_BIG_INTEGER);
        assertEval(VERY_BIG_INTEGER, "v");
        assertEval(VERY_BIG_INTEGER.multiply(BigInteger.valueOf(2)),
                   "(* 2 v)");
    }


    @Test
    public void testFloatInjection()
        throws Exception
    {
        TopLevel top = topLevel();

        top.define("v", 123.2);
        assertEval(123.2, "v");
//      assertEval(246.4, "(+ v v)");
    }


    @Test
    public void testDecimalInjection()
        throws Exception
    {
        TopLevel top = topLevel();

        BigDecimal d = BigDecimal.valueOf(420);
        top.define("v", d);
        assertEval(d, "v");
        assertEval(d.add(d), "(+ v v)");
    }


    @Test
    public void testBlobInjection()
        throws Exception
    {
        TopLevel top = topLevel();

        top.define("v", BYTES);
        assertEval(system().newBlob(BYTES), "v");
    }


    //========================================================================
    // Injection IonValues


    void testIonValueInjection(IonValue iv, String predicate)
        throws Exception
    {
        TopLevel top = topLevel();

        top.define("v", iv);
        assertEval(iv, "v");
        Object fv = eval("v");
        assertEquals(iv.isNullValue(), isAnyNull(top, fv));
        assertEval(true, "(" + predicate + " v)");
        fv.toString();                // Ensure that toString() doesn't throw.

        // App must not modify an injected object, so we clone.
        iv = iv.clone();
        iv.setTypeAnnotations("a");
        top.define("v", iv);
        assertEval(iv, "v");
        fv = eval("v");
        assertEquals(iv.isNullValue(), isAnyNull(top, fv));
        fv.toString();

        {
            IonList c = system().newList(iv.clone());
            top.define("c", c);
            assertEval(1, "(size c)");
            assertEval(c, "c");
            fv = eval("c");
            fv.toString();
            assertEval(iv, "(element c 0)");
            fv = eval("(element c 0)");
            assertEquals(iv.isNullValue(), isAnyNull(top, fv));
            fv.toString();

            iv = iv.clone();
            iv.setTypeAnnotations();
            c = system().newList(iv);
            top.define("c", c);
            assertEval(1, "(size c)");
            assertEval(c, "c");
            fv = eval("c");
            fv.toString();
            assertEval(iv, "(element c 0)");
            fv = eval("(element c 0)");
            assertEquals(iv.isNullValue(), isAnyNull(top, fv));
            fv.toString();

            c = c.clone();
            c.setTypeAnnotations("ann");
            top.define("c", c);
            assertEval("[ann]", "(annotations c)");
        }

        {
            IonSexp c = system().newSexp(iv.clone());
            top.define("c", c);
            assertEval(1, "(size c)");
            assertEval(c, "c");
            fv = eval("c");
            fv.toString();
            assertEval(iv, "(element c 0)");
            fv = eval("(element c 0)");
            assertEquals(iv.isNullValue(), isAnyNull(top, fv));
            fv.toString();

            iv = iv.clone();
            iv.setTypeAnnotations();
            c = system().newSexp(iv);
            top.define("c", c);
            assertEval(1, "(size c)");
            assertEval(c, "c");
            fv = eval("c");
            fv.toString();
            assertEval(iv, "(element c 0)");
            fv = eval("(element c 0)");
            assertEquals(iv.isNullValue(), isAnyNull(top, fv));
            fv.toString();

            c = c.clone();
            c.setTypeAnnotations("ann");
            top.define("c", c);
            assertEval("[ann]", "(annotations c)");
        }

        {
            IonStruct c = system().newEmptyStruct();
            c.put("f", iv.clone());
            top.define("c", c);
            assertEval(1, "(size c)");
            assertEval(c, "c");
            fv = eval("c");
            fv.toString();
            assertEval(iv, "(element c '''f''')");
            fv = eval("(element c '''f''')");
            assertEquals(iv.isNullValue(), isAnyNull(top, fv));
            fv.toString();

            iv = iv.clone();
            iv.setTypeAnnotations();
            c = system().newEmptyStruct();
            c.put("f", iv);
            top.define("c", c);
            assertEval(1, "(size c)");
            assertEval(c, "c");
            fv = eval("c");
            fv.toString();
            assertEval(iv, "(element c '''f''')");
            fv = eval("(element c '''f''')");
            assertEquals(iv.isNullValue(), isAnyNull(top, fv));
            fv.toString();

            c = c.clone();
            c.setTypeAnnotations("ann");
            top.define("c", c);
            assertEval("[ann]", "(annotations c)");
        }
    }


    @Test
    public void testIonNullInjection()
        throws Exception
    {
        IonNull iv = system().newNull();
        testIonValueInjection(iv, "is_null_null");
    }


    @Test
    public void testIonBoolInjection()
        throws Exception
    {
        IonBool iv = system().newNullBool();
        testIonValueInjection(iv, "is_bool");

        iv = system().newBool(false);
        testIonValueInjection(iv, "is_bool");

        iv = system().newBool(true);
        testIonValueInjection(iv, "is_bool");
    }


    @Test
    public void testIonIntInjection()
        throws Exception
    {
        IonInt iv = system().newNullInt();
        testIonValueInjection(iv, "is_int");

        iv = system().newInt(Long.MAX_VALUE);
        testIonValueInjection(iv, "is_int");

        iv = system().newInt(NumericsTest.VERY_BIG_INTEGER);
        testIonValueInjection(iv, "is_int");
    }


    @Test
    public void testIonFloatInjection()
        throws Exception
    {
        IonFloat iv = system().newNullFloat();
        testIonValueInjection(iv, "is_float");

        iv = system().newFloat(Float.MAX_VALUE);
        testIonValueInjection(iv, "is_float");
    }


    @Test
    public void testIonDecimalInjection()
        throws Exception
    {
        IonDecimal iv = system().newNullDecimal();
        testIonValueInjection(iv, "is_decimal");

        iv = system().newDecimal(12345);
        testIonValueInjection(iv, "is_decimal");

        Decimal negativeZero = Decimal.negativeZero(1);
        iv = system().newDecimal(negativeZero);
        testIonValueInjection(iv, "is_decimal");
        assertEval(true, "(= 0 v)");
    }


    @Test
    public void testIonTimestampInjection()
        throws Exception
    {
        IonTimestamp iv = system().newNullTimestamp();
        testIonValueInjection(iv, "is_timestamp");

        iv = system().newTimestamp(Timestamp.forDay(2013, 12, 2));
        testIonValueInjection(iv, "is_timestamp");
    }


    @Test
    public void testIonSymbolInjection()
        throws Exception
    {
        IonSymbol iv = system().newNullSymbol();
        testIonValueInjection(iv, "is_symbol");

        iv = system().newSymbol("vanilla soy mocha");
        testIonValueInjection(iv, "is_symbol");
    }


    @Test
    public void testIonStringInjection()
        throws Exception
    {
        IonString iv = system().newNullString();
        testIonValueInjection(iv, "is_string");

        iv = system().newString("pumpkin spice latte");
        testIonValueInjection(iv, "is_string");
    }


    @Test
    public void testIonClobInjection()
        throws Exception
    {
        IonClob iv = system().newNullClob();
        testIonValueInjection(iv, "is_clob");

        iv = system().newClob(BYTES);
        testIonValueInjection(iv, "is_clob");
    }


    @Test
    public void testIonBlobInjection()
        throws Exception
    {
        IonBlob iv = system().newNullBlob();
        testIonValueInjection(iv, "is_blob");

        iv = system().newBlob(BYTES);
        testIonValueInjection(iv, "is_blob");
    }


    @Test
    public void testIonListInjection()
        throws Exception
    {
        IonList iv = system().newNullList();
        testIonValueInjection(iv, "is_list");

        iv = system().newEmptyList();
        testIonValueInjection(iv, "is_list");
    }


    @Test
    public void testIonSexpInjection()
        throws Exception
    {
        IonSexp iv = system().newNullSexp();
        testIonValueInjection(iv, "is_sexp");

        iv = system().newEmptySexp();
        testIonValueInjection(iv, "is_sexp");
    }


    @Test
    public void testIonStructInjection()
        throws Exception
    {
        IonStruct iv = system().newNullStruct();
        testIonValueInjection(iv, "is_struct");

        iv = system().newEmptyStruct();
        testIonValueInjection(iv, "is_struct");
    }


    /**
     * Sadly we can't reuse {@link #testIonValueInjection(IonValue, String)}
     * because the results don't round-trip.
     */
    @Test
    public void testIonDatagramInjection()
        throws Exception
    {
        TopLevel top = topLevel();

        IonDatagram dg = system().newDatagram();
        IonList list   = system().newEmptyList();

        top.define("v", dg);
        Object fv = eval("v");
        checkIon(list, fv);
        assertEval(true, "(is_list v)");
        fv.toString();                // Ensure that toString() doesn't throw.

        // App must not modify an injected object!
        dg = system().newDatagram();
        dg.add().newInt(1113);
        dg.add().newEmptyList().add().newString("hero");

        // The datagram includes a leading IVM which isn't injected.
        assertEquals(3, dg.systemSize());

        top.define("v", dg);
        assertEval(true, "(=== v [1113,['''hero''']])");
    }
}
