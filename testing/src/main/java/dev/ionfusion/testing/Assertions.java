// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class Assertions
{
    private Assertions() {}


    /**
     * Asserts that the given objects are {@link #equals} and have the same
     * {@link #hashCode()}s.
     */
    public static void assertHashEquals(Object a, Object b)
    {
        assertEquals(a, b); // Check symmetry of equals()
        assertEquals(b, a);

        assertEquals(a.hashCode(), b.hashCode(), "hashCode");
    }

    /**
     * Asserts that the given objects are not {@link #equals} and have different
     * {@link #hashCode}s.
     */
    public static void assertNotHashEquals(Object a, Object b)
    {
        assertNotSame(a, b);

        assertNotEquals(a, b); // Check symmetry of equals()
        assertNotEquals(b, a);

        assertNotEquals(a.hashCode(), b.hashCode(), "hashCode");
    }
}
