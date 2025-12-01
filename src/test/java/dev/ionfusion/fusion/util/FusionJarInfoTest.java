// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

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
        assertThat(info.getReleaseLabel(), startsWith("0.3"));
    }
}
