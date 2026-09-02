// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

class ResourceDescriptorImpl
{
    private ResourceDescriptorImpl() {}


    //==================================================================================
    // Implementations

    /**
     * Satisfies the equals/hashCode contract for descriptors.
     */
    abstract static class AbstractResourceDescriptor
        implements ResourceDescriptor
    {
        public final boolean equals(ResourceDescriptor that)
        {
            if (this == that) { return true; }
            if (that == null) { return false; }

            ResourceIdentifier thisId = this.getResourceId();
            ResourceIdentifier thatId = that.getResourceId();
            return thisId != null && thatId != null && thisId.equals(thatId);
        }

        @Override
        public final boolean equals(Object that)
        {
            return (that instanceof ResourceDescriptor &&
                    this.equals((ResourceDescriptor) that));
        }


        private static final int HASH_SEED = AbstractResourceDescriptor.class.hashCode();

        @Override
        public final int hashCode()
        {
            ResourceIdentifier id = getResourceId();
            if (id == null)
            {
                return System.identityHashCode(this);
            }
            else
            {
                int result = HASH_SEED + id.hashCode();
                result ^= (result << 29) ^ (result >> 3);
                return result;
            }
        }
    }


    private abstract static class UnidentifiedResourceDescriptor
        extends AbstractResourceDescriptor
    {
        @Override
        public final ResourceIdentifier getResourceId()
        {
            return null;
        }
    }


    static final class NamedResourceDescriptor
        extends UnidentifiedResourceDescriptor
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
    }


    static final class UnknownResourceDescriptor
        extends UnidentifiedResourceDescriptor
    {
        @Override
        public String display()
        {
            return "unknown resource";
        }

        @Override
        public boolean isUnknown()
        {
            return true;
        }
    }
}
