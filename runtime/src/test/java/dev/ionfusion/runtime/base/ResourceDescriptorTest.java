// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static dev.ionfusion.testing.Assertions.assertHashEquals;
import static dev.ionfusion.testing.Assertions.assertNotHashEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ResourceDescriptorTest
{
    private static void checkEquality(Object d1, Object d2)
    {
        assertHashEquals(d1, d1);
        assertHashEquals(d2, d2);
        assertHashEquals(d1, d2);
    }

    private static void checkInequality(Object d1, Object d2)
    {
        assertHashEquals(d1, d1);
        assertHashEquals(d2, d2);

        assertNotHashEquals(d1, d2);
    }


    //==================================================================================
    // Named descriptors

    @Test
    void sameNamedDescriptorsNotEqual()
    {
        ResourceDescriptor desc1 = ResourceDescriptor.named("name");
        ResourceDescriptor desc2 = ResourceDescriptor.named("name");

        checkInequality(desc1, desc2);
    }

    @Test
    void testNamedDescriptor()
    {
        ResourceDescriptor desc = ResourceDescriptor.named("name");
        assertEquals("name", desc.display());
        assertFalse(desc.isUnknown());
        assertNull(desc.getResourceId());


        desc = SourceName.forDisplay("name");
        assertEquals("name", desc.display());
        assertFalse(desc.isUnknown());
        assertNull(desc.getResourceId());
    }

    @Test
    void namedDescriptorMustHaveName()
    {
        assertThrows(NullPointerException.class,
                     () -> ResourceDescriptor.named(null));
        assertThrows(IllegalArgumentException.class,
                     () -> ResourceDescriptor.named(""));
    }

    //==================================================================================
    // SourceNames

    /**
     * Retain legacy equality of SourceName instances, for now at least. The design plan
     * is for ResourceDescriptors to match on their ResourceIdentifier.
     */
    @Test
    void sourceNamesMatchOnDisplay()
    {
        ResourceDescriptor foo1 = SourceName.forDisplay("foo");
        ResourceDescriptor foo2 = SourceName.forDisplay("foo");
        ResourceDescriptor bar  = SourceName.forDisplay("bar");

        checkEquality(foo1, foo2);
        checkInequality(foo1, bar);
    }

    @Test
    void sourceNamesDontMatchNewDescriptors()
    {
        ResourceDescriptor desc = ResourceDescriptor.named("name");
        ResourceDescriptor name = SourceName.forDisplay("name");

        checkInequality(desc, name);
    }


    //==================================================================================
    // Unknown descriptors

    @Test
    void unknownDescriptorsNotEqual()
    {
        ResourceDescriptor unknown1 = ResourceDescriptor.unknown();
        ResourceDescriptor unknown2 = ResourceDescriptor.unknown();

        checkInequality(unknown1, unknown2);
    }

    @Test
    void testUnknownDescriptor()
    {
        ResourceDescriptor unknown = ResourceDescriptor.unknown();
        assertEquals("unknown resource", unknown.display());
        assertTrue(unknown.isUnknown());
        assertNull(unknown.getResourceId());
    }
}
