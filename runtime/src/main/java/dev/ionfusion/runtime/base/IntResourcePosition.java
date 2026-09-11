// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

final class IntResourcePosition
    extends BaseResourcePosition
{
    private final int myLine;
    private final int myColumn;
    private final int myOffset;

    IntResourcePosition(ResourceDescriptor desc, int line, int column, int offset)
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
