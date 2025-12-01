// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionStruct.unsafeStructSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazon.ion.IonReader;
import org.junit.jupiter.api.Test;

public class StandardReaderTest
    extends CoreTestCase
{
    @Test
    public void testIonSyntaxError()
        throws Exception
    {
        useTstRepo();

        // TODO This should be more specific, but SyntaxException is oriented
        // around problems with Fusion syntax forms, not Ion data forms.
        Throwable e =
            assertEvalThrows(FusionErrorException.class,
                             "(require '''/malformed/ion_syntax_error''')");
        assertTrue(e.getMessage().contains("Error reading /malformed/ion_syntax_error"));
    }

    @Test
    public void testReadingStructWithRepeat()
        throws Exception
    {
        IonReader reader = system().newReader("{f:1,f:2}");
        reader.next();
        Object s = StandardReader.read(evaluator(), reader);
        assertEquals(2, unsafeStructSize(evaluator(), s));
    }
}
