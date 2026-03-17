// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.cover;

import static java.nio.file.Files.newInputStream;

import dev.ionfusion.runtime.base.ModuleIdentity;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents a source file (either physical or embedded in a Jar) and associated
 * coverage metrics.
 * <p>
 * Assumes at most one module per file.
 */
public class CoveredFile
    extends CoveredEntity
{
    private final URI  myUri;
    private final Path myPath;

    /**
     * Tracks the module defined in this file.
     */
    private ModuleIdentity myModuleId;


    private CoveredFile(URI uri, Path path)
    {
        assert uri != null;
        myUri = uri;
        myPath = path.normalize();
    }


    static CoveredFile forUri(URI uri)
    {
        String scheme = uri.getScheme();
        if ("file".equalsIgnoreCase(scheme))
        {
            return new CoveredFile(uri, Paths.get(uri));
        }
        if ("jar".equalsIgnoreCase(scheme))
        {
            return new CoveredFile(uri, null);
        }

        throw new IllegalArgumentException("URI must have file or jar scheme: " + uri);
    }


    @Override
    public String describe()
    {
        return myPath.toString();
    }

    public URI getUri()
    {
        return myUri;
    }

    public Path getPath()
    {
        return myPath;
    }


    public InputStream readSource()
        throws IOException
    {
        return myPath != null ? newInputStream(myPath) : myUri.toURL().openStream();
    }


    public void containsModule(CoveredModule module)
    {
        assert myModuleId == null;
        myModuleId = module.getId();
    }


    /**
     * A file is a script if it's not associated with a module.
     */
    public boolean isScript()
    {
        return myModuleId == null;
    }

    public ModuleIdentity getModuleIdentity()
    {
        return myModuleId;
    }
}
