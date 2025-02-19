// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class FusionRegExpTest {
    @Test
    public void testPrivateConstructor()
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // ensure the constructor is private
        final Constructor constructor = FusionRegExp.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());

        try {
            // then set it to public for coverage
            constructor.setAccessible(true);
            constructor.newInstance();
        } finally {
            constructor.setAccessible(false);
        }
    }
}
