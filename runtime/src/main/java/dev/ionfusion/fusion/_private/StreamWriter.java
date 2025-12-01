// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private;

import static com.amazon.ion.system.IonTextWriterBuilder.UTF8;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@class Writer} that writes to an {@link OutputStream}, allowing bypass of
 * encoding mechanisms for better performance.
 */
public class StreamWriter
    implements Appendable, Closeable
{
    private final OutputStream myStream;
    private final Writer       myWriter;

    public StreamWriter(Path file)
        throws IOException
    {
        Files.createDirectories(file.getParent());

        myStream = Files.newOutputStream(file);

        // FIXME OutputStream will not be closed if this fails:
        myWriter = new BufferedWriter(new OutputStreamWriter(myStream, UTF8));
    }

    public StreamWriter(File dir, String fileName)
        throws IOException
    {
        this(dir.toPath().resolve(fileName));
    }

    @Override
    public void close()
        throws IOException
    {
        myWriter.close();
    }


    /**
     * Writes bytes directly to the underlying stream, without any encoding.
     */
    public final void write(byte[] buffer, int off, int len)
        throws IOException
    {
        // Flush the buffered writer, so we can write directly to the stream.
        myWriter.flush();
        myStream.write(buffer, off, len);
    }


    @Override
    public Appendable append(char c)
        throws IOException
    {
        myWriter.append(c);
        return this;
    }

    @Override
    public Appendable append(CharSequence csq)
        throws IOException
    {
        myWriter.append(csq);
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end)
        throws IOException
    {
        myWriter.append(csq, start, end);
        return this;
    }
}
