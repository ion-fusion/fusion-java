// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.testing;

import static dev.ionfusion.testing.Reflect.assertEqualProperties;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


public class ReflectTest
{
    private static class Bean
    {
        private String s = "default";
        private Integer i;

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }

        public Integer getI() {
            return i;
        }

        public void setI(Integer i) {
            this.i = i;
        }
    }


    @Test
    public void testAssertEqualProperties()
    {
        Bean a = new Bean();
        Bean b = new Bean();
        assertEqualProperties(a, b);

        a.setI(12);
        b.setI(12);
        assertEqualProperties(a, b);

        a.setS("foo");

        Throwable e = assertThrows(AssertionError.class,
                                   () -> assertEqualProperties(a, b));
        assertTrue(e.getMessage().contains("property s"));
    }
}
