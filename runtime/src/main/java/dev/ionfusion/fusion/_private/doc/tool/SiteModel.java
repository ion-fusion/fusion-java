// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import com.amazon.ion.Timestamp;
import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.util.FusionJarInfo;

/**
 * Holds site-wide information available to templates.
 */
public class SiteModel
{
    private final Timestamp     myTime;
    private final FusionJarInfo myJarInfo;

    public SiteModel()
        throws FusionException
    {
        myTime = Timestamp.nowZ();
        myJarInfo = new FusionJarInfo();
    }


    public Timestamp getTime()
    {
        return myTime;
    }

    public String getVersion()
    {
        return myJarInfo.getReleaseLabel();
    }

    public boolean isDevBuild()
    {
        return getVersion().endsWith("-SNAPSHOT");
    }
}
