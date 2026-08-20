// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static dev.ionfusion.runtime.base.ResourceIdentifier.forFile;
import static dev.ionfusion.testing.Assertions.assertHashEquals;
import static dev.ionfusion.testing.Assertions.assertNotHashEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
    // Identified descriptors

    void checkIdentified(ResourceIdentifier id)
    {
        ResourceDescriptor desc = ResourceDescriptor.identified(id);
        assertEquals(id.toString(), desc.display());
        assertFalse(desc.isUnknown());
        assertEquals(id, desc.getResourceId());
    }

    @Test
    void testIdentifiedDescriptor()
    {
        checkIdentified(forFile("/rsrc"));
        checkIdentified(ResourceIdentifier.forUri("file:///rsrc"));
        checkIdentified(ResourceIdentifier.forUri("http://example.com/rsrc"));
    }

    @Test
    void sameUriMeansEqual()
    {
        ResourceIdentifier id1  = forFile("/rsrc");
        ResourceIdentifier id2  = ResourceIdentifier.forUri("file:///rsrc");
        assertNotSame(id1, id2);
        checkEquality(id1, id2);

        ResourceDescriptor desc1 = ResourceDescriptor.identified(id1);
        ResourceDescriptor desc2 = ResourceDescriptor.identified(id2);
        assertNotSame(desc1, desc2);
        checkEquality(desc1, desc2);
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


        desc = ResourceDescriptor.named("name");
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

    @Test
    void namedAndIdentifiedAreNotEqual()
    {
        ResourceDescriptor fooNamed = ResourceDescriptor.named("/foo");
        ResourceDescriptor fooFile =
            ResourceDescriptor.identified(ResourceIdentifier.forFile("/foo"));

        assertEquals(fooNamed.display(), fooFile.display());
        checkInequality(fooNamed, fooFile);
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
