// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.testing;

import static dev.ionfusion.testing.Assertions.assertHashEquals;
import static dev.ionfusion.testing.Assertions.assertNotHashEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AssertionsTest
{
    @Test
    void handleNullInAssertHashEquals()
    {
        assertHashEquals(null, null);
        assertThrows(AssertionError.class,
                     () -> assertHashEquals(null, new Object()));
        assertThrows(AssertionError.class,
                     () -> assertHashEquals(new Object(), null));
    }


    @Test
    void handleNullInAssertNotHashEquals()
    {
        // Objects are the same, thus equal, thus fail
        assertThrows(AssertionError.class,
                     () -> assertNotHashEquals(null, null));

        assertNotHashEquals(null, new Object());
        assertNotHashEquals(new Object(), null);
    }
}
