// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.doc.tool;

import com.amazon.ion.Timestamp;
import dev.ionfusion.runtime.base.JarInfo;
import dev.ionfusion.runtime.embed.FusionRuntime;

/**
 * Holds site-wide information available to templates.
 */
public class SiteModel
{
    private final Timestamp myTime;
    private final JarInfo   myJarInfo;

    public SiteModel()
    {
        myTime = Timestamp.nowZ();
        myJarInfo = FusionRuntime.jarInfo();
    }


    public Timestamp getTime()
    {
        return myTime;
    }

    public String getVersion()
    {
        return myJarInfo.getVersion();
    }

    public boolean isDevBuild()
    {
        return getVersion().endsWith("-SNAPSHOT");
    }
}
