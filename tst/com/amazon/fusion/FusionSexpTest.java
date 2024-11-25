// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.unsafeBoolToJavaBoolean;
import static com.amazon.fusion.FusionNumber.unsafeTruncateIntToJavaInt;
import static com.amazon.fusion.FusionSexp.unsafeSexpToJavaList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FusionSexpTest
    extends CoreTestCase
{
    @Test
    public void testSexpToJavaList()
        throws Exception
    {
        Evaluator eval = evaluator();

        Object sexp = eval("(quote (1 true))");

        List<Object> list = unsafeSexpToJavaList(eval, sexp);
        assertEquals(2, list.size());
        assertEquals(1, unsafeTruncateIntToJavaInt(eval, list.get(0)));
        assertTrue(unsafeBoolToJavaBoolean(eval, list.get(1)));

        sexp = eval("(sexp)");
        list = unsafeSexpToJavaList(eval, sexp);
        assertEquals(0, list.size());

        sexp = eval("(quote null.sexp)");
        list = unsafeSexpToJavaList(eval, sexp);
        assertNull(list);
    }
}
