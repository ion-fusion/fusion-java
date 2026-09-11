// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import java.util.Objects;

class BaseResourcePosition
    implements ResourcePosition
{
    private final ResourceDescriptor myDescriptor;

    BaseResourcePosition(ResourceDescriptor desc)
    {
        Objects.requireNonNull(desc, "desc");
        myDescriptor = desc;
    }


    @Override
    public final ResourceDescriptor getResourceDesc()
    {
        return myDescriptor;
    }

    @Override
    public long getLine()
    {
        return 0;
    }

    @Override
    public long getColumn()
    {
        return 0;
    }

    @Override
    public long getOffset()
    {
        return -1;
    }


    /**
     * Returns a view of this object suitable for debugging. For displaying messages to
     * users, use {@link #display()} instead.
     */
    @Override
    public String toString()
    {
        return display();
    }

    public boolean equals(ResourcePosition that)
    {
        return (this == that ||
                (that != null &&
                 myDescriptor.equals(that.getResourceDesc()) &&
                 this.getLine() == that.getLine() &&
                 this.getColumn() == that.getColumn() &&
                 this.getOffset() == that.getOffset()));
    }

    @Override
    public boolean equals(Object that)
    {
        return that instanceof ResourcePosition && this.equals((ResourcePosition) that);
    }


    private static final int HASH_SEED = BaseResourcePosition.class.hashCode();

    @Override
    public int hashCode()
    {
        final int prime  = 8191;
        int       result = HASH_SEED + myDescriptor.hashCode();
        result ^= (result << 29) ^ (result >> 3);
        result = prime * result + (int) getLine();
        result ^= (result << 29) ^ (result >> 3);
        result = prime * result + (int) getColumn();
        result ^= (result << 29) ^ (result >> 3);
        result = prime * result + (int) getOffset();
        result ^= (result << 29) ^ (result >> 3);
        return result;
    }
}
