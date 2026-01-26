// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonSystem;
import com.amazon.ion.system.IonSystemBuilder;
import org.junit.jupiter.api.Test;


public class SourceLocationTest
{
    private void assertNoLocation(IonReader ir)
    {
        SourceLocation loc = SourceLocation.forCurrentSpan(ir);
        assertNull(loc, "expected null SourceLocation");

        loc = SourceLocation.forCurrentSpan(ir, null);
        assertNull(loc, "expected null SourceLocation");

        SourceName name = SourceName.forDisplay("test source");
        loc = SourceLocation.forCurrentSpan(ir, name);
        assertEquals("unknown location in test source", loc.display());
    }

    private void assertLocation(String expectedOffsets, IonReader ir)
    {
        SourceLocation loc = SourceLocation.forCurrentSpan(ir);
        assertEquals(expectedOffsets, loc.display());

        loc = SourceLocation.forCurrentSpan(ir, null);
        assertEquals(expectedOffsets, loc.display());

        SourceName name = SourceName.forDisplay("test source");
        loc = SourceLocation.forCurrentSpan(ir, name);
        assertEquals(expectedOffsets + " of test source", loc.display());
    }


    @Test
    public void testReaderLocationDisplay()
    {
        IonSystem sys = IonSystemBuilder.standard().build();
        IonDatagram dg = sys.getLoader().load("(hi)");

        // Binary reader doesn't display offsets.
        // TODO Display offsets in binary data.

        IonReader ir = sys.newReader(dg.getBytes());

        assertNoLocation(ir);  // Before first value

        ir.next();
        assertNoLocation(ir);

        ir.stepIn();
        assertNoLocation(ir);  // Before first child


        // Text reader gives line/column locations

        ir = sys.newReader("(hi)");

        assertNoLocation(ir);  // Before first value

        ir.next();
        assertLocation("1st line, 1st column", ir);

        ir.stepIn();
        assertNoLocation(ir);  // Before first child

        ir.next();
        assertLocation("1st line, 2nd column", ir);

        ir.next();
        assertNoLocation(ir);  // After last child


        // TODO test reading from DOM
    }


    private void checkLocation(SourceLocation loc, String display,
                               long line, long column, long offset)
    {
        // Normalize sentinels.
        if (line   < 1) line = column = 0;
        if (column < 1) column = 0;
        if (offset < 0) offset = -1;

        assertEquals(line,   loc.getLine(),   "line");
        assertEquals(column, loc.getColumn(), "column");
        assertEquals(offset, loc.getStartOffset(), "offset");

        SourceName name = loc.getSourceName();
        if (display == null)
        {
            display = "unknown location";
            if (name != null)
            {
                display += " in " + name.display();
            }
        }
        else if (name != null)
        {
            display += " of " + name.display();
        }

        assertEquals(display, loc.display(), "display");
    }

    private void assertNoLineColumn(long line, long column)
    {
        SourceLocation loc = SourceLocation.forLineColumn(line, column);
        assertNull(loc, "SourceLocation");

        loc = SourceLocation.forLineColumn(line, column, null);
        assertNull(loc, "SourceLocation");

        SourceName name = SourceName.forDisplay("test source");
        loc = SourceLocation.forLineColumn(line, column, name);
        assertSame(name, loc.getSourceName());
        checkLocation(loc, null, 0, 0, -1);
    }

    private void assertLineColumn(String display, long line, long column)
    {
        SourceLocation loc = SourceLocation.forLineColumn(line, column);
        assertSame(null, loc.getSourceName());
        checkLocation(loc, display, line, column, -1);

        loc = SourceLocation.forLineColumn(line, column, null);
        assertSame(null, loc.getSourceName());
        checkLocation(loc, display, line, column, -1);

        SourceName name = SourceName.forDisplay("test source");
        loc = SourceLocation.forLineColumn(line, column, name);
        assertSame(name, loc.getSourceName());
        checkLocation(loc, display, line, column, -1);
    }

    @Test
    public void testTextOffsets()
    {
        // Equivalent unknown line and column.
        assertNoLineColumn(-1, -1);
        assertNoLineColumn(-1,  0);
        assertNoLineColumn( 0, -1);
        assertNoLineColumn( 0,  0);

        // When line is unknown, column is ignored.
        assertNoLineColumn(-1,  5);
        assertNoLineColumn( 0,  6);

        assertLineColumn("1st line",  1,  0);
        assertLineColumn("2nd line",  2, -1);
        assertLineColumn("3rd line, 4th column",  3, 4);
    }
}
