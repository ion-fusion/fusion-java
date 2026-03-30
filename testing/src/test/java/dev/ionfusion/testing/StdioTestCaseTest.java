// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.testing;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;

public class StdioTestCaseTest
    extends StdioTestCase
{
    @Test
    public void multipleInputsConcatenate()
        throws IOException
    {
        supplyInput("a");
        supplyInput("b");
        supplyInput("c");

        BufferedReader buf =
            new BufferedReader(new InputStreamReader(stdin(), UTF_8));

        assertThat(buf.readLine(), is("abc"));
    }
}
