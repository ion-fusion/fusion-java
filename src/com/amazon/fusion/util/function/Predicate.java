// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion.util.function;

/**
 * Simulates Java 8's {@code java.util.function.Predicate}.
 * <p>
 * <b>Not for application use!</b>
 */
public interface Predicate<T>
{
    boolean test(T t);
}
