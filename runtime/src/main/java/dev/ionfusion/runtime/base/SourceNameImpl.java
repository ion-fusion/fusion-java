// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

class SourceNameImpl
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


    public boolean equals(SourceName other)
    {
        return (other != null && myDisplay.equals(other.display()));
    }

    @Override
    public boolean equals(Object other)
    {
        return (other instanceof SourceName && equals((SourceName) other));
    }


    private static final int HASH_SEED = SourceNameImpl.class.hashCode();

    @Override
    public int hashCode()
    {
        int result = HASH_SEED + myDisplay.hashCode();
        result ^= (result << 29) ^ (result >> 3);
        return result;
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
