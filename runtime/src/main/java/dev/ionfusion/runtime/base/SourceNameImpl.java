// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import dev.ionfusion.runtime.base.ResourceDescriptorImpl.AbstractResourceDescriptor;

class SourceNameImpl
    extends AbstractResourceDescriptor
    implements SourceName
{
    private final String myDisplay;


    SourceNameImpl(String display)
    {
        myDisplay = display;
    }


    @Override
    public String display()
    {
        return myDisplay;
    }

    @Override
    public ResourceIdentifier getResourceId()
    {
        return null;
    }

    @Override
    public ModuleIdentity getModuleIdentity()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return myDisplay;
    }


    //=========================================================================
    // Subclasses that add ResourceIdentifier and ModuleIdentity
    // This complexity only serves to reduce memory use.

    static class ResourceSourceName
        extends SourceNameImpl
    {
        private final ResourceIdentifier myResource;

        private ResourceSourceName(String display, ResourceIdentifier resource)
        {
            super(display);
            myResource = resource;
        }

        ResourceSourceName(ResourceIdentifier resource)
        {
            this(resource.toString(), resource);
        }

        @Override
        public ResourceIdentifier getResourceId()
        {
            return myResource;
        }
    }


    static class ModuleSourceName
        extends ResourceSourceName
    {
        private final ModuleIdentity myId;

        ModuleSourceName(ResourceIdentifier rsrc, ModuleIdentity id)
        {
            super(rsrc);
            myId   = id;
        }

        @Override
        public ModuleIdentity getModuleIdentity() { return myId; }
    }
}
