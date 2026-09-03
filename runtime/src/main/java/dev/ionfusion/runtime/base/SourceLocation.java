// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static dev.ionfusion.runtime._private.util.Ordinals.writeFriendlyOrdinal;

import com.amazon.ion.IonReader;
import com.amazon.ion.OffsetSpan;
import com.amazon.ion.TextSpan;
import com.amazon.ion.util.Spans;
import java.io.IOException;
import java.util.Objects;


/**
 * A specific location within some Fusion source code.
 * <p>
 * Because Fusion is oriented around Ion data, these locations have semantics
 * aligned with {@link com.amazon.ion.TextSpan} and
 * {@link com.amazon.ion.OffsetSpan}.
 */
public class SourceLocation
    implements CodePosition
{
    /**
     * This descriptor is used by all instances constructed with a null SourceName,
     * preserving the legacy behavior of this class WRT equals and hashCode.
     */
    private static final ResourceDescriptor DISTINCT_UNKNOWN_RESOURCE =
        ResourceDescriptor.unknown();

    /** Not null. */
    private final ResourceDescriptor myResource;


    /**
     * @param rsrc null will be replaced by {@link #DISTINCT_UNKNOWN_RESOURCE}.
     */
    private SourceLocation(ResourceDescriptor rsrc)
    {
        myResource = (rsrc != null ? rsrc : DISTINCT_UNKNOWN_RESOURCE);
    }


    @Override
    public ResourceDescriptor getResourceDesc()
    {
        return myResource;
    }


    @Override
    public long getLine()
    {
        return 0;
    }

    @Override
    public long getColumn()
    {
        return 0;
    }

    @Override
    public long getOffset()
    {
        return -1;
    }


    @Override
    public ModuleIdentity getModuleIdentity()
    {
        if (myResource instanceof SourceName)
        {
            return ((SourceName) myResource).getModuleIdentity();
        }
        return null;
    }


    //==================================================================================
    // Concrete implementations

    private static final class Shorts
        extends SourceLocation
    {
        private final short myLine;
        private final short myColumn;
        private final short myOffset;

        private Shorts(ResourceDescriptor name, short line, short column, short offset)
        {
            super(name);
            myLine = line;
            myColumn = column;
            myOffset = offset;
        }

        @Override
        public long getLine()
        {
            return myLine;
        }

        @Override
        public long getColumn()
        {
            return myColumn;
        }

        @Override
        public long getOffset()
        {
            return myOffset;
        }
    }


    private static final class Ints
        extends SourceLocation
    {
        private final int myLine;
        private final int myColumn;
        private final int myOffset;

        private Ints(ResourceDescriptor name, int line, int column, int offset)
        {
            super(name);
            myLine   = line;
            myColumn = column;
            myOffset = offset;
        }

        @Override
        public long getLine()
        {
            return myLine;
        }

        @Override
        public long getColumn()
        {
            return myColumn;
        }

        @Override
        public long getOffset()
        {
            return myOffset;
        }
    }


    private static final class Longs
        extends SourceLocation
    {
        private final long myLine;
        private final long myColumn;
        private final long myOffset;

        private Longs(ResourceDescriptor name, long line, long column, long offset)
        {
            super(name);
            myLine   = line;
            myColumn = column;
            myOffset = offset;
        }

        @Override
        public long getLine()
        {
            return myLine;
        }

        @Override
        public long getColumn()
        {
            return myColumn;
        }

        @Override
        public long getOffset()
        {
            return myOffset;
        }
    }


    //==================================================================================


    /**
     * Returns an instance that represents an unknown location in the given
     * source.
     *
     * @param desc can be null.
     *
     * @return null when all parameters are unknown.
     */
    public static SourceLocation forName(ResourceDescriptor desc)
    {
        if (desc == null) return null;

        // TODO Can this allocation be eliminated?
        //      We'll probably be creating lots of similar instances.
        return new SourceLocation(desc);
    }


    /**
     * Returns an instance that represents the given text location.
     *
     * @param line one-based.
     * Values less than 1 indicate that the line is unknown.
     * @param column one-based.
     * Values less than 1 indicate that the column is unknown.
     * Ignored if the line is unknown.
     * @param desc can be null.
     *
     * @return null when all parameters are unknown.
     */
    public static SourceLocation forLineColumn(long line, long column,
                                               ResourceDescriptor desc)
    {
        if (line < 1)
        {
            return forName(desc);
        }

        if (column < 0)
        {
            column = 0;
        }

        if (line <= Short.MAX_VALUE && column <= Short.MAX_VALUE)
        {
            return new Shorts(desc, (short) line, (short) column, (short) -1);
        }

        if (line <= Integer.MAX_VALUE && column <= Integer.MAX_VALUE)
        {
            return new Ints(desc, (int) line, (int) column, -1);
        }

        return new Longs(desc, line, column, -1);
    }


    /**
     * Returns an instance that represents the given text location.
     *
     * @param line one-based.
     * Values less than 1 indicate that the line is unknown.
     * @param column one-based.
     * Values less than 1 indicate that the column is unknown.
     * Ignored if the line is unknown.
     *
     * @return null when all parameters are unknown.
     */
    public static SourceLocation forLineColumn(long line, long column)
    {
        return forLineColumn(line, column, null);
    }


    /**
     * Returns an instance that represents the current span of the reader.
     * This currently only supports Ion text sources and only captures the
     * start position.
     *
     * @param source must not be null.
     * @param desc can be null.
     *
     *
     * @return null if no descriptor is given and no location could be determined from
     * the source.
     */
    public static SourceLocation forCurrentSpan(IonReader  source,
                                                ResourceDescriptor desc)
    {
        // SpanProvider.currentSpan() crashes if not on a value.
        if (source.getType() != null)
        {
            TextSpan   ts = Spans.currentSpan(TextSpan.class, source);
            OffsetSpan os = Spans.currentSpan(OffsetSpan.class, source);

            if (ts != null)
            {
                long line   = ts.getStartLine();
                long column = ts.getStartColumn();
                long offset = os.getStartOffset();

                if (line <= Short.MAX_VALUE &&
                    column <= Short.MAX_VALUE &&
                    offset <= Short.MAX_VALUE)
                {
                    return new Shorts(desc, (short) line, (short) column,
                                      (short) offset);
                }

                if (line <= Integer.MAX_VALUE &&
                    column <= Integer.MAX_VALUE &&
                    offset <= Integer.MAX_VALUE)
                {
                    return new Ints(desc, (int) line, (int) column, (int) offset);
                }

                return new Longs(desc, line, column, offset);
            }
        }

        return forName(desc);
    }


    /**
     * Displays this location in a human-readable form, in terms of line,
     * column, and source name.
     *
     * @param out the stream to write
     *
     * @throws IOException if thrown by the {@link Appendable}.
     */
    @Override
    public void display(Appendable out)
        throws IOException
    {
        long line   = getLine();
        long column = getColumn();

        if (line < 1)
        {
            out.append("unknown location");
            if (!myResource.isUnknown())
            {
                out.append(" in ").append(myResource.display());
            }
        }
        else
        {
            writeFriendlyOrdinal(out, line);
            out.append(" line");

            if (column > 0)
            {
                out.append(", ");
                writeFriendlyOrdinal(out, column);
                out.append(" column");
            }

            if (!myResource.isUnknown())
            {
                out.append(" of ");
                ModuleIdentity module = getModuleIdentity();
                if (module != null)
                {
                    out.append(module.absolutePath())
                       .append(" (at ")
                       .append(myResource.display())
                       .append(')');
                }
                else
                {
                    out.append(myResource.display());
                }
            }
        }
    }


    /**
     * Returns a view of this object suitable for debugging.
     * For displaying messages to users, use {@link #display()} instead.
     */
    @Override
    public String toString()
    {
        return display();
    }


    public boolean equals(ResourcePosition that)
    {
        return (this == that
                || (that != null
                    && this.myResource.equals(that.getResourceDesc())
                    && this.getLine()   == that.getLine()
                    && this.getColumn() == that.getColumn()
                    && this.getOffset() == that.getOffset()));
    }

    @Override
    public boolean equals(Object that)
    {
        return that instanceof ResourcePosition &&
               this.equals((ResourcePosition) that);
    }


    private static final int HASH_SEED = SourceLocation.class.hashCode();

    @Override
    public int hashCode()
    {
        final int prime = 8191;
        int result = HASH_SEED + Objects.hashCode(myResource);
        result ^= (result << 29) ^ (result >> 3);
        result = prime * result + (int) getLine();
        result ^= (result << 29) ^ (result >> 3);
        result = prime * result + (int) getColumn();
        result ^= (result << 29) ^ (result >> 3);
        result = prime * result + (int) getOffset();
        result ^= (result << 29) ^ (result >> 3);
        return result;
    }
}
