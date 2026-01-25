// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionNumber.makeInt;

import dev.ionfusion.fusion.FusionSexp.ImmutablePair;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;


final class FusionCollection
{
    private FusionCollection() {}


    //========================================================================
    // Representation Classes


    /**
     * TODO PERF Annotation capability should move down; it adds space overhead
     *   to every collection, most notably to {@link ImmutablePair}.
     */
    abstract static class BaseCollection<Self extends BaseCollection<Self>>
        extends BaseValue
        implements AnnotatableValue<Self>
    {
        /** Not null */
        final BaseSymbol[] myAnnotations;

        BaseCollection()
        {
            myAnnotations = BaseSymbol.EMPTY_ARRAY;
        }

        BaseCollection(BaseSymbol[] annotations)
        {
            assert annotations != null;
            myAnnotations = annotations;
        }

        @Override
        public final boolean isAnnotated()
        {
            return myAnnotations.length != 0;
        }

        @Override
        public final BaseSymbol[] getAnnotations()
        {
            return myAnnotations;
        }

        abstract int size()
            throws FusionException;
    }


    //========================================================================
    // Constructors


    //========================================================================
    // Predicates


    static boolean isCollection(Evaluator eval, Object v)
    {
        return (v instanceof BaseCollection);
    }


    //========================================================================
    // Accessors


    static int unsafeCollectionSize(Evaluator eval, Object collection)
        throws FusionException
    {
        return ((BaseCollection) collection).size();
    }


    //========================================================================
    // Procedure Helpers


    /**
     * @param expectation must not be null.
     * @return the Fusion collection, not null.
     */
    static Object checkCollectionArg(Evaluator eval,
                                     Procedure who,
                                     String    expectation,
                                     int       argNum,
                                     Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseCollection)
        {
            return arg;
        }

        throw who.argError(eval, expectation, argNum, args);
    }


    /**
     * @return the Fusion collection, not null.
     */
    static Object checkNullableCollectionArg(Evaluator eval,
                                             Procedure who,
                                             int       argNum,
                                             Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable collection";
        return checkCollectionArg(eval, who, expectation, argNum, args);
    }


    //========================================================================
    // Procedures


    static final class SizeProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            checkNullableCollectionArg(eval, this, 0, arg);
            int size = unsafeCollectionSize(eval, arg);
            return makeInt(eval, size);
        }
    }
}
