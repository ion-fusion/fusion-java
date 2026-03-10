// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.util;

import java.io.IOException;

/**
 * Utilities for displaying human-friendly ordinals like "1st", "2nd", or "23rd".
 */
public class Ordinals
{
    private Ordinals() { }


    private static String friendlySuffix(long i)
    {
        long lastDigit = i % 10;
        if (lastDigit == 1 && i != 11)
        {
            return "st";
        }
        if (lastDigit == 2 && i != 12)
        {
            return "nd";
        }
        if (lastDigit == 3 && i != 13)
        {
            return "rd";
        }
        return "th";
    }

    /**
     * Renders a zero-based index as a one-based ordinal like "1st", "12th, or "23rd".
     *
     * @param i the zero-based index to display.
     */
    public static String friendlyIndex(long i)
    {
        i++;
        return i + friendlySuffix(i);
    }

    /**
     * Writes a one-based ordinal like "1st", "12th, or "23rd".
     *
     * @param out must not be null.
     * @param i the one-based ordinal to display.
     *
     * @throws IOException if thrown by {@code out}.
     */
    public static void writeFriendlyOrdinal(Appendable out, long i)
        throws IOException
    {
        out.append(Long.toString(i));
        String suffix = friendlySuffix(i);
        out.append(suffix);
    }

    /**
     * Writes a zero-based index as a one-based ordinal like "1st", "12th, or "23rd".
     *
     * @param out must not be null.
     * @param i the zero-based index to display.
     *
     * @throws IOException if thrown by {@code out}.
     */
    public static void writeFriendlyIndex(Appendable out, long i)
        throws IOException
    {
        writeFriendlyOrdinal(out, i + 1);
    }

    /**
     * Writes a zero-based index as a one-based ordinal like "1st", "12th, or "23rd".
     *
     * @param out must not be null.
     * @param i the zero-based index to display.
     */
    public static void writeFriendlyIndex(StringBuilder out, long i)
    {
        i++;
        out.append(i);
        String suffix = friendlySuffix(i);
        out.append(suffix);
    }
}
