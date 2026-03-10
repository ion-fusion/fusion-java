// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.util;

/**
 * Provides static empty arrays to eliminate useless allocations.
 */
public class Empties
{
    private Empties() { }

    public static final byte[]   EMPTY_BYTE_ARRAY   = new byte[0];
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
}
