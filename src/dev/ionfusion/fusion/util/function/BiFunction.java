// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.function;

/**
 * Simulates Java 8's {@code java.util.function.BiFunction}.
 * <p>
 * <b>Not for application use!</b>
 * </p>
 */
public interface BiFunction<T, U, R>
{
    R apply(T t, U u);
}
