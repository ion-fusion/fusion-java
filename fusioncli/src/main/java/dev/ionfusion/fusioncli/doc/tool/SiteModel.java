// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.doc.tool;

import com.amazon.ion.Timestamp;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.FusionJarInfo;

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
