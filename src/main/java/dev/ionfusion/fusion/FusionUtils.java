// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonTextWriterBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 *
 */
final class FusionUtils
{
    /** This class is not to be instantiated. */
    private FusionUtils() { }


    static final byte[]   EMPTY_BYTE_ARRAY   = new byte[0];
    static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    static final String[] EMPTY_STRING_ARRAY = new String[0];


    /**
     * @param value must not be null.
     */
    static IonValue cloneIfContained(IonValue value)
    {
        if (value.getContainer() != null)
        {
            value = value.clone();
        }
        return value;
    }


    static String friendlySuffix(long i)
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
     * Renders a zero-based index as a one-based ordinal like
     * "1st", "12th, or "23rd".
     *
     * @param i the zero-based index to display.
     */
    static String friendlyIndex(long i)
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
    static void writeFriendlyOrdinal(Appendable out, long i)
        throws IOException
    {
        out.append(Long.toString(i));
        String suffix = friendlySuffix(i);
        out.append(suffix);
    }


    /**
     * Writes a zero-based index as a one-based ordinal like
     * "1st", "12th, or "23rd".
     *
     * @param out must not be null.
     * @param i the zero-based index to display.
     *
     * @throws IOException if thrown by {@code out}.
     */
    static void writeFriendlyIndex(Appendable out, long i)
        throws IOException
    {
        writeFriendlyOrdinal(out, i + 1);
    }


    /**
     * Writes a zero-based index as a one-based ordinal like
     * "1st", "12th, or "23rd".
     *
     * @param out must not be null.
     * @param i the zero-based index to display.
     */
    static void writeFriendlyIndex(StringBuilder out, long i)
    {
        i++;
        out.append(i);
        String suffix = friendlySuffix(i);
        out.append(suffix);
    }

    static void writeIon(Appendable out, IonValue v)
        throws IOException
    {
        IonWriter writer = IonTextWriterBuilder.standard().build(out);
        v.writeTo(writer);
        writer.flush();
    }


    static void createParentDirs(File file)
        throws IOException
    {
        File parent = file.getParentFile();
        if (parent != null && ! parent.exists())
        {
            if (! parent.mkdirs())
            {
                throw new IOException("Unable to create parent directory of "
                                         + file);
            }
        }
    }


    //=========================================================================


    /**
     * Reads properties from a URL.
     *
     * @throws FusionException if there's a problem reading the resource.
     */
    public static Properties readProperties(URL resource)
        throws FusionException
    {
        try (InputStream stream = resource.openStream())
        {
            Properties props = new Properties();
            props.load(stream);
            return props;
        }
        catch (IOException e)
        {
            String message =
                "Error reading properties from resource " + resource;
            throw new FusionException(message, e);
        }
    }


    /**
     * Reads properties from a file.
     *
     * @param file must be a readable properties file.
     *
     * @throws FusionException if there's a problem reading the file.
     */
    public static Properties readProperties(File file)
        throws FusionException
    {
        try (InputStream stream = new FileInputStream(file))
        {
            Properties props = new Properties();
            props.load(stream);
            return props;
        }
        catch (IOException e)
        {
            String message =
                "Error reading properties from file " + file;
            throw new FusionException(message, e);
        }
    }
}
