// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

final class LongResourcePosition
    extends BaseResourcePosition
{
    private final long myLine;
    private final long myColumn;
    private final long myOffset;

    LongResourcePosition(ResourceDescriptor desc, long line, long column, long offset)
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
