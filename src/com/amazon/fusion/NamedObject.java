// Copyright (c) 2023 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

/**
 * Fusion objects that support {@code object_name}.
 *
 * @see ObjectNameProc
 */
interface NamedObject
{
    /**
     * Produces a Fusion value that's the object's name for the purpose of the
     * {@code object_name} function.
     * The result must not change between invocations.
     *
     * @return the object name, or void; not (Java) null.
     */
    Object objectName(Evaluator eval);
}
