// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static dev.ionfusion.testing.Assertions.assertHashEquals;
import static dev.ionfusion.testing.Assertions.assertNotHashEquals;

import org.junit.jupiter.api.Test;

public class ResourceDescriptorTest
{
    @Test
    void unknownDescriptorsNotEqual()
    {
        ResourceDescriptor unknown1 = ResourceDescriptor.unknown();
        ResourceDescriptor unknown2 = ResourceDescriptor.unknown();

        assertHashEquals(unknown1, unknown1);
        assertHashEquals(unknown2, unknown2);

        assertNotHashEquals(unknown1, unknown2);
    }


    /**
     * Retain legacy equality of SourceName instances, for now at least. The design plan
     * is for ResourceDescriptors to match on their ResourceIdentifier.
     */
    @Test
    void sourceNamesMatchOnDisplay()
    {
        SourceName foo1 = SourceName.forDisplay("foo");
        SourceName foo2 = SourceName.forDisplay("foo");
        SourceName bar  = SourceName.forDisplay("bar");

        assertHashEquals(foo1, foo1);
        assertHashEquals(foo1, foo2);

        assertNotHashEquals(foo1, bar);
    }
}
