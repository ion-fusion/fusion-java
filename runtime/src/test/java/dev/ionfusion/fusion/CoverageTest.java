// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ionfusion.runtime._private.cover.CoverageCollector;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ResourceDescriptor;
import dev.ionfusion.runtime.base.ResourcePosition;
import dev.ionfusion.runtime.embed.TopLevel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class CoverageTest
    extends CoreTestCase
{
    static class Collector
        implements CoverageCollector
    {
        boolean instrumentOnlyLineOne = false;

        final Map<ResourcePosition, AtomicInteger> instrumented = new HashMap<>();

        @Override
        public boolean locationIsRecordable(ResourcePosition pos)
        {
            return (!instrumentOnlyLineOne || pos.getLine() == 1);
        }

        @Override
        public AtomicInteger locationInstrumented(ResourcePosition pos)
        {
            // For simplicity, we'll ignore the offset.
            pos = ResourcePosition.forPosition(pos.getResourceDesc(),
                                               pos.getLine(),
                                               pos.getColumn(),
                                               -1);
            return instrumented.computeIfAbsent(pos, l ->new AtomicInteger());
        }
    }


    private final Collector collector = new Collector();


    /**
     * @param line one-based
     * @param column one-based
     */
    private void checkCovered(ResourceDescriptor name, long line, long column)
    {
        ResourcePosition loc = ResourcePosition.forPosition(name, line, column, -1);
        assertTrue(collector.instrumented.get(loc).get() > 0);
    }


    /**
     * @param line one-based
     * @param column one-based
     */
    private void checkNotCovered(ResourceDescriptor name, long line, long column)
    {
        ResourcePosition loc = ResourcePosition.forPosition(name, line, column, -1);
        assertEquals(0, collector.instrumented.get(loc).get());
    }


    /**
     * @param line one-based
     * @param column one-based
     */
    private void checkNotInstrumented(ResourceDescriptor name, long line, long column)
    {
        ResourcePosition loc = ResourcePosition.forPosition(name, line, column, -1);
        assertNull(collector.instrumented.get(loc));
    }



    @Override
    protected StandardFusionRuntimeBuilder runtimeBuilder()
        throws FusionException
    {
        StandardFusionRuntimeBuilder b =
            (StandardFusionRuntimeBuilder) super.runtimeBuilder();

        b.setCoverageCollector(collector);

        return b;
    }


    @Test
    public void testCollection()
        throws FusionException
    {
        TopLevel top = topLevel();

        ResourceDescriptor desc = ResourceDescriptor.named("testCollection");
        eval("0", desc);
        checkCovered(desc,1, 1);

        desc = ResourceDescriptor.unknown();
        //    1 3 5 7 9
        eval("(if true\n" +
             "    1 2)",
             desc);
        checkCovered   (desc, 1, 1);
        checkCovered   (desc, 1, 5);
        checkCovered   (desc, 2, 5);
        checkNotCovered(desc, 2, 7);

        ResourceDescriptor name1 = ResourceDescriptor.named("define");
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

        ResourceDescriptor name2 = ResourceDescriptor.named("invoke");
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

        ResourceDescriptor desc = ResourceDescriptor.named("partial");
        //    1 3 5 7 9
        eval("(if true\n" +
             "    1 2)",
             desc);
        checkCovered        (desc, 1, 1);
        checkCovered        (desc, 1, 5);
        checkNotInstrumented(desc, 2, 5);
        checkNotInstrumented(desc, 2, 7);
    }
}
