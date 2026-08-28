// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionStruct.unsafeStructSize;
import static dev.ionfusion.fusion.StandardReader.read;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amazon.ion.IonReader;
import dev.ionfusion.runtime.base.ResourceDescriptor;
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
        assertThat(e.getMessage(),
                   allOf(startsWith("Error reading "),
                         containsString("/malformed/ion_syntax_error.fusion")));
    }

    @Test
    public void testReadingStructWithRepeat()
        throws Exception
    {
        IonReader reader = system().newReader("{f:1,f:2}");
        reader.next();
        Object s = read(evaluator(), reader, ResourceDescriptor.unknown());
        assertEquals(2, unsafeStructSize(evaluator(), s));
    }
}
