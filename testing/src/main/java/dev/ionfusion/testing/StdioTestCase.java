// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.testing;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.enumeration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;


public class StdioTestCase
{
    private final LinkedList<InputStream> myStdinData = new LinkedList<>();
    private       InputStream             myStdin;

    private final ByteArrayOutputStream myStdoutBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream myStderrBytes = new ByteArrayOutputStream();

    private final PrintStream myStdout = new PrintStream(myStdoutBytes);
    private final PrintStream myStderr = new PrintStream(myStderrBytes);


    protected void supplyInput(InputStream data)
    {
        myStdinData.push(data);
    }

    protected void supplyInput(byte[] bytes)
    {
        supplyInput(new ByteArrayInputStream(bytes));
    }

    protected void supplyInput(String s)
    {
        supplyInput(s.getBytes(UTF_8));
    }


    /**
     * Lazily constructs the standard input stream.
     * The stream will be empty unless {@link #supplyInput(String)} has been
     * called.
     */
    protected InputStream stdin()
    {
        if (myStdin == null)
        {
            myStdin = new SequenceInputStream(enumeration(myStdinData));
        }
        return myStdin;
    }

    protected PrintStream stdout()
    {
        return myStdout;
    }

    protected PrintStream stderr()
    {
        return myStderr;
    }


    protected String stdoutToString()
    {
        return toString(myStdoutBytes);
    }

    protected String stderrToString()
    {
        return toString(myStderrBytes);
    }


    private String toString(ByteArrayOutputStream bytes)
    {
        try
        {
            return bytes.toString(UTF_8.name());
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError(e);
        }
    }
}
