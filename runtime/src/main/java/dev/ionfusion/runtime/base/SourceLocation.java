// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import com.amazon.ion.IonReader;


/**
 * A specific location within some Fusion source code.
 * <p>
 * Because Fusion is oriented around Ion data, these locations have semantics
 * aligned with {@link com.amazon.ion.TextSpan} and
 * {@link com.amazon.ion.OffsetSpan}.
 */
public class SourceLocation
{
    private SourceLocation() {}


    /**
     * Returns an instance that represents an unknown location in the given resource.
     *
     * @param desc must not be null.
     *
     * @return a location with no offsets and the given descriptor.
     */
    public static ResourcePosition forName(ResourceDescriptor desc)
    {
        return ResourcePosition.unknown(desc);
    }


    /**
     * Returns an instance that represents the given text location.
     *
     * @param line one-based.
     * Values less than 1 indicate that the line is unknown.
     * @param column one-based.
     * Values less than 1 indicate that the column is unknown.
     * Ignored if the line is unknown.
     * @param desc must not be null.
     *
     * @return a location with no offsets and the given descriptor.
     */
    public static ResourcePosition forLineColumn(long line, long column,
                                                 ResourceDescriptor desc)
    {
        return ResourcePosition.forPosition(desc, line, column, -1);
    }


    /**
     * Returns an instance that represents the current span of the reader.
     * This currently only supports Ion text sources and only captures the
     * start position.
     *
     * @param source must not be null.
     * @param desc must not be null.
     *
     * @return a location with current span's offsets and the given descriptor.
     */
    public static ResourcePosition forCurrentSpan(IonReader  source,
                                                  ResourceDescriptor desc)
    {
        return ResourcePosition.forCurrentSpan(desc, source);
    }
}
