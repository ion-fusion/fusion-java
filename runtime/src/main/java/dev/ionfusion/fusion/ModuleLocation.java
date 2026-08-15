// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import com.amazon.ion.IonException;
import com.amazon.ion.IonReader;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.SourceName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

/**
 * Abstract location of module source code.
 * <p>
 * These are produced by a {@link ModuleRepository} upon module discovery,
 * encapsulating the physical location of the module code.  They are consumed by
 * the {@link LoadHandler}, when loading the module.
 * <p>
 * "ModuleResource" might be a better name, or perhaps "ModuleCodeProvider".
 * <p>
 * TODO: {@link SourceName} should track the repository holding the module.
 *    This would enable better messaging, cover Jar-based resources nicely, and
 *    allow the coverage report to partition source-files by repo root.
 */
abstract class ModuleLocation
{
    private final SourceName myName;

    private ModuleLocation(SourceName name)
    {
        assert name.getModuleIdentity() != null;
        myName = name;
    }


    /**
     * @return not null.
     */
    final SourceName sourceName()
    {
        return myName;
    }


    abstract InputStream openStream()
        throws IOException;

    IonReader openReader(Evaluator eval)
        throws FusionException
    {
        try
        {
            IonReader reader = null;
            InputStream in = openStream();
            try
            {
                reader = eval.getIonReaderBuilder().build(in);
                return reader;
            }
            finally
            {
                if (reader == null)
                {
                    // We failed constructing the IonReader!
                    in.close();
                }
            }
        }
        catch (IOException | IonException e)
        {
            String where = myName.getModuleIdentity().toString();
            String message =
                "Error loading " + where + ": " + e.getMessage();
            throw new FusionException(message, e);
        }
    }


    @Override
    public String toString()
    {
        return myName.toString();
    }


    /**
     * @param id must not be null.
     * @param sourceFile must not be null.
     * @return a new {@link ModuleLocation}.
     */
    static ModuleLocation forFile(ModuleIdentity id, File sourceFile)
    {
        return new FileModuleLocation(id, sourceFile);
    }

    /**
     * @param id must not be null.
     * @param url must not be null.
     * @return a new {@link ModuleLocation}.
     */
    static ModuleLocation forUrl(ModuleIdentity id, URL url)
    {
        // TODO We may want to handle jar: URLs specially, so we can distinguish
        //  the Jar's file-system path and the internal resource path.

        return url.getProtocol().equals("file")
                   ? new FileModuleLocation(id, urlToFile(url))
                   : new UrlModuleLocation(id, url);
    }


    private static File urlToFile(URL url)
    {
        try
        {
            return new File(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException("Malformed `file:` URL", e);
        }
    }


    private static final class FileModuleLocation
        extends ModuleLocation
    {
        public FileModuleLocation(ModuleIdentity id, File sourceFile)
        {
            super(SourceName.forModule(id, sourceFile));
        }

        @Override
        InputStream openStream()
            throws IOException
        {
            return Files.newInputStream(sourceName().getPath());
        }
    }


    private static final class UrlModuleLocation
        extends ModuleLocation
    {
        public UrlModuleLocation(ModuleIdentity id, URL url)
        {
            super(SourceName.forUrl(id, url));
        }

        @Override
        InputStream openStream()
            throws IOException
        {
            return sourceName().getUri().toURL().openStream();
        }
    }
}
