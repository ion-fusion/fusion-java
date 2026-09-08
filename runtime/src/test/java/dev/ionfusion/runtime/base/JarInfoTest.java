// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazon.ion.Timestamp;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Covers the discovery mechanism, which knows nothing of any particular artifact.
 * That our own jars are identified is checked where those artifacts are declared.
 */
public class JarInfoTest
{
    /**
     * Every artifact we report must be fully described; see `describe`.
     */
    @Test
    public void testIdentifiedArtifactsAreComplete()
    {
        Map<String, JarInfo> artifacts = JarInfo.identify();

        // Otherwise the loop below proves nothing.
        assertFalse(artifacts.isEmpty(), "No artifact was identified");

        for (Map.Entry<String, JarInfo> entry : artifacts.entrySet())
        {
            JarInfo info = entry.getValue();

            assertEquals(entry.getKey(), info.getArtifactId());
            assertThat(info.getVersion(),          notNullValue());
            assertThat(info.getBuildDate(),        notNullValue());
            assertThat(info.getCommitDate(),       notNullValue());
            assertThat(info.getShortCommitHash(),  notNullValue());
            assertThat(info.getLongCommitHash(),   notNullValue());
            assertThat(info.getRepositoryStatus(), notNullValue());
        }
    }


    @Test
    public void testResultIsCached()
    {
        assertSame(JarInfo.identify(), JarInfo.identify());
    }


    @Test
    public void testIdentifyIsUnmodifiable()
    {
        Map<String, JarInfo> artifacts = JarInfo.identify();

        assertThrows(UnsupportedOperationException.class,
                     () -> artifacts.remove("whatever"));

        // The shared instance must not have been damaged.
        assertEquals(artifacts, JarInfo.identify());
    }


    @Test
    public void testOfMatchesIdentify()
    {
        for (Map.Entry<String, JarInfo> entry : JarInfo.identify().entrySet())
        {
            assertSame(entry.getValue(), JarInfo.of(entry.getKey()));
        }
    }


    /**
     * An unknown artifact must be reported, not signalled, and never as null.
     */
    @Test
    public void testOfUnknownArtifact()
    {
        JarInfo info = JarInfo.of("no-such-artifact");

        assertEquals("no-such-artifact", info.getArtifactId());
        assertEquals(JarInfo.UNKNOWN, info.getVersion());
        assertEquals(JarInfo.UNKNOWN, info.getRepositoryStatus());
        assertEquals(Timestamp.valueOf("1970-01-01T00:00:00Z"), info.getBuildDate());
        assertEquals(Timestamp.valueOf("1970-01-01T00:00:00Z"), info.getCommitDate());
        assertThat(info.getShortCommitHash(), notNullValue());
        assertThat(info.getLongCommitHash(),  notNullValue());
        assertThat(info.toString(),           containsString(JarInfo.UNKNOWN));
    }
}
