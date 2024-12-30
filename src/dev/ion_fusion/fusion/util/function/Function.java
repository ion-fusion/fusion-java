// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ion_fusion.fusion.util.function;

/**
 * Simulates Java 8's {@code java.util.function.Function}.
 * <p>
 * <b>Not for application use!</b>
 */
public interface Function<T, R>
{
    R apply(T t);
}
