// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

class ResourceDescriptorImpl
{
    private ResourceDescriptorImpl() {}


    //==================================================================================
    // Implementations


    static final class NamedResourceDescriptor
        implements ResourceDescriptor
    {
        private final String myName;

        NamedResourceDescriptor(String name)
        {
            if (name.isEmpty())
            {
                throw new IllegalArgumentException("name must not be empty");
            }
            myName = name;
        }

        @Override
        public String display()
        {
            return myName;
        }

        @Override
        public ResourceIdentifier getResourceId()
        {
            return null;
        }

        // Default equals/hashCode are correct
    }


    static final class UnknownResourceDescriptor
        implements ResourceDescriptor
    {
        @Override
        public String display()
        {
            return "unknown resource";
        }

        @Override
        public ResourceIdentifier getResourceId()
        {
            return null;
        }

        @Override
        public boolean isUnknown()
        {
            return true;
        }

        // Default equals/hashCode are correct
    }
}
