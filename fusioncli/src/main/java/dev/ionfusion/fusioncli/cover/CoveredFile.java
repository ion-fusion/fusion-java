// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.cover;

import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.ResourceIdentifier;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

/**
 * Represents a source file (either physical or embedded in a Jar) and associated
 * coverage metrics.
 * <p>
 * Assumes at most one module per file.
 */
public class CoveredFile
    extends CoveredEntity
{
    private final ResourceIdentifier myResource;

    /**
     * Tracks the module defined in this file.
     */
    private ModuleIdentity myModuleId;


    private CoveredFile(ResourceIdentifier resource)
    {
        myResource = resource;
    }


    static CoveredFile forUri(URI uri)
    {
        return new CoveredFile(ResourceIdentifier.forUri(uri));
    }


    @Override
    public String describe()
    {
        return myResource.toString();
    }

    public URI getUri()
    {
        return myResource.getUri();
    }

    public Path getPath()
    {
        return myResource.getPath();
    }


    public InputStream readSource()
        throws IOException
    {
        return myResource.openStream();
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
