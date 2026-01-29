// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionCollection.BaseCollection;
import dev.ionfusion.fusion.FusionList.BaseList;
import dev.ionfusion.fusion.FusionSexp.BaseSexp;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;


final class FusionSequence
{
    private FusionSequence() {}


    //========================================================================
    // Representation


    abstract static class BaseSequence<Self extends BaseSequence<Self>>
        extends BaseCollection<Self>
    {
        BaseSequence() {}

        BaseSequence(BaseSymbol[] annotations)
        {
            super(annotations);
        }

        /**
         * Assumes that this is a proper sequence, and that the length doesn't
         * extend beyond the end of this sequence or the destination array.
         */
        abstract void unsafeCopy(Evaluator eval, int srcPos, Object[] dest,
                                 int destPos, int length)
            throws FusionException;

        /**
         * Append a number of sequences to this one.
         *
         * @param sequences may be empty.
         * @return a sequence of the same type as this one.
         */
        abstract BaseSequence append(Evaluator eval, Object[] sequences)
            throws FusionException;

        /**
         * Append a number of sequences to this one, stretching this instance
         * if possible.
         *
         * @param sequences may be empty.
         * @return a sequence of the same type as this one.
         */
        BaseSequence appendM(Evaluator eval, Object[] sequences)
            throws FusionException
        {
            return append(eval, sequences);
        }

        /**
         * Append an sexp to this sequence, returning an sexp.
         *
         * @return null if this is an improper sexp.
         */
        abstract <T extends BaseSexp<T>> BaseSexp<?> sexpAppend(Evaluator eval, BaseSexp<T> back)
            throws FusionException;

        /**
         * Second part of double-dispatch from {@link #looseEquals}.
         * @param left is not a null value.
         */
        abstract BaseBool looseEquals2(Evaluator eval, BaseList left)
            throws FusionException;

        /**
         * Second part of double-dispatch from {@link #looseEquals}.
         * @param left is not a null value.
         */
        abstract BaseBool looseEquals2(Evaluator eval, BaseSexp left)
            throws FusionException;

        /**
         * Returns void if the position is out of bounds.
         *
         * @param eval is required in case a subclass needs to create values,
         * for example due to lazy injection.
         */
        abstract Object elt(Evaluator eval, int pos)
            throws FusionException;

        /** Throws if the position is out of bounds. */
        abstract Object unsafeRef(Evaluator eval, int pos)
            throws FusionException;
    }


    //========================================================================
    // Predicates


    static boolean isSequence(Evaluator eval, Object v)
    {
        return (v instanceof BaseSequence);
    }


    //========================================================================
    // Accessors

    /**
     * @param sequence must be a sequence.
     * @return void if the position is out of bounds.
     */
    static Object unsafeSequenceElt(Evaluator eval, Object sequence, int pos)
        throws FusionException
    {
        return ((BaseSequence) sequence).elt(eval, pos);
    }


    //========================================================================
    // Procedure Helpers


    /**
     * @param expectation must not be null.
     * @return the Fusion sequence, not null.
     */
    static Object checkSequenceArg(Evaluator eval,
                                   Procedure who,
                                   String    expectation,
                                   int       argNum,
                                   Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseSequence)
        {
            return arg;
        }

        throw who.argError(eval, expectation, argNum, args);
    }


    /**
     * Checks that an argument is a Fusion sequence, including typed nulls.
     *
     * @return the Fusion sequence, not null.
     */
    static Object checkNullableSequenceArg(Evaluator eval,
                                           Procedure who,
                                           int       argNum,
                                           Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable sequence";
        return checkSequenceArg(eval, who, expectation, argNum, args);
    }


    //========================================================================
    // Procedures


    static final class AppendProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityAtLeast(1, args);
            int arity = args.length;

            Object first = checkNullableSequenceArg(eval, this, 0, args);

            Object[] seqArgs = new Object[arity - 1];

            for (int i = 1; i < arity; i++)
            {
                seqArgs[i - 1] = checkNullableSequenceArg(eval, this, i, args);
            }

            return ((BaseSequence) first).append(eval, seqArgs);
        }
    }

    static final class AppendMProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityAtLeast(1, args);
            int arity = args.length;

            Object first = checkNullableSequenceArg(eval, this, 0, args);

            Object[] seqArgs = new Object[arity - 1];

            for (int i = 1; i < arity; i++)
            {
                seqArgs[i - 1] = checkNullableSequenceArg(eval, this, i, args);
            }

            return ((BaseSequence) first).appendM(eval, seqArgs);
        }
    }
}
