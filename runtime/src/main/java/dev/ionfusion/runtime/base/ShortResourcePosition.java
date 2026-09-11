// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

final class ShortResourcePosition
    extends BaseResourcePosition
{
    private final short myLine;
    private final short myColumn;
    private final short myOffset;

    ShortResourcePosition(ResourceDescriptor desc,
                          short line,
                          short column,
                          short offset)
    {
        super(desc);
        myLine   = line;
        myColumn = column;
        myOffset = offset;
    }

    @Override
    public long getLine()
    {
        return myLine;
    }

    @Override
    public long getColumn()
    {
        return myColumn;
    }

    @Override
    public long getOffset()
    {
        return myOffset;
    }
}
