// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionSymbol.BaseSymbol.internSymbols;
import static dev.ionfusion.fusion.FusionSymbol.BaseSymbol.unsafeSymbolsToJavaStrings;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;

interface AnnotatableValue<Self extends AnnotatableValue<Self>>
{
    /**
     * @param annotations must not be null and must not contain elements
     * that are null or annotated.
     */
    Self annotate(Evaluator eval, BaseSymbol[] annotations);

    /**
     * @param annotations must not be null and must not contain null elements.
     */
    default Self annotate(Evaluator eval, String[] annotations)
    {
        return annotate(eval, internSymbols(annotations));
    }

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
