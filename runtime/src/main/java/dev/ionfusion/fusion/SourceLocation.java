// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion._private.FusionUtils.writeFriendlyOrdinal;

import com.amazon.ion.IonReader;
import com.amazon.ion.OffsetSpan;
import com.amazon.ion.TextSpan;
import com.amazon.ion.util.Spans;
import java.io.IOException;
import java.util.Objects;


/**
 * A specific location within some source of data.  In this library they are
 * generally used to reference Fusion source code, but they are equally useful
 * for other forms of serialized data.
 * <p>
 * Because Fusion is oriented around Ion data, these locations have semantics
 * aligned with {@link com.amazon.ion.TextSpan} and
 * {@link com.amazon.ion.OffsetSpan}.
 */
public class SourceLocation
{
    /** May be null. */
    private final SourceName myName;


    /**
     * @param name may be null
     */
    private SourceLocation(SourceName name)
    {
        myName = name;
    }


    /**
     * Gets the name of the source of this location.
     * @return null if the source name isn't known.
     */
    public SourceName getSourceName()
    {
        return myName;
    }

    /**
     * Gets the one-based line number.
     * @return zero if the line is unknown.
     */
    public long getLine()
    {
        return 0;
    }

    /**
     * Gets the one-based column number.
     * <p>
     * Because it doesn't make sense to count columns without counting lines,
     * this value is zero whenever the line number is zero.
     * </p>
     * @return zero if the column is unknown.
     */
    public long getColumn()
    {
        return 0;
    }


    // TODO Define what this offset is counting.
    /**
     * Gets the zero-based starting offset.
     * @return -1 if the offset is unknown.
     */
    public long getStartOffset()
    {
        return -1;
    }


    private static final class Shorts
        extends SourceLocation
    {
        private final short myLine;
        private final short myColumn;
        private final short myStartOffset;

        private Shorts(SourceName name, short line, short column,
                       short startOffset)
        {
            super(name);
            myLine = line;
            myColumn = column;
            myStartOffset = startOffset;
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
        public long getStartOffset()
        {
            return myStartOffset;
        }
    }


    private static final class Ints
        extends SourceLocation
    {
        private final int myLine;
        private final int myColumn;
        private final int myStartOffset;

        private Ints(SourceName name, int line, int column, int startOffset)
        {
            super(name);
            myLine = line;
            myColumn = column;
            myStartOffset = startOffset;
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
        public long getStartOffset()
        {
            return myStartOffset;
        }
    }


    private static final class Longs
        extends SourceLocation
    {
        private final long myLine;
        private final long myColumn;
        private final long myStartOffset;

        private Longs(SourceName name, long line, long column, long startOffset)
        {
            super(name);
            myLine = line;
            myColumn = column;
            myStartOffset = startOffset;
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
        public long getStartOffset()
        {
            return myStartOffset;
        }
    }


    /**
     * Returns an instance that represents an unknown location in the given
     * source.
     *
     * @param name may be null.
     *
     * @return null when all parameters are unknown.
     */
    public static SourceLocation forName(SourceName name)
    {
        if (name == null) return null;

        // TODO Can this allocation be eliminated?
        //      We'll probably be creating lots of similar instances.
        return new SourceLocation(name);
    }


    /**
     * Returns an instance that represents the given text location.
     *
     * @param line one-based.
     * Values less than 1 indicate that the line is unknown.
     * @param column one-based.
     * Values less than 1 indicate that the column is unknown.
     * Ignored if the line is unknown.
     * @param name may be null.
     *
     * @return null when all parameters are unknown.
     */
    public static SourceLocation forLineColumn(long line, long column,
                                               SourceName name)
    {
        if (line < 1)
        {
            return forName(name);
        }

        if (column < 0)
        {
            column = 0;
        }

        if (line <= Short.MAX_VALUE && column <= Short.MAX_VALUE)
        {
            return new Shorts(name, (short) line, (short) column, (short) -1);
        }

        if (line <= Integer.MAX_VALUE && column <= Integer.MAX_VALUE)
        {
            return new Ints(name, (int) line, (int) column, -1);
        }

        return new Longs(name, line, column, -1);
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
     * This currently only supports Ion text sources, and only captures the
     * start position.
     *
     * @param source must not be null.
     * @param name may be null.
     *
     * @return null if no name is given and no location could be determined from
     * the source.
     */
    public static SourceLocation forCurrentSpan(IonReader  source,
                                                SourceName name)
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
                    return new Shorts(name, (short) line, (short) column,
                                      (short) offset);
                }

                if (line <= Integer.MAX_VALUE &&
                    column <= Integer.MAX_VALUE &&
                    offset <= Integer.MAX_VALUE)
                {
                    return new Ints(name, (int) line, (int) column, (int) offset);
                }

                return new Longs(name, line, column, offset);
            }
        }

        return forName(name);
    }


    /**
     * Returns an instance that represents the current span of the reader.
     * This currently only supports Ion text sources, and only captures the
     * start position.
     *
     * @param source must not be null.
     *
     * @return null if no location could be determined from the source.
     */
    public static SourceLocation forCurrentSpan(IonReader source)
    {
        return forCurrentSpan(source, null);
    }


    private void displayOrdinal(Appendable out, long ord)
        throws IOException
    {
        if (ord < 1)
        {
            out.append("???");
        }
        else
        {
            writeFriendlyOrdinal(out, ord);
        }
    }


    /**
     * Displays this location in a human-readable form, in terms of line,
     * column, and source name.
     *
     * @param out the stream to write
     *
     * @throws IOException if thrown by the {@link Appendable}.
     */
    public void display(Appendable out)
        throws IOException
    {
        long line   = getLine();
        long column = getColumn();

        if (line < 1)
        {
            out.append("unknown location");
            if (myName != null)
            {
                out.append(" in ");
                out.append(myName.display());
            }
        }
        else
        {
            displayOrdinal(out, line);
            out.append(" line");

            if (column > 0)
            {
                out.append(", ");
                displayOrdinal(out, column);
                out.append(" column");
            }

            if (myName != null)
            {
                out.append(" of ");
                out.append(myName.display());
            }
        }
    }

    /**
     * Displays this location in a human-readable form, in terms of line,
     * column, and source name.
     *
     * @return not null.
     */
    public String display()
    {
        StringBuilder out = new StringBuilder();
        try
        {
            display(out);
        }
        catch (IOException e) { /* shouldn't happen */ }
        return out.toString();
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


    public boolean equals(SourceLocation that)
    {
        return (this == that
                || (that != null
                    && Objects.equals(this.myName, that.myName)
                    && this.getLine()        == that.getLine()
                    && this.getColumn()      == that.getColumn()
                    && this.getStartOffset() == that.getStartOffset()));
    }

    @Override
    public boolean equals(Object that)
    {
        return that instanceof SourceLocation && equals((SourceLocation) that);
    }


    private static final int HASH_SEED = SourceLocation.class.hashCode();

    @Override
    public int hashCode()
    {
        final int prime = 8191;
        int result = HASH_SEED + Objects.hashCode(myName);
        result ^= (result << 29) ^ (result >> 3);
        result = prime * result + (int) getLine();
        result ^= (result << 29) ^ (result >> 3);
        result = prime * result + (int) getColumn();
        result ^= (result << 29) ^ (result >> 3);
        result = prime * result + (int) getStartOffset();
        result ^= (result << 29) ^ (result >> 3);
        return result;
    }
}
