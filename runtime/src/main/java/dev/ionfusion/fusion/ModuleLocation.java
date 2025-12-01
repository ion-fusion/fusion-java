// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import com.amazon.ion.IonReader;
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
 * "ModuleResource" might be a better name, or perhaps "ModuleCodeProvider"
 * since they can only be consumed once (see {@link #read}).
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
        this.myName = name;
    }


    /**
     * @return may be null.
     */
    final SourceName sourceName()
    {
        return myName;
    }


    /**
     * This method may only be called once per instance.
     * The result may be positioned on the value to be read.
     */
    abstract IonReader read(Evaluator eval)
        throws IOException;


    /**
     * Returns the directory (if any) containing the module.  This is used to
     * set the load-relative-directory when evaluating a module declaration.
     * <p>
     * At present, this doesn't work for Jar-bundled resources.
     * Perhaps this mechanism should use URLs instead of Strings.
     */
    String parentDirectory()
    {
        return null;
    }


    @Override
    public String toString()
    {
        SourceName name = sourceName();
        return (name == null ? super.toString() : name.toString());
    }

    static ModuleLocation forIonReader(IonReader source, SourceName name)
    {
        return new IonReaderModuleLocation(source, name);
    }

    static ModuleLocation forFile(ModuleIdentity id, File sourceFile)
    {
        return new FileModuleLocation(id, sourceFile);
    }


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


    private static final class IonReaderModuleLocation
        extends ModuleLocation
    {
        private final IonReader  mySource;

        IonReaderModuleLocation(IonReader source, SourceName name)
        {
            super(name);
            mySource = source;
        }

        @Override
        IonReader read(Evaluator eval)
        {
            return mySource;
        }
    }


    private abstract static class InputStreamModuleLocation
        extends ModuleLocation
    {
        InputStreamModuleLocation(SourceName name)
        {
            super(name);
        }

        abstract InputStream open()
            throws IOException;

        @Override
        IonReader read(Evaluator eval)
            throws IOException
        {
            IonReader reader = null;
            InputStream in = open();
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
    }


    private static final class FileModuleLocation
        extends InputStreamModuleLocation
    {
        public FileModuleLocation(ModuleIdentity id, File sourceFile)
        {
            super(SourceName.forModule(id, sourceFile));
        }

        @Override
        InputStream open()
            throws IOException
        {
            return Files.newInputStream(sourceName().getFile().toPath());
        }

        @Override
        String parentDirectory()
        {
            return sourceName().getFile().getParentFile().getAbsolutePath();
        }
    }


    private static final class UrlModuleLocation
        extends InputStreamModuleLocation
    {
        public UrlModuleLocation(ModuleIdentity id, URL url)
        {
            super(SourceName.forUrl(id, url));
        }

        @Override
        InputStream open()
            throws IOException
        {
            return sourceName().getUrl().openStream();
        }
    }
}
