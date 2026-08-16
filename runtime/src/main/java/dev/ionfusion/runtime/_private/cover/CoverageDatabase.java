// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.cover;

import static com.amazon.ion.IonType.LIST;
import static com.amazon.ion.IonType.STRING;
import static com.amazon.ion.IonType.STRUCT;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.newDirectoryStream;

import com.amazon.ion.IonException;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonTextWriterBuilder;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.SourceLocation;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Collects, reads, and writes coverage instrumentation data.
 */
public class CoverageDatabase
    implements CoverageCollector
{
    private final Set<File> myRepositories = ConcurrentHashMap.newKeySet();

    private final Map<URI, CovResource> myResources = new ConcurrentHashMap<>();

    public CoverageDatabase()
    {
    }


    public void loadSessions(Path dataDir)
        throws IOException
    {
        Path sessionsDir = dataDir.resolve("sessions");
        if (Files.exists(sessionsDir))
        {
            try (DirectoryStream<Path> stream = newDirectoryStream(sessionsDir))
            {
                for (Path p : stream)
                {
                    if (isRegularFile(p))
                    {
                        loadSession(p);
                    }
                }
            }
        }
    }


    /**
     * Records a Fusion repository used by a runtime while collecting
     * coverage data.
     * The coverage analyzer opens file-based repositories to discover modules
     * that would have been instrumented but were never loaded.
     * <p>
     * TODO: This mechanism should be enhanced to support Jar repositories.
     *
     * @param repoDir must not be null.
     */
    void noteRepository(File repoDir)
    {
        assert repoDir != null : "repoDir is null";
        myRepositories.add(repoDir);
    }


    public Set<File> getRepositories()
    {
        return myRepositories;
    }


    /**
     * Indicates whether this database can record the given location.
     */
    @Override
    public boolean locationIsRecordable(SourceLocation loc)
    {
        // We can record locations within identified resources.
        return loc.getResourceId() != null;
    }


    private CovResource resourceInstrumented(URI uri)
    {
        return myResources.computeIfAbsent(uri, CovResource::new);
    }

    /**
     * Records that the code at some location has been instrumented.
     *
     * @param loc must be {@linkplain #locationIsRecordable recordable}.
     */
    @Override
    public AtomicInteger locationInstrumented(SourceLocation loc)
    {
        URI uri = loc.getResourceId().getUri();
        long offset = loc.getStartOffset();
        ModuleIdentity module = loc.getModuleIdentity();

        return resourceInstrumented(uri).containsModule(module)
                                        .offsetInstrumented(offset);
    }


    //=====================================================================


    public interface ResourceVisitor
    {
        void visit(URI uri, ModuleIdentity module);
    }

    public void forEachResource(ResourceVisitor visitor)
    {
        myResources.forEach((uri, cov) -> {
            visitor.visit(uri, cov.moduleId);
        });
    }


    public interface CoverageEntryVisitor
    {
        void visit(URI uri, long offset, ModuleIdentity module, AtomicInteger count);
    }

    public void forEachCoverageEntry(CoverageEntryVisitor visitor)
    {
        myResources.forEach((uri, cov) -> {
            cov.offsets.forEach((offset, count) -> {
                visitor.visit(uri, offset, cov.moduleId, count);
            });
        });
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

    private void writeSourceName(IonWriter iw, CovResource rsrc)
        throws IOException
    {
        iw.stepIn(STRUCT);
        {
            URI uri = rsrc.uri;
            iw.setFieldName("uri");
            iw.writeString(uri.toString());

            ModuleIdentity id = rsrc.moduleId;
            if (id != null)
            {
                iw.setFieldName("module");
                iw.writeString(id.absolutePath());
            }
        }
        iw.stepOut();
    }

    private void writeLocation(IonWriter iw, long offset, Number coverage)
        throws IOException
    {
        iw.stepIn(STRUCT);
        {
            assert offset >= 0;

            boolean covered = coverage.longValue() > 0;

            iw.setFieldName("offset");
            iw.writeInt(offset);

            iw.setFieldName("covered");
            iw.writeBool(covered);
        }
        iw.stepOut();
    }


    private void writeLocations(IonWriter iw, CovResource rsrc)
        throws IOException
    {
        iw.stepIn(LIST);
        {
            for (Map.Entry<Long, AtomicInteger> entry : rsrc.offsets.entrySet())
            {
                writeLocation(iw, entry.getKey(), entry.getValue());
            }
        }
        iw.stepOut();
    }


    private void writeSource(IonWriter iw, CovResource rsrc)
        throws IOException
    {
        iw.stepIn(STRUCT);
        {
            iw.setFieldName("name");
            writeSourceName(iw, rsrc);

            iw.setFieldName("locations");
            writeLocations(iw, rsrc);
        }
        iw.stepOut();
    }


    synchronized void write(Path myStorageFile)
        throws IOException
    {
        try (OutputStream out = Files.newOutputStream(myStorageFile))
        {
            IonTextWriterBuilder builder =
                IonTextWriterBuilder.minimal()
                                    .withWriteTopLevelValuesOnNewLines(true);
            try (IonWriter iw = builder.build(out))
            {
                writeRepositories(iw);

                for (CovResource rsrc : myResources.values())
                {
                   writeSource(iw, rsrc);
                }
            }
        }
    }

    void uncheckedWrite(Path storageFile)
        throws UncheckedIOException
    {
        try
        {
            write(storageFile);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
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


    private CovResource readSourceName(IonReader in)
    {
        CovResource rsrc;

        assert in.getType() == STRUCT;
        in.stepIn();
        {
            URI uri = null;
            ModuleIdentity module = null;

            while (in.next() != null)
            {
                String path = in.stringValue();

                switch (in.getFieldName())
                {
                    // TODO Defend against repeated fields.
                    case "uri":
                    {
                        uri = URI.create(path);
                        break;
                    }
                    case "module":
                    {
                        module = ModuleIdentity.forAbsolutePath(path);
                        break;
                    }
                    default:
                    {
                        // Ignore it.
                        break;
                    }
                }
            }

            rsrc = resourceInstrumented(uri).containsModule(module);
        }
        in.stepOut();

        return rsrc;
    }


    private void readLocation(IonReader in, CovResource rsrc)
    {
        assert in.getType() == STRUCT;
        in.stepIn();
        {
            long    offset  = -1;
            boolean covered = false;

            while (in.next() != null)
            {
                switch (in.getFieldName())
                {
                    case "offset":
                    {
                        offset = in.longValue();
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
            assert offset >= 0;

            // Record the location even if it isn't covered
            AtomicInteger counter = rsrc.offsetInstrumented(offset);
            if (covered)
            {
                counter.getAndIncrement();
            }
        }
        in.stepOut();
    }


    private void readLocations(IonReader in, CovResource rsrc)
    {
        assert in.getType() == LIST;
        in.stepIn();
        {
            while (in.next() != null)
            {
                readLocation(in, rsrc);
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
            CovResource rsrc = null;

            while (in.next() != null)
            {
                switch (in.getFieldName())
                {
                    case "name":
                    {
                        rsrc = readSourceName(in);
                        break;
                    }
                    case "locations":
                    {
                        // TODO I'm too lazy to handle out-of-order fields.
                        assert rsrc != null;
                        readLocations(in, rsrc);
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


    private void loadSession(Path session)
        throws IOException
    {
        try (InputStream is = Files.newInputStream(session))
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
        catch (IOException | IonException e)
        {
            String msg = "Error reading coverage data at " + session;
            throw new IOException(msg, e);
        }
    }


    private static class CovResource
    {
        final URI                uri;
        ModuleIdentity           moduleId;
        Map<Long, AtomicInteger> offsets = new ConcurrentHashMap<>();

        public CovResource(URI uri)
        {
            assert uri != null;
            this.uri = uri;
        }

        AtomicInteger offsetInstrumented(long startOffset)
        {
            return offsets.computeIfAbsent(startOffset, o -> new AtomicInteger());
        }

        CovResource containsModule(ModuleIdentity id)
        {
            if (moduleId == null)
            {
                moduleId = id;
            }
            // We currently support at most one module per resource.
            assert id == null || id == moduleId;
            return this;
        }
    }
}
