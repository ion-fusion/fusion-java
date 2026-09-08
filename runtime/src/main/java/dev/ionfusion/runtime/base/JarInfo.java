// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import com.amazon.ion.Timestamp;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Provides build information about the Ion Fusion artifacts on the classpath so
 * that tools and consuming applications can introspect their dependencies.
 * <p>
 * Each of our jars carries one resource describing itself, and all of them use the
 * same resource path, so {@link #identify()} reports <em>every</em> artifact it can
 * see rather than whichever one happens to come first on the classpath. That holds
 * even when our jars have been repacked into a single shaded jar, since the several
 * resources are then merged into one.
 * <p>
 * Nothing here fails. When the build couldn't determine a datum, or when no artifact
 * has the requested id, stand-in values are reported instead; see {@link #UNKNOWN}.
 * No method returns null.
 */
public final class JarInfo
{
    /**
     * Location, within each of our jars, of the resource describing it.
     * Must agree with the {@code buildlogic.jar-info-conventions} Gradle plugin.
     */
    private static final String RESOURCE =
        "META-INF/dev.ionfusion.versions.properties";

    /** Reported in place of any datum that isn't known. */
    public static final String UNKNOWN = "unknown";

    private static final String NO_SHORT_HASH = "0";
    private static final String NO_LONG_HASH  = "0000000000000000000000000000000000000000";
    private static final String NO_DATE       = "1970-01-01T00:00:00Z";


    /**
     * The data the build records about each artifact: the sole source of truth for
     * both their property names and what to report when they aren't known.
     * <p>
     * An artifact lacking any of these is ignored, so that consumers never see a
     * half-described jar.
     */
    private enum Field
    {
        VERSION     ("version",         UNKNOWN),
        BUILD_DATE  ("buildDate",       NO_DATE),
        COMMIT_DATE ("commitDate",      NO_DATE),
        SHORT_HASH  ("shortCommitHash", NO_SHORT_HASH),
        LONG_HASH   ("longCommitHash",  NO_LONG_HASH),
        REPO_STATUS ("repoStatus",      UNKNOWN);

        private final String myPropertyName;
        private final String myUnknownValue;

        Field(String propertyName, String unknownValue)
        {
            myPropertyName = propertyName;
            myUnknownValue = unknownValue;
        }

        /** Gets this datum's name within an artifact's property namespace. */
        String propertyName()
        {
            return myPropertyName;
        }

        /** Gets the value to report when this datum isn't known. */
        String unknownValue()
        {
            return myUnknownValue;
        }
    }


    /**
     * Caches the artifacts visible to this class. The holder idiom defers the work
     * until first use and leaves the thread-safety to the class loader.
     */
    private static final class Holder
    {
        static final Map<String, JarInfo> ARTIFACTS =
            identify(JarInfo.class.getClassLoader());
    }


    private final String    myArtifactId;
    private final String    myVersion;
    private final Timestamp myBuildDate;
    private final Timestamp myCommitDate;
    private final String    myShortCommitHash;
    private final String    myLongCommitHash;
    private final String    myRepositoryStatus;


    /**
     * @param values must hold every {@link Field}.
     */
    private JarInfo(String artifactId, Map<Field, String> values)
    {
        myArtifactId       = artifactId;
        myVersion          = values.get(Field.VERSION);
        myBuildDate        = timestamp(values, Field.BUILD_DATE);
        myCommitDate       = timestamp(values, Field.COMMIT_DATE);
        myShortCommitHash  = values.get(Field.SHORT_HASH);
        myLongCommitHash   = values.get(Field.LONG_HASH);
        myRepositoryStatus = values.get(Field.REPO_STATUS);
    }


    //========================================================================
    // Discovery


    /**
     * Identifies every Ion Fusion artifact visible to this library, keyed by
     * artifact id and ordered by it.
     * <p>
     * The result is computed once and cached.
     *
     * @return an unmodifiable map, empty if no artifact could be identified.
     */
    public static Map<String, JarInfo> identify()
    {
        return Holder.ARTIFACTS;
    }


    /**
     * Gets build information about one artifact.
     *
     * @param artifactId identifies the artifact of interest, as published; for
     *   example, {@code "ion-fusion-runtime"}.
     *
     * @return never null; an instance reporting {@link #UNKNOWN} throughout when the
     *   artifact isn't on the classpath.
     */
    public static JarInfo of(String artifactId)
    {
        JarInfo info = identify().get(artifactId);
        return (info != null ? info : unknown(artifactId));
    }


    //========================================================================
    // Accessors


    /**
     * Gets the id of the artifact described here.
     *
     * @return not null.
     */
    public String getArtifactId()
    {
        return myArtifactId;
    }

    /**
     * Gets the public release label of this artifact.
     * Don't attempt to parse this label; we reserve the right to change its format
     * at any time.
     *
     * @return not null; {@link #UNKNOWN} if the artifact wasn't identified.
     */
    public String getVersion()
    {
        return myVersion;
    }

    /**
     * Gets the time at which this artifact was built.
     * This records when the artifact's build information last changed, which for an
     * unchanged artifact is earlier than the build that produced it.
     *
     * @return not null; the epoch if the date wasn't determined.
     */
    public Timestamp getBuildDate()
    {
        return myBuildDate;
    }

    /**
     * Gets the time of the commit this artifact was built from.
     *
     * @return not null; the epoch if the date wasn't determined.
     */
    public Timestamp getCommitDate()
    {
        return myCommitDate;
    }

    /**
     * Gets the abbreviated hash of the commit this artifact was built from.
     *
     * @return not null; all zeros if the commit wasn't determined.
     */
    public String getShortCommitHash()
    {
        return myShortCommitHash;
    }

    /**
     * Gets the full hash of the commit this artifact was built from.
     *
     * @return not null; all zeros if the commit wasn't determined.
     */
    public String getLongCommitHash()
    {
        return myLongCommitHash;
    }

    /**
     * Gets the state of the repository this artifact was built from.
     *
     * @return not null; {@code "clean"} if the working copy had no modifications,
     *   {@code "dirty"} if it did, or {@link #UNKNOWN}.
     */
    public String getRepositoryStatus()
    {
        return myRepositoryStatus;
    }


    @Override
    public String toString()
    {
        return myArtifactId + " " + myVersion + " (" + myShortCommitHash + ")"
            + ("clean".equals(myRepositoryStatus)
                   ? ""
                   : " (repository: " + myRepositoryStatus + ")");
    }


    //========================================================================


    /**
     * Identifies every Ion Fusion artifact visible to a class loader.
     *
     * @param loader may be null, meaning the system class loader.
     *
     * @return an unmodifiable map, empty if no artifact could be identified.
     */
    private static Map<String, JarInfo> identify(ClassLoader loader)
    {
        Properties props = loadProperties(loader);

        // Each key is `<artifactId>.<field>`, and no field name contains a dot, so
        // everything before the last dot names the artifact. Taking the *last* dot
        // keeps this working should an artifact id ever contain one.
        TreeSet<String> artifactIds = new TreeSet<>();
        for (String key : props.stringPropertyNames())
        {
            int dot = key.lastIndexOf('.');
            if (dot > 0) artifactIds.add(key.substring(0, dot));
        }

        Map<String, JarInfo> artifacts = new TreeMap<>();
        for (String artifactId : artifactIds)
        {
            JarInfo info = describe(props, artifactId);
            if (info != null) artifacts.put(artifactId, info);
        }

        return Collections.unmodifiableMap(artifacts);
    }


    /**
     * Merges every copy of our resource that a class loader can see. Failures are
     * ignored: incomplete information is more useful than none, and this must never
     * be the reason an application fails to start.
     *
     * @param loader may be null, meaning the system class loader.
     */
    private static Properties loadProperties(ClassLoader loader)
    {
        Properties props = new Properties();
        try
        {
            // A null loader means our own class came from the bootstrap loader.
            Enumeration<URL> resources =
                (loader != null
                     ? loader.getResources(RESOURCE)
                     : ClassLoader.getSystemResources(RESOURCE));

            while (resources.hasMoreElements())
            {
                URL url = resources.nextElement();
                try
                {
                    try (InputStream in = url.openStream())
                    {
                        props.load(in);
                    }
                }
                catch (Exception e)
                {
                    // Skip this one; the others may still be readable.
                }
            }
        }
        catch (Exception e)
        {
            // Nothing more we can do.
        }
        return props;
    }


    /**
     * @return null if the properties don't fully describe the artifact.
     */
    private static JarInfo describe(Properties props, String artifactId)
    {
        EnumMap<Field, String> values = new EnumMap<>(Field.class);
        for (Field field : Field.values())
        {
            String value = property(props, artifactId, field);
            if (value == null) return null;
            values.put(field, value);
        }

        return new JarInfo(artifactId, values);
    }


    private static JarInfo unknown(String artifactId)
    {
        EnumMap<Field, String> values = new EnumMap<>(Field.class);
        for (Field field : Field.values())
        {
            values.put(field, field.unknownValue());
        }

        return new JarInfo(artifactId, values);
    }


    /**
     * @return null but not empty string.
     */
    private static String property(Properties props,
                                   String     artifactId,
                                   Field      field)
    {
        String value =
            props.getProperty(artifactId + "." + field.propertyName(), "");
        return (value.isEmpty() ? null : value);
    }


    private static Timestamp timestamp(Map<Field, String> values, Field field)
    {
        try
        {
            return Timestamp.valueOf(values.get(field));
        }
        catch (IllegalArgumentException e)
        {
            // Badly formatted timestamp. Ignore it.
            return Timestamp.valueOf(field.unknownValue());
        }
    }
}
