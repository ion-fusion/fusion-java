// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Simple class for neatly printing tabular data.
 */
class TablePrinter
{
    private final ArrayList<String[]> myRows = new ArrayList<>();

    private int myIndentation = 0;
    private final int myGutterWidth = 2;

    private String myBlanks = "                    ";


    //=========================================================================
    // Configuration

    public void setIndent(int indent)
    {
        assert indent >= 0;
        myIndentation = indent;
    }

    public void addRow(String[] elements)
    {
        myRows.add(elements);
    }

    //=========================================================================
    // Rendering

    public void render(Appendable out)
        throws IOException
    {
        int[] columnWidths = computeColumnWidths();

        for (String[] row : myRows)
        {
            renderBlanks(myIndentation, out);

            for (int col = 0; col < row.length; col++)
            {
                String cell = row[col];
                out.append(cell);

                // Don't pad after the last column in the row.
                if (col+1 != row.length)
                {
                    int paddingNeeded = columnWidths[col] - cell.length();

                    renderBlanks(paddingNeeded + myGutterWidth, out);
                }
            }

            out.append('\n');
        }
    }


    private int[] computeColumnWidths()
    {
        // This assumes that there is at least one row, and that the first
        // row has the most columns.  This should usually be the case with headers.
        int columnCount = myRows.get(0).length;

        int[] columnWidths = new int[columnCount];

        for (String[] row : myRows)
        {
            for (int col = 0; col < row.length; col++)
            {
                int cellWidth = row[col].length();

                if (columnWidths[col] < cellWidth)
                {
                    columnWidths[col] = cellWidth;
                }
            }
        }

        return columnWidths;
    }

    private void renderBlanks(int count, Appendable out)
        throws IOException
    {
        if (count > 0)
        {
            while (myBlanks.length() < count)
            {
                myBlanks += myBlanks;
            }

            out.append(myBlanks, 0, count);
        }
    }
}
