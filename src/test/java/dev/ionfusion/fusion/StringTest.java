// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionString.unsafeStringToJavaString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.amazon.ion.util.IonTextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class StringTest
    extends CoreTestCase
{
    @BeforeEach
    public void requires()
        throws FusionException
    {
        topLevel().requireModule("/fusion/string");
    }


    @Test
    public void testUnsafeStringToJavaString()
        throws Exception
    {
        String text = "87g2hsst";
        Object s = eval(IonTextUtils.printString(text));
        assertEquals(text, unsafeStringToJavaString(topLevel(), s));

        s = eval("null.string");
        assertEquals(null, unsafeStringToJavaString(topLevel(), s));
    }


    @Test
    public void testStringAppendInvalid()
        throws Exception
    {
        expectArgumentExn("(string_append 1)",0);
        expectArgumentExn("(string_append true)",0);
    }


    @Test
    public void testStringCaseTransformFail()
        throws Exception
    {
        String [] ops = { "string_to_upper", "string_to_lower" };

        for (String op : ops)
        {
            expectArityExn("("+op+")");
            expectArityExn("("+op+" \"a\" \"b\")");

            expectContractExn("("+op+" {})");
            expectContractExn("("+op+" null.string)");
        }

    }

}
