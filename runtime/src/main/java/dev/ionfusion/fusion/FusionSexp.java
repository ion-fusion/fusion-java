// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionBool.trueBool;
import static dev.ionfusion.fusion.FusionCompare.EqualityTier.LOOSE_EQUAL;
import static dev.ionfusion.fusion.FusionCompare.EqualityTier.STRICT_EQUAL;
import static dev.ionfusion.fusion.FusionCompare.EqualityTier.TIGHT_EQUAL;
import static dev.ionfusion.fusion.FusionIo.dispatchIonize;
import static dev.ionfusion.fusion.FusionIo.dispatchWrite;
import static dev.ionfusion.fusion.FusionSymbol.BaseSymbol.internSymbols;
import static dev.ionfusion.fusion.FusionVoid.voidValue;

import com.amazon.ion.IonSequence;
import com.amazon.ion.IonSexp;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionCompare.EqualityTier;
import dev.ionfusion.fusion.FusionIterator.AbstractIterator;
import dev.ionfusion.fusion.FusionList.BaseList;
import dev.ionfusion.fusion.FusionSequence.BaseSequence;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.SourceLocation;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


final class FusionSexp
{
    private FusionSexp() {}


    static final EmptySexp EMPTY_SEXP = new EmptySexp();
    static final NullSexp  NULL_SEXP  = new NullSexp();


    /**
     * Returns an unannotated, immutable {@code null.sexp}.
     */
    static NullSexp nullSexp(Evaluator eval)
    {
        return NULL_SEXP;
    }

    static NullSexp nullSexp(Evaluator eval, BaseSymbol[] annotations)
    {
        return NULL_SEXP.annotate(eval, annotations);
    }

    static BaseSexp nullSexp(Evaluator eval, String[] annotations)
    {
        return NULL_SEXP.annotate(eval, annotations);
    }

    /**
     * Returns an unannotated, immutable empty sexp: {@code ()}.
     */
    static EmptySexp emptySexp(Evaluator eval)
    {
        return EMPTY_SEXP;
    }

    static EmptySexp emptySexp(Evaluator eval, BaseSymbol[] annotations)
    {
        return EMPTY_SEXP.annotate(eval, annotations);
    }

    static BaseSexp emptySexp(Evaluator eval, String[] annotations)
    {
        return EMPTY_SEXP.annotate(eval, annotations);
    }


    /**
     * Caller must have injected children.
     */
    static BaseSexp immutableSexp(Evaluator eval, Object[] elements)
    {
        BaseSexp c = EMPTY_SEXP;

        int i = elements.length;
        while (i-- != 0)
        {
            c = new ImmutablePair(elements[i], c);
        }

        return c;
    }


    /**
     * Caller must have injected children.
     * @param elements must not be null.
     */
    static BaseSexp immutableSexp(Evaluator eval, List<?> elements)
    {
        BaseSexp c = EMPTY_SEXP;

        int i = elements.size();
        while (i-- != 0)
        {
            c = new ImmutablePair(elements.get(i), c);
        }

        return c;
    }


    /**
     * Caller must have injected children.
     *
     * @param annotations must not be null.
     * @param elements must not be null.
     */
    static BaseSexp immutableSexp(Evaluator    eval,
                                  BaseSymbol[] annotations,
                                  Object[]     elements)
    {
        if (elements.length == 0)
        {
            return emptySexp(eval, annotations);
        }

        BaseSexp c = EMPTY_SEXP;

        int i = elements.length;
        while (--i != 0)
        {
            c = new ImmutablePair(elements[i], c);
        }

        c = new ImmutablePair(annotations, elements[0], c);

        return c;
    }

    /**
     * Caller must have injected children.
     *
     * @param annotations must not be null.
     * @param elements must not be null.
     */
    static BaseSexp immutableSexp(Evaluator eval,
                                  String[]  annotations,
                                  Object[]  elements)
    {
        return immutableSexp(eval, internSymbols(annotations), elements);
    }


    /**
     * Caller must have injected children.
     *
     * @param annotations must not be null.
     * @param elements must not be null.
     */
    static BaseSexp immutableSexp(Evaluator    eval,
                                  BaseSymbol[] annotations,
                                  List<?>      elements)
    {
        int size = elements.size();
        if (size == 0)
        {
            return emptySexp(eval, annotations);
        }

        BaseSexp c = EMPTY_SEXP;

        int i = size;
        while (--i != 0)
        {
            c = new ImmutablePair(elements.get(i), c);
        }

        c = new ImmutablePair(annotations, elements.get(0), c);

        return c;
    }

    /**
     * Caller must have injected children.
     *
     * @param annotations must not be null.
     * @param elements must not be null.
     */
    static BaseSexp immutableSexp(Evaluator eval,
                                  String[]  annotations,
                                  List<?>   elements)
    {
        return immutableSexp(eval, internSymbols(annotations), elements);
    }


    static ImmutablePair pair(Evaluator eval, Object head, Object tail)
    {
        return new ImmutablePair(head, tail);
    }


    static ImmutablePair pair(Evaluator eval, BaseSymbol[] annotations,
                              Object head, Object tail)
    {
        return new ImmutablePair(annotations, head, tail);
    }

    static ImmutablePair pair(Evaluator eval, String[] annotations,
                              Object head, Object tail)
    {
        return pair(eval, internSymbols(annotations), head, tail);
    }


    static Object sexpFromIonSequence(Evaluator eval, IonSequence seq)
    {
        String[] annotations = seq.getTypeAnnotations();

        if (seq.isNullValue())
        {
            return nullSexp(eval, annotations);
        }

        int i = seq.size();
        if (i == 0)
        {
            return emptySexp(eval, annotations);
        }

        Object sexp = EMPTY_SEXP;
        while (i-- != 0)
        {
            IonValue iv = seq.get(i);
            Object head = eval.inject(iv);
            if (i == 0)
            {
                sexp = pair(eval, annotations, head, sexp);
            }
            else
            {
                sexp = pair(eval, head, sexp);
            }
        }

        return sexp;
    }


    //========================================================================
    // Predicates

    static boolean isSexp(Evaluator eval, Object v)
    {
        return (v instanceof BaseSexp);
    }

    static boolean isEmptySexp(Evaluator eval, Object v)
    {
        return (v instanceof EmptySexp);
    }

    static boolean isNullSexp(Evaluator eval, Object v)
    {
        return (v instanceof NullSexp);
    }

    static boolean isPair(Evaluator eval, Object v)
    {
        return (v instanceof ImmutablePair);
    }


    //========================================================================
    // Accessors


    static int unsafeSexpSize(Evaluator eval, Object sexp)
        throws FusionException
    {
        return ((BaseSexp) sexp).size(eval);
    }


    static Object unsafePairDot(Evaluator eval, Object pair, int pos)
        throws FusionException
    {
        ImmutablePair p = (ImmutablePair) pair;
        for ( ; pos > 0; pos--)
        {
            Object tail = p.myTail;
            if (tail instanceof ImmutablePair)
            {
                p = (ImmutablePair) tail;
            }
            else
            {
                return voidValue(eval);
            }
        }
        return p.myHead;
    }


    static Object unsafePairHead(Evaluator eval, Object pair)
        throws FusionException
    {
        return ((ImmutablePair) pair).myHead;
    }


    static Object unsafePairTail(Evaluator eval, Object pair)
        throws FusionException
    {
        return ((ImmutablePair) pair).myTail;
    }

    /**
     * Performs <em>n</em> tail operations.
     * That is, traverse <em>n</em> elements from the front of a sexp.
     */
    static Object unsafePairTailN(Evaluator eval, Object pair, int n)
        throws FusionException
    {
        while (n-- != 0)
        {
            pair = unsafePairTail(eval, pair);
        }
        return pair;
    }


    static Object unsafeSexpAdd(Evaluator eval, Object sexp, Object value)
    {
        // TODO copy annotations?
        if (sexp instanceof NullSexp)
        {
            return new ImmutablePair(value, EMPTY_SEXP);
        }

        return new ImmutablePair(value, sexp);
    }

    static FusionIterator unsafeSexpIterator(Evaluator eval, Object sexp)
        throws FusionException
    {
        BaseSexp c = (BaseSexp) sexp;
        return new SexpIterator(c);
    }


    //========================================================================
    // Transformers


    /**
     * Extracts the values out of a Fusion sexp.
     *
     * @param sexp must be a proper sexp; it is not type-checked!
     *
     * @return null if given {@code null.sexp}, otherwise a list of the values
     *  in the sexp.
     *
     * @throws FusionException
     */
    static List<Object> unsafeSexpToJavaList(Evaluator eval, Object sexp)
        throws FusionException
    {
        if (isNullSexp(eval, sexp)) return null;

        List<Object> elems = new LinkedList<>();

        while (isPair(eval, sexp))
        {
            elems.add(unsafePairHead(eval, sexp));

            sexp = unsafePairTail(eval, sexp);
        }

        return elems;
    }


    /**
     * @param sexp must be a proper sexp; it is not type-checked!
     */
    static IonSexp unsafeCopyToIonSexp(Object sexp,
                                       ValueFactory factory,
                                       boolean throwOnConversionFailure)
        throws FusionException
    {
        BaseSexp base = (BaseSexp) sexp;
        return base.copyToIonValue(factory, throwOnConversionFailure);
    }

    /**
     * @param sexp must be a proper sexp; it is not type-checked!
     */
    static IonSexp unsafeCopyToIonSexp(Object sexp, ValueFactory factory)
        throws FusionException
    {
        BaseSexp base = (BaseSexp) sexp;
        return base.copyToIonValue(factory, true);
    }

    /**
     * @param sexp must be a proper sexp; it is not type-checked!
     *
     * @return null if the sexp and its contents cannot be ionized.
     */
    static IonSexp unsafeCopyToIonSexpMaybe(Object sexp,
                                            ValueFactory factory)
        throws FusionException
    {
        BaseSexp base = (BaseSexp) sexp;
        return base.copyToIonValue(factory, false);
    }


    //========================================================================


    static abstract class BaseSexp<Self extends BaseSexp<Self>>
        extends BaseSequence<Self>
    {
        BaseSexp() {}

        BaseSexp(BaseSymbol[] annotations)
        {
            super(annotations);
        }

        @Override
        int size(Evaluator eval) throws FusionException { return 0; }

        @Override
        Object elt(Evaluator eval, int pos)
            throws FusionException
        {
            return voidValue(eval);
        }

        @Override
        Object unsafeRef(Evaluator eval, int pos)
            throws FusionException
        {
            String message =
                "No index " + pos + " in sequence " + this;
            throw new IndexOutOfBoundsException(message);
        }

        @Override
        void unsafeCopy(Evaluator eval, int srcPos, Object[] dest,
                        int destPos, int length)
            throws FusionException
        {
            assert length == 0;
        }

        /**
         * @return null if this is not a proper sexp.
         */
        @Override
        <T extends BaseSexp<T>> BaseSexp<?> sexpAppend(Evaluator eval, BaseSexp<T> back)
            throws FusionException
        {
            return back.annotate(eval, myAnnotations);
        }

        @Override
        BaseSexp append(Evaluator eval, Object[] sequences)
            throws FusionException
        {
            int len = sequences.length;
            if (len == 0) return this;

            BaseSexp back = EMPTY_SEXP;
            for (int i = len - 1 ; i >= 0; i--)
            {
                back = ((BaseSequence) sequences[i]).sexpAppend(eval, back);
                if (back == null)
                {
                    throw new ArgumentException("append", "proper sequence",
                                                i+1, sequences[i]);
                }
            }

            BaseSexp result = sexpAppend(eval, back);
            if (result == null)
            {
                throw new ArgumentException("append", "proper sequence",
                                            0, this);
            }
            return result;
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (this == right) return trueBool(eval);

            if (right instanceof BaseSequence)
            {
                BaseSequence r = (BaseSequence) right;
                return r.looseEquals2(eval, this);
            }
            return falseBool(eval);
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            return SyntaxSexp.makeOriginal(eval, loc, this);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator      eval,
                                       SyntaxSymbol   context,
                                       SourceLocation loc)
            throws FusionException
        {
            assert size(eval) == 0;

            SyntaxValue stx = SyntaxSexp.make(eval, loc, this);
            return Syntax.applyContext(eval, context, stx);
        }

        @Override
        abstract IonSexp copyToIonValue(ValueFactory factory,
                                        boolean throwOnConversionFailure)
            throws FusionException;
    }


    private static final class NullSexp
        extends BaseSexp<NullSexp>
    {
        private NullSexp() {}

        NullSexp(BaseSymbol[] annotations)
        {
            super(annotations);
        }

        @Override
        boolean isAnyNull()
        {
            return true;
        }

        @Override
        public NullSexp annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return NULL_SEXP;
            return new NullSexp(annotations);
        }

        @Override
        BaseSexp append(Evaluator eval, Object[] sequences)
            throws FusionException
        {
            BaseSexp empty = emptySexp(eval, myAnnotations);
            return empty.append(eval, sequences);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return makeBool(eval, right instanceof NullSexp);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseList left)
            throws FusionException
        {
            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseSexp left)
            throws FusionException
        {
            return falseBool(eval);
        }

        @Override
        IonSexp copyToIonValue(ValueFactory factory,
                              boolean throwOnConversionFailure)
            throws FusionException
        {
            IonSexp sexp = factory.newNullSexp();
            sexp.setTypeAnnotations(getAnnotationsAsJavaStrings());
            return sexp;
        }

        @Override
        void write(Evaluator eval, Appendable out) throws IOException
        {
            writeAnnotations(out, myAnnotations);
            out.append("null.sexp");
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, FusionException
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());
            out.writeNull(IonType.SEXP);
        }
    }


    private static final class EmptySexp
        extends BaseSexp<EmptySexp>
    {
        private EmptySexp() {}

        EmptySexp(BaseSymbol[] annotations)
        {
            super(annotations);
        }

        @Override
        public EmptySexp annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return EMPTY_SEXP;
            return new EmptySexp(annotations);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return makeBool(eval, right instanceof EmptySexp);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseSexp right)
            throws FusionException
        {
            return makeBool(eval, right instanceof EmptySexp);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseList right)
            throws FusionException
        {
            return makeBool(eval, right.size() == 0);
        }

        @Override
        IonSexp copyToIonValue(ValueFactory factory,
                              boolean throwOnConversionFailure)
            throws FusionException
        {
            IonSexp sexp = factory.newEmptySexp();
            sexp.setTypeAnnotations(getAnnotationsAsJavaStrings());
            return sexp;
        }

        @Override
        void write(Evaluator eval, Appendable out) throws IOException
        {
            writeAnnotations(out, myAnnotations);
            out.append("()");
        }

        @Override
        public void ionize(Evaluator eval, IonWriter out)
            throws IOException, FusionException
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());
            out.stepIn(IonType.SEXP);
            out.stepOut();
        }
    }


    static final class ImmutablePair
        extends BaseSexp<ImmutablePair>
    {
        private final Object myHead;
        private final Object myTail;

        private ImmutablePair(Object head, Object tail)
        {
            myHead = head;
            myTail = tail;
        }

        private ImmutablePair(BaseSymbol[] annotations,
                              Object head, Object tail)
        {
            super(annotations);
            myHead = head;
            myTail = tail;
        }


        @Override
        public ImmutablePair annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return new ImmutablePair(annotations, myHead, myTail);
        }

        Object head() { return myHead; }
        Object tail() { return myTail; }


        @Override
        int size(Evaluator eval)
            throws FusionException
        {
            int size = 1;
            ImmutablePair p = this;
            while (p.myTail instanceof ImmutablePair)
            {
                p = (ImmutablePair) p.myTail;
                size++;
            }
            if (p.myTail instanceof EmptySexp)
            {
                return size;
            }

            throw new ArgumentException("size", "proper sexp", 0, this);
        }

        @Override
        Object elt(Evaluator eval, int pos)
            throws FusionException
        {
            ImmutablePair p = this;
            for ( ; pos > 0; pos--)
            {
                Object tail = p.myTail;
                if (! (tail instanceof ImmutablePair))
                {
                    return voidValue(eval);
                }
                p = (ImmutablePair) tail;
            }
            return p.myHead;
        }

        @Override
        Object unsafeRef(Evaluator eval, int i)
            throws FusionException
        {
            String message =
                "No index " + i + " in sequence " + this;
            throw new IndexOutOfBoundsException(message);
        }

        /**
         * Assumes that this is a proper sexp!
         */
        @Override
        void unsafeCopy(Evaluator eval, int srcPos, Object[] dest,
                        int destPos, int length)
            throws FusionException
        {
            if (length != 0)
            {
                BaseSexp tail = (BaseSexp) myTail;
                if (srcPos == 0)
                {
                    dest[destPos] = myHead;

                    tail.unsafeCopy(eval, srcPos, dest, destPos+1, length-1);
                }
                else
                {
                    tail.unsafeCopy(eval, srcPos-1, dest, destPos, length);
                }
            }
        }

        @Override
        BaseSexp sexpAppend(Evaluator eval, BaseSexp back)
            throws FusionException
        {
            if (myTail instanceof BaseSexp)
            {
                Object tail = ((BaseSexp) myTail).sexpAppend(eval, back);
                if (tail != null)
                {
                    return pair(eval, myAnnotations, myHead, tail);
                }
            }
            return null;
        }

        private static BaseBool actualPairEqual(Evaluator     eval,
                                                EqualityTier  tier,
                                                ImmutablePair left,
                                                ImmutablePair right)
            throws FusionException
        {
            while (true)
            {
                Object lv = left.myHead;
                Object rv = right.myHead;

                BaseBool b = tier.eval(eval, lv, rv);
                if (b.isFalse()) return b;

                lv = left.myTail;
                rv = right.myTail;
                if (lv instanceof ImmutablePair)
                {
                    if (rv instanceof ImmutablePair)
                    {
                        left  = (ImmutablePair) lv;
                        right = (ImmutablePair) rv;
                    }
                    else
                    {
                        break;
                    }
                }
                else
                {
                    return tier.eval(eval, lv, rv);
                }
            }

            return falseBool(eval);
        }

        @Override
        BaseBool strictEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof ImmutablePair)
            {
                ImmutablePair rp = (ImmutablePair) right;
                return actualPairEqual(eval, STRICT_EQUAL, this, rp);
            }
            return falseBool(eval);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof ImmutablePair)
            {
                ImmutablePair rp = (ImmutablePair) right;
                return actualPairEqual(eval, TIGHT_EQUAL, this, rp);
            }
            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseSexp right)
            throws FusionException
        {
            if (right instanceof ImmutablePair)
            {
                ImmutablePair rp = (ImmutablePair) right;
                return actualPairEqual(eval, LOOSE_EQUAL, this, rp);
            }
            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseList right)
            throws FusionException
        {
            final int rSize = right.size();

            ImmutablePair lp = this;
            for (int i = 0; i < rSize; i++)
            {
                BaseBool result =
                    looseEquals(eval, lp.myHead, right.unsafeRef(eval, i));

                if (result.isFalse()) return result;

                Object tail = lp.myTail;
                if (tail instanceof ImmutablePair)
                {
                    lp = (ImmutablePair) tail;
                }
                else if (tail instanceof EmptySexp)
                {
                    return makeBool(eval, i+1 == rSize);
                }
                else break;
            }

            return falseBool(eval);
        }

        /**
         * Converts this pair to a normal pair of syntax objects.
         */
        private BaseSexp toPairOfSyntaxMaybe(Evaluator eval,
                                             SyntaxSymbol   context,
                                             SourceLocation loc)
            throws FusionException
        {
            SyntaxValue head =
                Syntax.datumToSyntaxMaybe(eval, myHead, context, loc);
            if (head == null) return null;

            Object tail = myTail;
            if (isPair(eval, tail))
            {
                tail = ((ImmutablePair)tail).toPairOfSyntaxMaybe(eval,
                                                                 context,
                                                                 loc);
            }
            else if (! isEmptySexp(eval, tail))
            {
                tail = Syntax.datumToSyntaxMaybe(eval, tail, context, loc);
            }
            if (tail == null) return null;

            return pair(eval, myAnnotations, head, tail);
        }

        /**
         * TODO This needs to do cycle detection.
         *   https://github.com/ion-fusion/fusion-java/issues/65
         *
         * @return null if an element can't be converted into syntax.
         */
        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator      eval,
                                       SyntaxSymbol   context,
                                       SourceLocation loc)
            throws FusionException
        {
            BaseSexp newPair = toPairOfSyntaxMaybe(eval, context, loc);
            if (newPair == null) return null;

            SyntaxValue stx = SyntaxSexp.make(eval, loc, newPair);

            // TODO This should retain context, but not push it
            //      down to the current children (which already have it).
            //      https://github.com/ion-fusion/fusion-java/issues/68
            //return Syntax.applyContext(eval, context, stx);

            return stx;
        }

        @Override
        IonSexp copyToIonValue(ValueFactory factory,
                              boolean throwOnConversionFailure)
            throws FusionException
        {
            IonSexp is = factory.newEmptySexp();
            is.setTypeAnnotations(getAnnotationsAsJavaStrings());

            ImmutablePair pair = this;
            while (true)
            {
                IonValue ion = copyToIonValue(pair.myHead, factory,
                                              throwOnConversionFailure);
                if (ion == null) return null;

                is.add(ion);

                Object tail = pair.myTail;
                if (tail instanceof ImmutablePair)
                {
                    pair = (ImmutablePair) tail;
                }
                else if (tail instanceof EmptySexp)
                {
                    return is;
                }
                else if (throwOnConversionFailure)
                {
                    throw new IonizeFailure(this);
                }
                else
                {
                    return null; // failure
                }
            }
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            writeAnnotations(out, myAnnotations);
            out.append('(');

            ImmutablePair pair = this;
            while (true)
            {
                if (pair != this) out.append(' ');

                dispatchWrite(eval, out, pair.myHead);

                Object tail = pair.myTail;
                if (tail instanceof ImmutablePair)
                {
                    pair = (ImmutablePair) tail;
                }
                else if (tail instanceof EmptySexp)
                {
                    break;
                }
                else
                {
                    out.append(" {.} ");
                    dispatchWrite(eval, out, tail);
                    break;
                }
            }

            out.append(')');
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, FusionException
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());
            out.stepIn(IonType.SEXP);

            ImmutablePair pair = this;
            while (true)
            {
                dispatchIonize(eval, out, pair.myHead);

                Object tail = pair.myTail;
                if (tail instanceof ImmutablePair)
                {
                    pair = (ImmutablePair) tail;
                }
                else if (tail instanceof EmptySexp)
                {
                    break;
                }
                else
                {
                    throw new IonizeFailure(this);
                }
            }
            out.stepOut();
        }
    }


    //========================================================================


    static final class SexpProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            return immutableSexp(eval, args);
        }
    }


    static final class IsSexpProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean result = isSexp(eval, arg);
            return makeBool(eval, result);
        }
    }


    static final class PairProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object head, Object tail)
            throws FusionException
        {
            return pair(eval, head, tail);
        }
    }


    static final class IsPairProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object value)
            throws FusionException
        {
            boolean result = isPair(eval, value);
            return makeBool(eval, result);
        }
    }


    static final class UnsafePairHeadProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object pair)
            throws FusionException
        {
            return unsafePairHead(eval, pair);
        }
    }


    static final class UnsafePairTailProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object pair)
            throws FusionException
        {
            return unsafePairTail(eval, pair);
        }
    }


    private static final class SexpIterator
        extends AbstractIterator
    {
        private BaseSexp mySexp;

        public SexpIterator(BaseSexp sexp)
        {
            mySexp = sexp;
        }

        @Override
        boolean hasNext(Evaluator eval)
            throws FusionException
        {
            return (mySexp instanceof ImmutablePair);
        }

        @Override
        Object next(Evaluator eval)
            throws FusionException
        {
            try
            {
                ImmutablePair pair = (ImmutablePair) mySexp;
                mySexp = (BaseSexp) pair.myTail;
                return pair.myHead;
            }
            catch (ClassCastException e)
            {
                throw new ArgumentException("iterator_next", "proper sexp",
                                            0, this);
            }
        }
    }


    static final class UnsafeSexpIteratorProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object list)
            throws FusionException
        {
            return unsafeSexpIterator(eval, list);
        }
    }
}
