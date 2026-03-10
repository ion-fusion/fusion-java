// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.util;

import static java.nio.file.Files.newInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Utilities for reading {@code .properties} files.
 *
 * The key responsibility is ensuring that any error message includes the intention.
 */
public final class PropertiesFiles
{
    /**
     * This class is not to be instantiated.
     */
    private PropertiesFiles() { }


    /**
     * Reads properties from a URL.
     *
     * @throws IOException if there's a problem reading the resource.
     */
    public static Properties readProperties(URL resource)
        throws IOException
    {
        try (InputStream stream = resource.openStream())
        {
            Properties props = new Properties();
            props.load(stream);
            return props;
        }
        catch (IOException e)
        {
            String message = "Error reading properties from resource " + resource;
            throw new IOException(message, e);
        }
    }


    /**
     * Reads properties from a file.
     *
     * @param file must be a readable properties file.
     *
     * @throws IOException if there's a problem reading the file.
     */
    public static Properties readProperties(Path file)
        throws IOException
    {
        try (InputStream stream = newInputStream(file))
        {
            Properties props = new Properties();
            props.load(stream);
            return props;
        }
        catch (IOException e)
        {
            String message = "Error reading properties from file " + file;
            throw new IOException(message, e);
        }
    }
}
