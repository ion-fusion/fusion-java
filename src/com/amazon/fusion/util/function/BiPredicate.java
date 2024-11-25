// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion.util.function;

/**
 * Simulates Java 8's {@code java.util.function.BiPredicate}.
 * <p>
 * <b>Not for application use!</b>
 * </p>
 */
public interface BiPredicate<T, U>
//  extends java.util.function.BiPredicate
{
    boolean test(T t, U u);
}
