// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion._Private_Trampoline;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides information about this release of the FusionJava library.
 */
public final class FusionJarInfo
{
    private String ourReleaseLabel;


    /**
     * Constructs a new instance that can provide build information about this
     * library.
     *
     * @throws FusionException if there's a problem loading the build info.
     */
    public FusionJarInfo()
        throws FusionException
    {
        loadBuildProperties();
    }


    /**
     * Gets the public release label of this library.
     * Don't attempt to parse this label; we reserve the right to change its
     * format at any time.
     *
     * @return null if the label is unknown.
     */
    public String getReleaseLabel()
    {
        return ourReleaseLabel;
    }

    // TODO writeTo(IonWriter)

    //========================================================================

    /**
     * Gets a property, ensuring a non-empty value.
     * @return null but not empty string
     */
    private String nonEmptyProperty(Properties props, String name)
    {
        String value = props.getProperty(name, "");
        if (value.length() == 0) value = null;
        return value;
    }

    private void loadBuildProperties()
        throws FusionException
    {
        String file = getClass().getSimpleName() + ".properties";
        try
        {
            Properties props = new Properties();

            InputStream in = getClass().getResourceAsStream(file);
            if (in != null)
            {
                try
                {
                    props.load(in);
                }
                finally
                {
                    in.close();
                }
            }

            ourReleaseLabel = nonEmptyProperty(props, "release_label");
        }
        catch (IOException e)
        {
            String message = "Unable to load " + file;
            throw _Private_Trampoline.newFusionException(message, e);
        }
    }
}
