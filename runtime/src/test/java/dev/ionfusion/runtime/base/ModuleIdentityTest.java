// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

public class ModuleIdentityTest
{
    @Test
    void iterateSingleLevelIdentity()
    {
        ModuleIdentity id = ModuleIdentity.forAbsolutePath("/first");
        checkIteration(id, "first");
    }

    @Test
    void iterateDeepIdentity()
    {
        ModuleIdentity id = ModuleIdentity.forAbsolutePath("/a/b/c/d/e/f/g");
        checkIteration(id, "a", "b", "c", "d", "e", "f", "g");
    }


    private void checkIteration(ModuleIdentity id, String... path)
    {
        Iterator<String> iter = id.iterate();
        for (String n : path)
        {
            assertTrue(iter.hasNext());
            assertEquals(n, iter.next());
        }
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);

        // Calling hasNext is not required for proper iteration.
        iter = id.iterate();
        for (String n : path)
        {
            assertEquals(n, iter.next());
        }
        assertThrows(NoSuchElementException.class, iter::next);
    }
}
