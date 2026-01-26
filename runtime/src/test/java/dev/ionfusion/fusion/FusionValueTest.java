// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionSexp.isEmptySexp;
import static dev.ionfusion.fusion.FusionSexp.isSexp;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionValue.annotationsAsJavaStrings;
import static dev.ionfusion.fusion.FusionValue.isAnnotated;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.ion.IonInt;
import com.amazon.ion.IonValue;
import dev.ionfusion.runtime.embed.TopLevel;
import org.junit.jupiter.api.Test;

public class FusionValueTest
    extends CoreTestCase
{
    // TODO this isn't currently testing the case where the FV's system
    // matches the given system... because there's no API for declaring
    // the FusionRuntime's IonSystem.

    @Test
    public void testIonValue()
        throws Exception
    {
        Object fv = eval("12");
        checkLong(12, fv);
        IonValue iv = FusionValue.copyToIonValue(evaluator(), fv, system());
        assertEquals(12, ((IonInt)iv).intValue());

        fv = eval("(lambda () 12)");
        assertSame(null, FusionValue.copyToIonValueMaybe(evaluator(), fv, system()));
    }

    @Test
    public void testBadCopyToIonValue()
        throws Exception
    {
        Object fv = eval("(lambda () 12)");
        assertThrows(FusionException.class,
                     () ->  FusionValue.copyToIonValue(evaluator(), fv, system()));
    }

    @Test
    public void testIsAnnotated()
        throws Exception
    {
        useTstRepo();
        TopLevel top = topLevel();
        Evaluator eval = evaluator();
        top.requireModule("/testutils");

        Object values = top.lookup("representative_ion_data");
        assertTrue(isSexp(eval, values));

        while (! isEmptySexp(eval, values))
        {
            Object value = unsafePairHead(eval, values);
            assertFalse(isAnnotated(eval, value));

            value = top.call("annotate", value, "ann");
            assertTrue(isAnnotated(eval, value), value.toString());

            String[] anns = annotationsAsJavaStrings(eval, value);
            assertArrayEquals(new String[]{ "ann" }, anns, "annotations on " + value);

            values = unsafePairTail(eval, values);
        }
    }


    private boolean hasAnnotation(String expr, String annotation)
        throws Exception
    {
        Object value = eval(expr);
        return FusionValue.hasAnnotation(evaluator(), value, annotation);
    }

    @Test
    public void testHasAnnotation()
        throws Exception
    {
        useTstRepo();

        assertTrue(hasAnnotation("(quote a::null)", "a"));
        assertTrue(hasAnnotation("(quote a::b::null)", "a"));
        assertTrue(hasAnnotation("(quote a::b::null)", "b"));
        assertTrue(hasAnnotation("(quote ''::null)", ""));

        assertFalse(hasAnnotation("(quote a::null)", "x"));
        assertFalse(hasAnnotation("(quote a::b::null)", "x"));
    }
}
