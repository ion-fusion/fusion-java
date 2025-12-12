// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionSymbol.BaseSymbol.unsafeSymbolsToJavaStrings;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;

interface AnnotatableValue
{
    /**
     * @param annotations must not be null and must not contain elements
     * that are null or annotated.
     *
     * @throws UnsupportedOperationException if this value isn't annotatable.
     */
    Object annotate(Evaluator eval, BaseSymbol[] annotations)
        throws FusionException;

    /**
     * Determines whether this value has actual annotations.
     */
    default boolean isAnnotated()
        throws FusionException
    {
        return false;
    }

    /**
     * @return the annotation symbols; not null.
     * <b>Must not be modified by the caller!</b>
     */
    default BaseSymbol[] getAnnotations()
        throws FusionException
    {
        // TODO should return ImmutableList of BaseSymbol
        return BaseSymbol.EMPTY_ARRAY;
    }

    default String[] getAnnotationsAsJavaStrings()
        throws FusionException
    {
        return unsafeSymbolsToJavaStrings(getAnnotations());
    }
}
