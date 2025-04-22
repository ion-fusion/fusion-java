// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ionfusion.fusion.FusionException;
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
        assertTrue(info.getReleaseLabel().startsWith("0.3"));
    }
}
