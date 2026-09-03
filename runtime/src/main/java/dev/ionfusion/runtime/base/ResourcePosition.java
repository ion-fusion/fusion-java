// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A specific location within some resource.
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
    void display(Appendable out)
        throws IOException;


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
}
