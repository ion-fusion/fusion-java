// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.IonType.LIST;
import static com.amazon.ion.IonType.STRING;
import static com.amazon.ion.IonType.STRUCT;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonTextWriterBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Records and persists coverage instrumentation data.
 * <p>
 * Instances are tied to a specific filesystem directory, and assumes full
 * control of its content.  In particular, no other process, and no other
 * {@code CoverageDatabase} instance, should access the directory until the
 * database is flushed via {@link #write()}.  This implies that instances need
 * to be interned or otherwise deduplicated, based on physical directory.
 * At present, {@link _Private_CoverageCollectorImpl} implements these
 * constraints.
 * <p>
 * TODO: The flushing protocol would be more obvious if this class implemented
 *       {@link java.io.Closeable}.
 */
class CoverageDatabase
{
    private static final class SourceNameComparator
        implements Comparator<SourceName>
    {
        @Override
        public int compare(SourceName a, SourceName b)
        {
            return a.display().compareTo(b.display());
        }
    }

    static final Comparator<SourceName> SRCNAME_COMPARE =
        new SourceNameComparator();


    /**
     * Compares locations by line/column, ignoring the offset.
     */
    private static final class SourceLocationComparator
        implements Comparator<SourceLocation>
    {
        @Override
        public int compare(SourceLocation a, SourceLocation b)
        {
            if (a.getLine() < b.getLine()) return -1;
            if (a.getLine() > b.getLine()) return  1;

            if (a.getColumn() < b.getColumn()) return -1;
            if (a.getColumn() > b.getColumn()) return  1;

            return 0;
        }
    }

    static final Comparator<SourceLocation> SRCLOC_COMPARE =
        new SourceLocationComparator();


    //=========================================================================


    private final Path myStorageFile;

    private final Set<File> myRepositories = new HashSet<>();

    private final Map<SourceLocation,Boolean> myLocations = new HashMap<>();


    public CoverageDatabase(Path dataDir)
        throws IOException
    {
        myStorageFile = dataDir.resolve("coverage.ion");

        if (Files.exists(myStorageFile))
        {
            read();
        }
    }


    /**
     * Records a Fusion repository that was used by a runtime while collecting
     * coverage data.
     * The coverage analyzer uses these to synthesize File repositories
     * in order to discover modules that would have been instrumented but were
     * never loaded.
     * <p>
     * TODO: This mechanism should be enhanced to support Jar repositories.
     *
     * @param repoDir must not be null.
     */
    synchronized void noteRepository(File repoDir)
    {
        assert repoDir != null : "repoDir is null";
        myRepositories.add(repoDir);
    }


    synchronized Set<File> getRepositories()
    {
        return myRepositories;
    }


    /**
     * Indicates whether this database can record the given location.
     */
    boolean locationIsRecordable(SourceLocation loc)
    {
        // We can record a location with either a file or a URL.
        SourceName name = loc.getSourceName();
        return name != null && (name.getFile() != null || name.getUrl() != null);
    }


    /**
     * Records that the code at some location has been instrumented.
     *
     * @param loc must be {@linkplain #locationIsRecordable recordable}.
     */
    synchronized void locationInstrumented(SourceLocation loc)
    {
        Boolean prev = myLocations.put(loc, Boolean.FALSE);

        // If already covered, don't un-cover it!
        // TODO This is expensive for repeatedly compiled sources.
        if (prev != null && prev)
        {
            myLocations.put(loc, prev);
        }
    }


    /**
     * Records that the code at some location is about to be evaluated.
     *
     * @param loc must have been {@linkplain #locationInstrumented instrumented}.
     */
    synchronized void locationEvaluated(SourceLocation loc)
    {
        myLocations.put(loc, Boolean.TRUE);
    }


    /** Has a location been covered? */
    synchronized boolean locationCovered(SourceLocation loc)
    {
        Boolean covered = myLocations.get(loc);
        return (covered != null && covered);
    }


    // TODO Collect this eagerly
    synchronized Set<SourceName> sourceNames()
    {
        Set<SourceName> names = new HashSet<>();

        for (SourceLocation loc : myLocations.keySet())
        {
            SourceName name = loc.getSourceName();
            if (name != null)
            {
                names.add(name);
            }
        }

        return names;
    }


    SourceName[] sortedNames()
    {
        Set<SourceName> sourceSet = sourceNames();

        SourceName[] sourceArray = sourceSet.toArray(new SourceName[0]);

        Arrays.sort(sourceArray, SRCNAME_COMPARE);

        return sourceArray;
    }


    /**
     * @return not null.
     */
    synchronized Set<SourceLocation> locations()
    {
        return myLocations.keySet();
    }


    synchronized SourceLocation[] sortedLocations(SourceName name)
    {
        ArrayList<SourceLocation> locsList = new ArrayList<>();

        for (SourceLocation loc : myLocations.keySet())
        {
            if (name.equals(loc.getSourceName()))
            {
                locsList.add(loc);
            }
        }

        SourceLocation[] locsArray = locsList.toArray(new SourceLocation[0]);

        Arrays.sort(locsArray, SRCLOC_COMPARE);

        return locsArray;
    }


    //=====================================================================


    private void writeRepositories(IonWriter iw)
        throws IOException
    {
        iw.addTypeAnnotation("Recorded repositories");
        iw.stepIn(IonType.LIST);
        {
            for (File f : myRepositories)
            {
                String path = f.getAbsolutePath();
                iw.writeString(path);
            }
        }
        iw.stepOut();
    }


    private void writeSourceName(IonWriter iw, SourceName name)
        throws IOException
    {
        iw.stepIn(STRUCT);
        {
            File file = name.getFile();
            if (file != null)
            {
                iw.setFieldName("file");
                iw.writeString(file.getPath());
            }
            else
            {
                URL url = name.getUrl();
                iw.setFieldName("url");
                iw.writeString(url.toExternalForm());
            }

            ModuleIdentity id = name.getModuleIdentity();
            if (id != null)
            {
                iw.setFieldName("module");
                iw.writeString(id.absolutePath());
            }
        }
        iw.stepOut();
    }


    private void writeLocation(IonWriter iw, SourceLocation loc)
        throws IOException
    {
        iw.stepIn(STRUCT);
        {
            long line = loc.getLine();
            long col  = loc.getColumn();

            boolean covered = locationCovered(loc);

            iw.setFieldName("line");
            iw.writeInt(line);

            iw.setFieldName("column");
            iw.writeInt(col);

            iw.setFieldName("covered");
            iw.writeBool(covered);
        }
        iw.stepOut();
    }


    private void writeLocations(IonWriter iw, SourceName name)
        throws IOException
    {
        SourceLocation[] locations = sortedLocations(name);
        assert locations.length != 0;

        iw.stepIn(LIST);
        {
            for (SourceLocation loc : locations)
            {
                writeLocation(iw, loc);
            }
        }
        iw.stepOut();
    }


    private void writeSource(IonWriter iw, SourceName name)
        throws IOException
    {
        iw.stepIn(STRUCT);
        {
            iw.setFieldName("name");
            writeSourceName(iw, name);

            iw.setFieldName("locations");
            writeLocations(iw, name);
        }
        iw.stepOut();
    }


    synchronized void write()
        throws IOException
    {
        Files.createDirectories(myStorageFile.getParent());
        try (OutputStream out = Files.newOutputStream(myStorageFile))
        {
            IonTextWriterBuilder builder =
                IonTextWriterBuilder.minimal()
                                    .withWriteTopLevelValuesOnNewLines(true);
            try (IonWriter iw = builder.build(out))
            {
                writeRepositories(iw);

                for (SourceName name : sourceNames())
                {
                    writeSource(iw, name);
                }
            }
        }
    }


    //=====================================================================


    private void readRepositories(IonReader in)
    {
         in.next();
         assert in.getType() == LIST;
         in.stepIn();
         {
             while (in.next() == STRING)
             {
                 String path = in.stringValue();
                 myRepositories.add(new File(path));
             }
         }
         in.stepOut();
    }


    private SourceName readSourceName(IonReader in)
        throws IOException
    {
        SourceName name;

        assert in.getType() == STRUCT;
        in.stepIn();
        {
            String file   = null;
            URL    url    = null;
            String module = null;

            while (in.next() != null)
            {
                String path = in.stringValue();

                switch (in.getFieldName())
                {
                    // TODO Defend against repeated fields.
                    case "file":
                    {
                        file = path;
                        break;
                    }
                    case "url":
                    {
                        url = new URL(path);
                        break;
                    }
                    case "module":
                    {
                        module = path;
                        break;
                    }
                    default:
                    {
                        // Ignore it.
                        break;
                    }
                }
            }

            if (module != null)
            {
                ModuleIdentity id = ModuleIdentity.forAbsolutePath(module);
                if (file != null)
                {
                    name = SourceName.forModule(id, new File(file));
                }
                else
                {
                    assert url != null;
                    name = SourceName.forUrl(id, url);
                }
            }
            else
            {
                // Without a module ID, this must've been a file-system script.
                assert file != null && url == null;
                name = SourceName.forFile(file);
            }
        }
        in.stepOut();

        return name;
    }


    private void readLocation(IonReader in, SourceName name)
        throws IOException
    {
        assert in.getType() == STRUCT;
        in.stepIn();
        {
            long    line    = 0;
            long    column  = 0;
            boolean covered = false;

            while (in.next() != null)
            {
                switch (in.getFieldName())
                {
                    case "line":
                    {
                        line = in.longValue();
                        break;
                    }
                    case "column":
                    {
                        column = in.longValue();
                        break;
                    }
                    case "covered":
                    {
                        covered = in.booleanValue();
                        break;
                    }
                    default:
                    {
                        // ignore it
                        break;
                    }
                }
            }

            SourceLocation loc =
                SourceLocation.forLineColumn(line, column, name);
            assert loc != null;

            locationInstrumented(loc);
            if (covered)
            {
                locationEvaluated(loc);
            }
        }
        in.stepOut();
    }


    private void readLocations(IonReader in, SourceName name)
        throws IOException
    {
        assert in.getType() == LIST;
        in.stepIn();
        {
            while (in.next() != null)
            {
                readLocation(in, name);
            }
        }
        in.stepOut();
    }


    private void readSource(IonReader in)
        throws IOException
    {
        assert in.getType() == STRUCT;
        in.stepIn();
        {
            SourceName name = null;

            while (in.next() != null)
            {
                switch (in.getFieldName())
                {
                    case "name":
                    {
                        name = readSourceName(in);
                        break;
                    }
                    case "locations":
                    {
                        // TODO I'm too lazy to handle out-of-order fields.
                        assert name != null;
                        readLocations(in, name);
                        break;
                    }
                    default:
                    {
                        // Ignore it.
                        break;
                    }
                }
            }
        }
        in.stepOut();
    }


    private void read()
        throws IOException
    {
        try (InputStream is = Files.newInputStream(myStorageFile))
        {
            try (IonReader ir = IonReaderBuilder.standard().build(is))
            {
                readRepositories(ir);

                while (ir.next() != null)
                {
                    readSource(ir);
                }
            }
        }
    }
}
