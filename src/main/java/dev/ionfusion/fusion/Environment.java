// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import java.util.Set;

/**
 * Maps identifiers to {@link Binding}s.
 * Environments are compile-time entities, arranged in a hierarchy of lexical
 * scopes.
 */
interface Environment
{
    Namespace namespace();


    /** What's the lexical depth?  0 == namespace-level */
    int getDepth();


    /**
     * NOT RECURSIVE TO ENCLOSING ENVIRONMENTS!
     *
     * @param marks not null.
     *
     * @return given binding if not substituted here; not null.
     */
    Binding substitute(Binding binding, Set<MarkWrap> marks);


    /**
     * Finds the effective binding for a free variable.
     *
     * @param marks not null.
     *
     * @return null if the name is not substituted here.
     */
    Binding substituteFree(BaseSymbol name, Set<MarkWrap> marks);
}
