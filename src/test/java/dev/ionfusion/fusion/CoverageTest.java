// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class CoverageTest
    extends CoreTestCase
{
    static class Collector
        implements _Private_CoverageCollector
    {
        boolean instrumentOnlyLineOne = false;

        final Set<SourceLocation> instrumented = new HashSet<>();
        final Set<SourceLocation> evaluated    = new HashSet<>();

        @Override
        public boolean locationIsRecordable(SourceLocation loc)
        {
            return (instrumentOnlyLineOne ? loc.getLine() == 1 : true);
        }

        @Override
        public void locationInstrumented(SourceLocation loc)
        {
            // For simplicity, we'll ignore the offset.
            SourceLocation loc2 =
                SourceLocation.forLineColumn(loc.getLine(),
                                             loc.getColumn(),
                                             loc.getSourceName());
            instrumented.add(loc2);
        }

        @Override
        public void locationEvaluated(SourceLocation loc)
        {
            // For simplicity, we'll ignore the offset.
            SourceLocation loc2 =
                SourceLocation.forLineColumn(loc.getLine(),
                                             loc.getColumn(),
                                             loc.getSourceName());
            evaluated.add(loc2);
        }

        @Override
        public void flushMetrics()
        {
        }
    }


    private final Collector collector = new Collector();


    /**
     * @param line one-based
     * @param column one-based
     */
    private void checkCovered(SourceName name, long line, long column)
    {
        SourceLocation loc = SourceLocation.forLineColumn(line, column, name);
        assertTrue(collector.instrumented.contains(loc));
        assertTrue(collector.evaluated.contains(loc));
    }


    /**
     * @param line one-based
     * @param column one-based
     */
    private void checkCovered(long line, long column)
    {
        checkCovered(null, line, column);
    }


    /**
     * @param line one-based
     * @param column one-based
     */
    private void checkNotCovered(SourceName name, long line, long column)
    {
        SourceLocation loc = SourceLocation.forLineColumn(line, column, name);
        assertTrue(collector.instrumented.contains(loc));
        assertFalse(collector.evaluated.contains(loc));
    }


    /**
     * @param line one-based
     * @param column one-based
     */
    private void checkNotCovered(long line, long column)
    {
        checkNotCovered(null, line, column);
    }

    /**
     * @param line one-based
     * @param column one-based
     */
    private void checkNotInstrumented(SourceName name, long line, long column)
    {
        SourceLocation loc = SourceLocation.forLineColumn(line, column, name);
        assertFalse(collector.instrumented.contains(loc));
        assertFalse(collector.evaluated.contains(loc));
    }


    /**
     * @param line one-based
     * @param column one-based
     */
    private void checkNotInstrumented(long line, long column)
    {
        checkNotInstrumented(null, line, column);
    }


    @Override
    protected FusionRuntimeBuilder runtimeBuilder()
        throws FusionException
    {
        FusionRuntimeBuilder b = super.runtimeBuilder();

        b.setCoverageCollector(collector);

        return b;
    }


    @Test
    public void testCollection()
        throws FusionException
    {
        TopLevel top = topLevel();

        eval("0");
        checkCovered(1, 1);

        //    1 3 5 7 9
        eval("(if true\n" +
             "    1 2)");
        checkCovered   (1, 1);
        checkCovered   (1, 5);
        checkCovered   (2, 5);
        checkNotCovered(2, 7);

        SourceName name1 = SourceName.forDisplay("define");
        //        1 3 5 7 9
        top.eval("(define (f t)\n" +
                 "  (if t      \n" +
                 "      1      \n" +
                 "      2))",
                 name1);
        checkCovered   (name1, 1, 1);
        checkNotCovered(name1, 2, 3);
        checkNotCovered(name1, 2, 7);
        checkNotCovered(name1, 3, 7);
        checkNotCovered(name1, 4, 7);

        top.call("f", true);
        checkCovered   (name1, 2, 3);
        checkCovered   (name1, 2, 7);
        checkCovered   (name1, 3, 7);
        checkNotCovered(name1, 4, 7);

        SourceName name2 = SourceName.forDisplay("invoke");
        //        1 3 5 7 9
        top.eval("(f false)",
                 name2);
        checkCovered(name2, 1, 1);
        checkCovered(name2, 1, 2);
        checkCovered(name2, 1, 4);
        checkCovered(name1, 4, 7);
    }

    @Test
    public void testPartialInstrumentation()
        throws FusionException
    {
        collector.instrumentOnlyLineOne = true;

        //    1 3 5 7 9
        eval("(if true\n" +
             "    1 2)");
        checkCovered        (1, 1);
        checkCovered        (1, 5);
        checkNotInstrumented(2, 5);
        checkNotInstrumented(2, 7);
    }
}
