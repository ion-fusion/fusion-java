// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.junit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.enumeration;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;


public class StdioTestCase
{
    private LinkedList<InputStream> myStdinData;
    private InputStream             myStdin;

    private ByteArrayOutputStream myStdoutBytes;
    private ByteArrayOutputStream myStderrBytes;

    private PrintStream myStdout;
    private PrintStream myStderr;


    @BeforeEach
    public void setUpStdio()
    {
        myStdoutBytes = new ByteArrayOutputStream();
        myStderrBytes = new ByteArrayOutputStream();

        myStdout = new PrintStream(myStdoutBytes);
        myStderr = new PrintStream(myStderrBytes);
    }

    @AfterEach
    public void tearDownStdio()
    {
        myStdinData = null;
        myStdin     = null;

        myStdoutBytes = null;
        myStderrBytes = null;
        myStdout = null;
        myStderr = null;
    }


    protected void supplyInput(InputStream data)
    {
        if (myStdinData == null)
        {
            myStdinData = new LinkedList<>();
        }
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
            if (myStdinData != null)
            {
                myStdin = new SequenceInputStream(enumeration(myStdinData));
            }
            else
            {
                myStdin = new ByteArrayInputStream(new byte[0]);
            }
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
