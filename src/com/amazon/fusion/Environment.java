// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.util.Set;

/**
 * Binds identifiers to {@link FusionValue}s.
 * Environments are generally arranged in a hierarchy of lexical scopes.
 */
interface Environment
    extends Store // TODO temporary!
{
    Namespace namespace();


    /** What's the lexical depth?  0 == top-level */
    int getDepth();


    /**
     * NOT RECURSIVE TO ENCLOSING ENVIRONMENTS!
     *
     * @param marks not null.
     *
     * @return given binding if not substituted here; not null.
     */
    Binding substitute(Binding binding, Set<Integer> marks);


    /**
     * Finds the value bound to a given name.
     *
     * @param name must not be null or empty.
     *
     * @return the bound value, or null if there's no value.
     */
    FusionValue lookup(Binding binding);
}
