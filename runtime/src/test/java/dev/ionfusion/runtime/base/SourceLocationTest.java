// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static dev.ionfusion.runtime.base.ResourceIdentifier.forFile;
import static dev.ionfusion.testing.Assertions.assertHashEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonSystem;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonSystemBuilder;
import org.junit.jupiter.api.Test;


public class SourceLocationTest
{
    private void checkPath(String path, ResourcePosition pos)
    {
        assertHashEquals(ResourceIdentifier.forFile(path),
                         pos.getResourceDesc().getResourceId());
    }

    private void assertNoLocation(IonReader ir)
    {
        ResourceDescriptor desc = ResourceDescriptor.named("test source");
        ResourcePosition loc = SourceLocation.forCurrentSpan(ir, desc);
        assertSame(desc, loc.getResourceDesc());
        assertEquals("unknown position of test source", loc.display());

        desc = ResourceDescriptor.identified(forFile("/dummy/path"));
        loc = SourceLocation.forCurrentSpan(ir, desc);
        assertSame(desc, loc.getResourceDesc());
        checkPath("/dummy/path", loc);
        assertEquals("unknown position of /dummy/path", loc.display());
    }

    private void assertLocation(String expectedOffsets, IonReader ir)
    {
        ResourceDescriptor desc = ResourceDescriptor.unknown();
        ResourcePosition loc = SourceLocation.forCurrentSpan(ir, desc);
        assertSame(desc, loc.getResourceDesc());
        assertEquals(expectedOffsets, loc.display());

        desc = ResourceDescriptor.named("test source");
        loc = SourceLocation.forCurrentSpan(ir, desc);
        assertSame(desc, loc.getResourceDesc());
        assertEquals(expectedOffsets + " of test source", loc.display());

        desc = ResourceDescriptor.identified(forFile("/dummy/path"));
        loc = SourceLocation.forCurrentSpan(ir, desc);
        assertSame(desc, loc.getResourceDesc());
        assertEquals(expectedOffsets + " of /dummy/path", loc.display());
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


    private void checkLocation(ResourcePosition loc, String display,
                               long line, long column, long offset)
    {
        // Normalize sentinels.
        if (line   < 1) line = column = 0;
        if (column < 1) column = 0;
        if (offset < 0) offset = -1;

        assertEquals(line,   loc.getLine(),   "line");
        assertEquals(column, loc.getColumn(), "column");
        assertEquals(offset, loc.getOffset(), "offset");

        ResourceDescriptor name = loc.getResourceDesc();
        if (display == null)
        {
            display = "unknown position";
            if (!name.isUnknown())
            {
                display += " of " + name.display();
            }
        }
        else if (!name.isUnknown())
        {
            display += " of " + name.display();
        }

        assertEquals(display, loc.display(), "display");
    }

    private void assertNoLineColumn(long line, long column)
    {
        ResourceDescriptor desc = ResourceDescriptor.unknown();
        ResourcePosition loc = SourceLocation.forLineColumn(line, column, desc);
        assertSame(desc, loc.getResourceDesc());
        checkLocation(loc, null, 0, 0, -1);

        desc = ResourceDescriptor.named("test source");
        loc = SourceLocation.forLineColumn(line, column, desc);
        assertSame(desc, loc.getResourceDesc());
        checkLocation(loc, null, 0, 0, -1);
    }

    private void assertLineColumn(String display, long line, long column)
    {
        ResourceDescriptor desc = ResourceDescriptor.unknown();
        ResourcePosition loc = SourceLocation.forLineColumn(line, column, desc);
        assertSame(desc, loc.getResourceDesc());
        checkLocation(loc, display, line, column, -1);

        desc = ResourceDescriptor.named("test source");
        loc = SourceLocation.forLineColumn(line, column, desc);
        assertSame(desc, loc.getResourceDesc());
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


    @Test
    void forNameRequiresDescriptor()
        throws Exception
    {
        assertThrows(NullPointerException.class,
                     () -> SourceLocation.forName(null));
    }

    @Test
    void forLineColumnRequiresDescriptor()
        throws Exception
    {
        assertThrows(NullPointerException.class,
                     () -> SourceLocation.forLineColumn(1, 2, null));
    }

    @Test
    void forCurrentSpanRequiresDescriptor()
        throws Exception
    {
        IonReaderBuilder builder = IonReaderBuilder.standard();
        try (IonReader reader = builder.build("{}"))
        {
            assertThrows(NullPointerException.class,
                         () -> SourceLocation.forCurrentSpan(reader, null));
        }
    }
}
