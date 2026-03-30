// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Provides access to the effective standard input and output streams for a component.
 * These may not be the same as the JVM's standard streams such as {@code System.in} and
 * {@code System.out}.
 */
public class Stdio
    implements Flushable
{
    private final InputStream  myStdin;
    private final OutputStream myStdout;
    private final PrintStream  myStderr;


    public Stdio(InputStream stdin, OutputStream stdout, PrintStream stderr)
    {
        myStdin  = stdin;
        myStdout = stdout;
        myStderr = stderr;
    }

    /**
     * Captures the standard streams of the JVM.
     * <p>
     * This bypasses {@link System#out} and its {@link PrintStream} wrapper, which
     * ignores any I/O exceptions.
     */
    public static Stdio forSystem()
    {
        OutputStream stdout = new FileOutputStream(FileDescriptor.out);
        return new Stdio(System.in, stdout, System.err);
    }


    public InputStream stdin()
    {
        return myStdin;
    }

    public OutputStream stdout()
    {
        return myStdout;
    }

    /**
     * Gets the standard error stream.  Since this is a {@link PrintStream}, it does not
     * throw {@link IOException}.  This assumes that we should generally ignore problems
     * writing error messages.
     */
    public PrintStream stderr()
    {
        return myStderr;
    }


    /**
     * Flushes the output streams.
     *
     * @throws IOException if an I/O error occurs flushing {@link #stdout()};
     * {@link #stderr()} flushes silently.
     */
    public void flush()
        throws IOException
    {
        myStdout.flush();
        myStderr.flush();
    }

    public void flushQuietly()
    {
        try
        {
            flush();
        }
        catch (IOException e)
        {
            // Ignore
        }
    }
}
