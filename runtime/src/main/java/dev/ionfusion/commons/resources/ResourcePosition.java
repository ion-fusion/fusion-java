// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.commons.resources;

import static dev.ionfusion.runtime._private.util.Ordinals.displayFriendlyPosition;
import static java.util.Objects.requireNonNull;

import com.amazon.ion.IonReader;
import com.amazon.ion.OffsetSpan;
import com.amazon.ion.TextSpan;
import com.amazon.ion.util.Spans;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A specific offset and/or line and column within some resource.
 * <p>
 * Because Fusion is oriented around Ion data, these locations have semantics aligned
 * with {@link com.amazon.ion.TextSpan} and {@link com.amazon.ion.OffsetSpan}.
 */
public interface ResourcePosition
{
    /**
     * Describes the resource containing this location.
     *
     * @return not null.
     */
    ResourceDescriptor getResourceDesc();


    /**
     * Gets the one-based line number.
     *
     * @return zero if the line and column are unknown.
     */
    long getLine();

    /**
     * Gets the one-based column number.
     * <p>
     * Because it doesn't make sense to count columns without counting lines, this value
     * is zero whenever the line number is zero.
     * </p>
     *
     * @return zero if the line and column are unknown.
     */
    long getColumn();

    /**
     * Gets the zero-based offset.
     *
     * @return -1 if the offset is unknown.
     */
    long getOffset();


    /**
     * Displays this position in a human-readable form, in terms of line, column, and
     * resource.  Additional semantic context may also be included.
     *
     * @param out the stream to write into.
     *
     * @throws IOException if thrown by the {@link Appendable}.
     */
    default void display(Appendable out)
        throws IOException
    {
        displayFriendlyPosition(out, getLine(), getColumn(), getOffset());

        ResourceDescriptor rsrc = getResourceDesc();
        if (!rsrc.isUnknown())
        {
            out.append(" of ").append(rsrc.display());
        }
    }


    /**
     * Displays this position in a human-readable form, in terms of line, column, and
     * resource.  Additional semantic context may also be included.
     *
     * @return not null.
     */
    default String display()
    {
        StringBuilder out = new StringBuilder();
        try
        {
            display(out);
        }
        catch (IOException e)
        {
            // StringBuilder shouldn't throw this, but let's be safe.
            throw new UncheckedIOException(e);
        }
        return out.toString();
    }


    //==================================================================================
    // Factories

    /**
     * Returns an instance representing an unknown position in the given resource.
     *
     * @param desc must not be null.
     *
     * @return an instance with the given descriptor and no position.
     */
    static ResourcePosition unknown(ResourceDescriptor desc)
    {
        requireNonNull(desc);

        // TODO Consider reducing allocation by memoizing into a descriptor attribute.
        //      These will be allocated frequently when "reading" a DOM.
        return new BaseResourcePosition(desc);
    }


    /**
     * Returns an instance representing the given position in the resource.
     *
     * @param desc must not be null.
     * @param line one-based.
     * Values less than 1 indicate that the line is unknown.
     * @param column one-based.
     * Values less than 1 indicate that the column is unknown.
     * Ignored if the line is unknown.
     * @param offset zero-based.
     * Values less than 0 indicate that the offset is unknown.
     *
     * @return an instance with the given descriptor and position.
     */
    static ResourcePosition forPosition(ResourceDescriptor desc,
                                        long line,
                                        long column,
                                        long offset)
    {
        requireNonNull(desc);

        if (line < 1)
        {
            if (offset < 0)
            {
                return unknown(desc);
            }

            line = 0; // normalize
            column = 0;
        }
        else if (column < 0)
        {
            column = 0;
        }

        if (line <= Short.MAX_VALUE &&
            column <= Short.MAX_VALUE &&
            offset <= Short.MAX_VALUE)
        {
            return new ShortResourcePosition(desc, (short) line, (short) column,
                                             (short) offset);
        }

        if (line <= Integer.MAX_VALUE &&
            column <= Integer.MAX_VALUE &&
            offset <= Integer.MAX_VALUE)
        {
            return new IntResourcePosition(desc, (int) line, (int) column,
                                           (int) offset);
        }

        return new LongResourcePosition(desc, line, column, offset);
    }


    /**
     * Returns an instance representing the current span of the reader. This captures
     * the span's start position, not the full span.
     *
     * @param desc must not be null.
     * @param reader must not be null.
     *
     * @return an instance with the given descriptor and the reader's current position.
     */
    public static ResourcePosition forCurrentSpan(ResourceDescriptor desc,
                                                  IonReader reader)
    {
        requireNonNull(desc);

        // SpanProvider.currentSpan() crashes if not on a value.
        if (reader.getType() == null)
        {
            return unknown(desc);
        }

        long line   = 0;
        long column = 0;
        long offset = -1;

        TextSpan ts = Spans.currentSpan(TextSpan.class, reader);
        if (ts != null)
        {
            line   = ts.getStartLine();
            column = ts.getStartColumn();
        }

        OffsetSpan os = Spans.currentSpan(OffsetSpan.class, reader);
        if (os != null)
        {
            offset = os.getStartOffset();
        }

        return forPosition(desc, line, column, offset);
    }
}
