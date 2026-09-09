// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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
    public static final Timestamp EPOCH_TIME = Timestamp.forMillis(0, 0);

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
     * Examine some hard-coded version properties.
     */
    @Test
    public void verifyTestVersions()
    {
        JarInfo info = JarInfo.of("good.artifact");
        assertThat(info, notNullValue());
        assertThat(info.getVersion(), is("test version"));
        // This property is present but has malformed value
        assertEquals(EPOCH_TIME, info.getBuildDate());

        // If any property is absent, the whole artifact becomes unknown.
        checkUnknownArtifact("missing-version");
    }


    /**
     * An unknown artifact must be reported, not signalled, and never as null.
     */
    @Test
    public void testOfUnknownArtifact()
    {
        checkUnknownArtifact("no-such-artifact");
    }

    private static void checkUnknownArtifact(String artifactId)
    {
        JarInfo info = JarInfo.of(artifactId);

        assertEquals(artifactId, info.getArtifactId());
        assertEquals(JarInfo.UNKNOWN, info.getVersion());
        assertEquals(JarInfo.UNKNOWN, info.getRepositoryStatus());
        assertEquals(EPOCH_TIME, info.getBuildDate());
        assertEquals(EPOCH_TIME, info.getCommitDate());
        assertThat(info.getShortCommitHash(), notNullValue());
        assertThat(info.getLongCommitHash(),  notNullValue());
        assertThat(info.toString(),           containsString(JarInfo.UNKNOWN));
    }
}
