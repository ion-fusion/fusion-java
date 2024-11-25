// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazon.fusion.FusionException;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class FusionJarInfoTest
{
    @Test
    public void testConstruction()
        throws FusionException
    {
        FusionJarInfo info = new FusionJarInfo();

        // When running on a laptop that can't run our Ant targets (due to
        // unavailable Brazil CLI and HappyTrails stuff) this test will fail.
        // Adding -DNOBRAZIL to the Eclipse Installed JRE will allow us to
        // succeed in that case.
        if (System.getProperty("NOBRAZIL") != null) return;

        assertTrue(info.getReleaseLabel().startsWith("R"));
    }
}
