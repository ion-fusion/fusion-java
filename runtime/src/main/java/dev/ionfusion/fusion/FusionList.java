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
import static dev.ionfusion.fusion.FusionNumber.makeInt;
import static dev.ionfusion.fusion.FusionNumber.unsafeTruncateIntToJavaInt;
import static dev.ionfusion.fusion.FusionSymbol.BaseSymbol.internSymbols;
import static dev.ionfusion.fusion.FusionUtils.EMPTY_OBJECT_ARRAY;
import static dev.ionfusion.fusion.FusionVoid.voidValue;

import com.amazon.ion.IonList;
import com.amazon.ion.IonSequence;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionCompare.EqualityTier;
import dev.ionfusion.fusion.FusionSequence.BaseSequence;
import dev.ionfusion.fusion.FusionSexp.BaseSexp;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


final class FusionList
{
    private FusionList() {}

    static final ImmutableList EMPTY_IMMUTABLE_LIST =
        new ImmutableList(FusionUtils.EMPTY_OBJECT_ARRAY);

    static final NullList NULL_LIST = new NullList();

    //========================================================================
    // Constructors


    /**
     * Turns null.list into a {@link NullList} with the same annotations.
     */
    static BaseList listFromIonSequence(Evaluator eval, IonSequence seq)
    {
        BaseSymbol[] annotations = internSymbols(seq.getTypeAnnotations());

        if (seq.isNullValue())
        {
            return nullList(eval, annotations);
        }

        if (seq.isEmpty())
        {
            return immutableList(eval, annotations, EMPTY_OBJECT_ARRAY);
        }
        else
        {
            return new LazyInjectingList(annotations,
                                         seq.toArray(EMPTY_OBJECT_ARRAY));
        }
    }


    static NullList nullList(Evaluator eval)
    {
        return NULL_LIST;
    }


    static NullList nullList(Evaluator eval, BaseSymbol[] annotations)
    {
        if (annotations.length == 0) return NULL_LIST;
        return new NullList(annotations);
    }

    static NullList nullList(Evaluator eval, String[] annotations)
    {
        if (annotations.length == 0) return NULL_LIST;
        return new NullList(internSymbols(annotations));
    }


    /**
     * @param elements must not be null. This method assumes ownership!
     */
    static Object mutableList(Evaluator eval, Object[] elements)
    {
        return new MutableList(elements);
    }


    /**
     * Creates a mutable list containing the elements.
     * @param elements must be injected.
     */
    static MutableList mutableList(Evaluator eval, List<?> elements)
    {
        return new MutableList(elements.toArray());
    }


    /**
     * @param elements must not be null. This method assumes ownership!
     */
    static ImmutableList immutableList(Evaluator eval, Object[] elements)
    {
        if (elements.length == 0)
        {
            return EMPTY_IMMUTABLE_LIST;
        }
        else
        {
            return new ImmutableList(elements);
        }
    }


    /**
     * @param elements must not be null. This method assumes ownership!
     */
    static ImmutableList immutableList(Evaluator eval,
                                       BaseSymbol[] annotations,
                                       Object[] elements)
    {
        if (elements.length == 0 && annotations.length == 0)
        {
            return EMPTY_IMMUTABLE_LIST;
        }
        else
        {
            return new ImmutableList(annotations, elements);
        }
    }

    /**
     * @param elements must not be null. This method assumes ownership!
     */
    static ImmutableList immutableList(Evaluator eval,
                                       String[] annotations,
                                       Object[] elements)
    {
        return immutableList(eval, internSymbols(annotations), elements);
    }



    /**
     * @param elements must not be null
     */
    static ImmutableList immutableList(Evaluator eval, List<?> elements)
    {
        if (elements.isEmpty())
        {
            return EMPTY_IMMUTABLE_LIST;
        }
        else
        {
            return new ImmutableList(elements.toArray());
        }
    }


    /**
     * @param elements must not be null. This method assumes ownership!
     */
    static ImmutableList immutableList(Evaluator eval,
                                       BaseSymbol[] annotations,
                                       List<?> elements)
    {
        if (elements.isEmpty() && annotations.length == 0)
        {
            return EMPTY_IMMUTABLE_LIST;
        }
        else
        {
            return new ImmutableList(annotations, elements.toArray());
        }
    }

    /**
     * @param elements must not be null. This method assumes ownership!
     */
    static ImmutableList immutableList(Evaluator eval,
                                       String[] annotations,
                                       List<?> elements)
    {
        return immutableList(eval, internSymbols(annotations), elements);
    }


    /**
     * @param elements must not be null. This method assumes ownership!
     */
    static Object stretchyList(Evaluator eval, Object[] elements)
    {
        return new StretchyList(elements);
    }


    //========================================================================
    // Predicates


    static boolean isList(Evaluator eval, Object v)
    {
        return (v instanceof BaseList);
    }

    static boolean isImmutableList(Evaluator eval, Object v)
    {
        return (v instanceof ImmutableList);
    }

    static boolean isNullList(Evaluator eval, Object v)
    {
        return (v instanceof NullList);
    }

    static boolean isMutableList(Evaluator eval, Object v)
    {
        return (v instanceof MutableList);
    }

    static boolean isStretchyList(Evaluator eval, Object v)
    {
        return (v instanceof StretchyList);
    }


    //========================================================================
    // Accessors


    static int unsafeListSize(Evaluator eval, Object list)
    {
        return ((BaseList) list).size();
    }


    static Object unsafeListElement(Evaluator eval, Object list, int pos)
    {
        return ((BaseList) list).unsafeRef(eval, pos);
    }

    static void unsafeListSet(Evaluator eval, Object list,
                              int pos, Object value)
    {
        ((MutableList) list).unsafeSet(pos, value);
    }

    static Object unsafeListAdd(Evaluator eval, Object list, Object value)
    {
        return ((BaseList) list).add(eval, value);
    }

    static Object unsafeListAddM(Evaluator eval, Object list, Object value)
    {
        return ((BaseList) list).addM(eval, value);
    }


    @Deprecated
    static Iterator<?> unsafeJavaIterate(Evaluator eval, Object list)
    {
        return ((BaseList) list).javaIterate(eval);
    }

    static FusionIterator unsafeListIterator(Evaluator eval, Object list)
    {
        return FusionIterator.iterate(eval, unsafeJavaIterate(eval, list));
    }


    //========================================================================
    // Transformers


    /**
     * @param list must be a list; it is not type-checked!
     */
    static void unsafeListCopy(Evaluator eval, Object list, int srcPos,
                               Object[] dest, int destPos, int length)
    {
        ((BaseList) list).unsafeCopy(eval, srcPos, dest, destPos, length);
    }


    /**
     * Copies the elements of this list into a new array.
     *
     * @param list must be a list; it is not type-checked!
     *
     * @return null if this is null.list.
     */
    static Object[] unsafeListExtract(Evaluator eval, Object list)
    {
        return ((BaseList) list).extract(eval);
    }


    /**
     * @param list must be a list; it is not type-checked!
     */
    static BaseList unsafeListSubseq(Evaluator eval, Object list,
                                     int srcPos, int length)
    {
        BaseList lst = (BaseList) list;

        if (srcPos == 0 && length == lst.size()) return lst;

        Object[] copy;
        if (length == 0)
        {
            copy = EMPTY_OBJECT_ARRAY;
        }
        else
        {
            copy = new Object[length];
            unsafeListCopy(eval, list, srcPos, copy, 0, length);
        }

        return lst.makeSimilar(BaseSymbol.EMPTY_ARRAY, copy);
    }


    static Object unsafeListAppendM(Evaluator eval, Object list, Object[] args)
        throws FusionException
    {
        return ((BaseList) list).appendM(eval, args);
    }


    static BaseSexp unsafeListToSexp(Evaluator eval, Object list)
        throws FusionException
    {
        if (list instanceof NullList)
        {
            BaseSymbol[] annotations = ((NullList)list).myAnnotations;
            return FusionSexp.nullSexp(eval, annotations);
        }

        BaseSexp s = FusionSexp.EMPTY_SEXP;

        BaseList l = (BaseList) list;
        for (int i = l.size(); i-- != 0; )
        {
            Object head = l.elt(eval, i);
            s = FusionSexp.pair(eval, head, s);
        }

        return s;
    }

    /**
     * @param list must be a list; it is not type-checked!
     */
    static IonList unsafeCopyToIonList(Object list, ValueFactory factory)
        throws FusionException
    {
        return unsafeCopyToIonList(list, factory, true);
    }


    /**
     * @param list must be a list; it is not type-checked!
     *
     * @return null if the list and its elements cannot be ionized.
     */
    static IonList unsafeCopyToIonListMaybe(Object list,
                                            ValueFactory factory)
        throws FusionException
    {
        return unsafeCopyToIonList(list, factory, false);
    }


    /**
     * @param list must be a list; it is not type-checked!
     *
     * @return null if the list and its elements cannot be ionized.
     */
    static IonList unsafeCopyToIonList(Object list,
                                       ValueFactory factory,
                                       boolean throwOnConversionFailure)
        throws FusionException
    {
        BaseList base = (BaseList) list;
        return base.copyToIonValue(factory, throwOnConversionFailure);
    }


    //========================================================================


    /**
     * Prevents mutation from Java code and is distinguishable from mutable
     * lists.
     */
    abstract static class BaseList
        extends BaseSequence
    {
        /**
         * The elements within this list.
         *
         * <b>WARNING!</b> The {@link LazyInjectingList} subclass may mutate
         * elements of this list while still appearing immutable. Every method
         * that reads from this array MUST be overridden there and properly
         * synchronized.
         */
        Object[] myValues;

        /**
         * @param elements must not be null. This method assumes ownership!
         */
        BaseList(Object[] elements)
        {
            myValues = elements;
        }

        /**
         * @param elements must not be null. This method assumes ownership!
         */
        BaseList(BaseSymbol[] annotations, Object[] elements)
        {
            super(annotations);
            myValues = elements;
        }


        /**
         * @param annotations must not be null. This method assumes ownership
         * of the array and it must not be modified later.
         * @param elements must not be null. This method assumes ownership!
         */
        abstract BaseList makeSimilar(BaseSymbol[] annotations,
                                      Object[] elements);


        Object[] values(Evaluator eval)
        {
            return myValues;
        }

        @Override
        int size()
        {
            return myValues.length;
        }


        @Override
        Object elt(Evaluator eval, int pos)
            throws FusionException
        {
            if (pos < 0 || size() <= pos) return voidValue(eval);
            return myValues[pos];
        }

        @Override
        Object unsafeRef(Evaluator eval, int pos)
        {
            return myValues[pos];
        }


        @Override
        final BaseSexp sexpAppend(Evaluator eval, BaseSexp back)
            throws FusionException
        {
            int size = size();
            if (size != 0)
            {
                Object[] values = values(eval);
                for (int i = size - 1; i >= 0; i--)
                {
                    back = FusionSexp.pair(eval, values[i], back);
                }
            }

            return back;
        }

        abstract ImmutableList toImmutableList(Evaluator eval);

        @Override
        void unsafeCopy(Evaluator eval, int srcPos, Object[] dest, int destPos,
                        int length)
        {
            Object[] values = values(eval);
            System.arraycopy(values, srcPos, dest, destPos, length);
        }


        Object[] extract(Evaluator eval)
        {
            int size = size();
            Object[] elements = new Object[size];
            unsafeCopy(eval, 0, elements, 0, size);
            return elements;
        }


        private static BaseBool actualListEqual(Evaluator    eval,
                                                EqualityTier tier,
                                                BaseList     left,
                                                BaseList     right)
            throws FusionException
        {
            assert ! (left.isAnyNull() || right.isAnyNull());

            int size = left.size();
            if (size == right.size())
            {
                Object[] lVals = left.values(eval);
                Object[] rVals = right.values(eval);

                for (int i = 0; i < size; i++)
                {
                    Object lv = lVals[i];
                    Object rv = rVals[i];

                    BaseBool b = tier.eval(eval, lv, rv);
                    if (b.isFalse()) return b;
                }

                return trueBool(eval);
            }
            return falseBool(eval);
        }

        @Override
        BaseBool strictEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseList)
            {
                BaseList r = (BaseList) right;
                if (! r.isAnyNull())
                {
                    return actualListEqual(eval, STRICT_EQUAL, this, r);
                }
            }
            return falseBool(eval);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseList)
            {
                BaseList r = (BaseList) right;
                if (! r.isAnyNull())
                {
                    return actualListEqual(eval, TIGHT_EQUAL, this, r);
                }
            }
            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseSequence)
            {
                BaseSequence r = (BaseSequence) right;
                return r.looseEquals2(eval, this);
            }
            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseList left)
            throws FusionException
        {
            // Neither is null.list
            return actualListEqual(eval, LOOSE_EQUAL, left, this);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseSexp left)
            throws FusionException
        {
            // (= <BaseList> <BaseSexp>) is implemented the other way
            return left.looseEquals2(eval, this);
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            return SyntaxList.makeOriginal(eval, loc, this);
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
            SyntaxList stx;

            int size = size();
            if (size == 0 && (this instanceof ImmutableList))
            {
                stx = SyntaxList.make(eval, null, this);
            }
            else
            {
                Object[] children = new Object[size];
                for (int i = 0; i < size; i++)
                {
                    Object rawChild = unsafeRef(eval, i);
                    Object child =
                        Syntax.datumToSyntaxMaybe(eval, rawChild, context, loc);
                    if (child == null)
                    {
                        // Hit something that's not syntax-able
                        return null;
                    }
                    children[i] = child;
                }

                BaseSymbol[] anns = getAnnotations();
                Object list = immutableList(eval, anns, children);
                stx = SyntaxList.make(eval, loc, list);
            }

            // TODO This should retain context, but not push it
            //      down to the current children (which already have it).
            //      https://github.com/ion-fusion/fusion-java/issues/68
            //return Syntax.applyContext(eval, context, stx);

            return stx;
        }


        /**
         * @return null if the list and its elements cannot be ionized
         *  UNLESS throwOnConversionFailure
         */
        @Override
        IonList copyToIonValue(ValueFactory factory,
                               boolean throwOnConversionFailure)
            throws FusionException
        {
            int len = size();
            IonValue[] ions = new IonValue[len];
            for (int i = 0; i < len; i++)
            {
                IonValue ion = copyToIonValue(myValues[i], factory,
                                              throwOnConversionFailure);
                if (ion == null)
                {
                    assert !throwOnConversionFailure;
                    return null;
                }

                ions[i] = ion;
            }

            IonList list = factory.newList(ions);
            list.setTypeAnnotations(getAnnotationsAsJavaStrings());
            return list;
        }

        BaseList add(Evaluator eval, Object value)
        {
            int len = size();
            Object[] copy = Arrays.copyOf(values(eval), len + 1);
            copy[len] = value;
            return makeSimilar(myAnnotations, copy);
        }

        BaseList addM(Evaluator eval, Object value)
        {
            return add(eval, value);
        }


        @Override
        BaseList append(Evaluator eval, Object[] args)
            throws FusionException
        {
            int myLen = size();
            int newLen = myLen;
            for (Object arg : args)
            {
                newLen += ((BaseSequence) arg).size();
            }

            if (newLen == myLen) return this; // Nothing to append

            Object[] copy = Arrays.copyOf(values(eval), newLen);

            int pos = myLen;
            for (Object arg : args)
            {
                BaseSequence v = (BaseSequence) arg;
                int argLen = v.size();

                v.unsafeCopy(eval, 0, copy, pos, argLen);
                pos += argLen;
            }
            assert pos == newLen;

            return makeSimilar(myAnnotations, copy);
        }


        Iterator<?> javaIterate(Evaluator eval)
        {
            Object[] values = values(eval);
            return Arrays.asList(values).iterator();
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            writeAnnotations(out, myAnnotations);

            out.append('[');

            int length = size();
            for (int i = 0; i < length; i++)
            {
                if (i != 0) out.append(", ");
                dispatchWrite(eval, out, myValues[i]);
            }

            out.append(']');
        }


        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, FusionException
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());
            out.stepIn(IonType.LIST);
            for (int i = 0; i < size(); i++)
            {
                dispatchIonize(eval, out, myValues[i]);
            }
            out.stepOut();
        }
    }


    private static class MutableList
        extends BaseList
    {
        /**
         * @param elements must not be null. This method assumes ownership!
         */
        MutableList(Object[] elements)
        {
            super(elements);
        }

        /**
         * @param elements must not be null. This method assumes ownership!
         */
        MutableList(BaseSymbol[] annotations, Object[] elements)
        {
            super(annotations, elements);
        }

        /**
         * @param elements must not be null. This method assumes ownership!
         */
        @Override
        BaseList makeSimilar(BaseSymbol[] annotations, Object[] elements)
        {
            return new MutableList(annotations, elements);
        }

        @Override
        ImmutableList toImmutableList(Evaluator eval)
        {
            return new ImmutableList(getAnnotations(), extract(eval));
        }

        /**
         * Assumes ownership of arguments.
         */
        @Override
        public Object annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            // Since this instance is mutable, we cannot share the array.
            Object[] values = Arrays.copyOf(myValues, size());
            return makeSimilar(annotations, values);
        }

        void unsafeSet(int pos, Object value)
        {
            myValues[pos] = value;
        }
    }


    private static class ImmutableList
        extends BaseList
    {
        /**
         * @param elements must not be null. This method assumes ownership!
         */
        ImmutableList(Object[] elements)
        {
            super(elements);
        }

        /**
         * @param elements must not be null. This method assumes ownership!
         */
        ImmutableList(BaseSymbol[] annotations, Object[] elements)
        {
            super(annotations, elements);
        }

        /**
         * @param elements must not be null. This method assumes ownership!
         */
        @Override
        BaseList makeSimilar(BaseSymbol[] annotations, Object[] elements)
        {
            return new ImmutableList(annotations, elements);
        }

        @Override
        ImmutableList toImmutableList(Evaluator eval)
        {
            return this;
        }

        @Override
        public Object annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            // Since this instance is immutable, we can share the array.
            return makeSimilar(annotations, myValues);
        }
    }


    private static final class NullList
        extends ImmutableList
    {
        NullList()
        {
            super(EMPTY_OBJECT_ARRAY);
        }

        NullList(BaseSymbol[] annotations)
        {
            super(annotations, EMPTY_OBJECT_ARRAY);
        }

        @Override
        boolean isAnyNull()
        {
            return true;
        }

        @Override
        int size()
        {
            return 0;
        }

        @Override
        public Object annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return new NullList(annotations);
        }

        @Override
        BaseList append(Evaluator eval, Object[] args)
            throws FusionException
        {
            BaseList empty =
                (myAnnotations.length == 0
                    ? EMPTY_IMMUTABLE_LIST
                    : new ImmutableList(myAnnotations, EMPTY_OBJECT_ARRAY));

            return empty.append(eval, args);
        }

        @Override
        Object[] extract(Evaluator eval)
        {
            return null;
        }

        @Override
        BaseBool strictEquals(Evaluator eval, Object right)
            throws FusionException
        {
            boolean b = (right instanceof NullList);
            return makeBool(eval, b);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            boolean b = (right instanceof NullList);
            return makeBool(eval, b);
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
            return makeBool(eval, left.isAnyNull());
        }

        @Override
        IonList copyToIonValue(ValueFactory factory,
                               boolean throwOnConversionFailure)
            throws FusionException
        {
            IonList list = factory.newNullList();
            list.setTypeAnnotations(getAnnotationsAsJavaStrings());
            return list;
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException
        {
            writeAnnotations(out, myAnnotations);
            out.append("null.list");
        }


        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, FusionException
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());
            out.writeNull(IonType.LIST);
        }
    }


    private static class StretchyList
        extends MutableList
    {
        private int mySize;

        /**
         * @param elements must not be null. This method assumes ownership!
         */
        StretchyList(Object[] elements)
        {
            super(elements);
            mySize = elements.length;
        }

        /**
         * @param elements must not be null. This method assumes ownership!
         */
        StretchyList(BaseSymbol[] annotations, Object[] elements)
        {
            super(annotations, elements);
            mySize = elements.length;
        }

        /**
         * @param elements must not be null. This method assumes ownership!
         */
        @Override
        BaseList makeSimilar(BaseSymbol[] annotations, Object[] elements)
        {
            return new StretchyList(annotations, elements);
        }

        @Override
        int size()
        {
            return mySize;
        }

        @Override
        BaseList addM(Evaluator eval, Object value)
        {
            if (mySize == myValues.length)
            {
                // This expansion math is what's used by ArrayList, so I'm
                // assuming that it works well in practice.
                int newLength = (mySize * 3) / 2 + 1;
                myValues = Arrays.copyOf(myValues, newLength);
            }

            myValues[mySize++] = value;
            return this;
        }

        @Override
        BaseList appendM(Evaluator eval, Object[] args)
            throws FusionException
        {
            int newLen = mySize;
            for (Object arg : args)
            {
                newLen += ((BaseSequence) arg).size();
            }

            // Note that mySize <= myValues.length
            if (mySize < newLen)
            {
                Object[] newValues =
                    (myValues.length < newLen
                        ? Arrays.copyOf(myValues, newLen)
                        : myValues);

                int pos = mySize;
                for (Object arg : args)
                {
                    BaseSequence v = (BaseSequence) arg;
                    int argLen = v.size();

                    v.unsafeCopy(eval, 0, newValues, pos, argLen);
                    pos += argLen;
                }
                assert pos == newLen;

                // Be careful not to mutate this until the copies succeed.
                myValues = newValues;
                mySize   = pos;
            }

            return this;
        }

        @Override
        Iterator<?> javaIterate(Evaluator eval)
        {
            Iterator<Object> iterator = new Iterator<Object>()
            {
                private int i = 0;

                @Override
                public boolean hasNext()
                {
                    return (i < mySize);
                }

                @Override
                public Object next()
                {
                    if (i < mySize) return myValues[i++];
                    throw new NoSuchElementException();
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();
                }
            };
            return iterator;
        }
    }

    private static final class LazyInjectingList
        extends ImmutableList
    {
        /**
         * @param elements must not be null. This method assumes ownership!
         */
        LazyInjectingList(BaseSymbol[] annotations, Object[] elements)
        {
            super(annotations, elements);
            assert elements.length != 0;
        }

        /**
         * Synchronized so this immutable class is thread-safe for reads.
         */
        private synchronized void injectElements(Evaluator eval)
        {
            if (myValues[0] instanceof IonValue)
            {
                for (int i = 0; i < myValues.length; i++)
                {
                    myValues[i] = eval.inject((IonValue) myValues[i]);
                }
            }
        }

        @Override
        Object[] values(Evaluator eval)
        {
            injectElements(eval);
            return myValues;
        }

        @Override
        public Object annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            // Since this instance is immutable, we can share the array AFTER
            // we inject the children.
            injectElements(eval);
            return super.annotate(eval, annotations);
        }

        @Override
        Object elt(Evaluator eval, int pos)
            throws FusionException
        {
            injectElements(eval);
            return super.elt(eval, pos);
        }

        @Override
        Object unsafeRef(Evaluator eval, int pos)
        {
            injectElements(eval);
            return myValues[pos];
        }

        @Override
        IonList copyToIonValue(ValueFactory factory,
                               boolean throwOnConversionFailure)
            throws FusionException
        {
            synchronized (this)
            {
                if (myValues[0] instanceof IonValue)
                {
                    int len = size();
                    IonValue[] ions = new IonValue[len];

                    for (int i = 0; i < len; i++)
                    {
                        ions[i] = factory.clone((IonValue) myValues[i]);
                    }

                    IonList list = factory.newList(ions);
                    list.setTypeAnnotations(getAnnotationsAsJavaStrings());
                    return list;
                }
            }
            // else our elements have already been injected, copy as normal.

            return super.copyToIonValue(factory, throwOnConversionFailure);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            synchronized (this)
            {
                if (myValues[0] instanceof IonValue)
                {
                    IonWriter iw = WRITER_BUILDER.build(out);
                    ionize(eval, iw);
                    iw.finish();
                    return;
                }
            }
            // else our elements have already been injected, ionize as normal.
            super.write(eval, out);
        }

        /**
         * This implementation avoids injecting children while ionizing.
         * On the assumption that most ionization occurs just before data is
         * released (that is, at the end of data processing), this avoids
         * generating a useless deep transformation of the data.
         */
        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, FusionException
        {
            synchronized (this)
            {
                if (myValues[0] instanceof IonValue)
                {
                    out.setTypeAnnotations(getAnnotationsAsJavaStrings());
                    out.stepIn(IonType.LIST);
                    {
                        int len = myValues.length;
                        for (Object elt : myValues)
                        {
                            ((IonValue) elt).writeTo(out);
                        }
                    }
                    out.stepOut();
                    return;
                }
            }
            // else our elements have already been injected, ionize as normal.

            super.ionize(eval, out);
        }
    }


    //========================================================================
    // Procedure Helpers


    /**
     * @param expectation must not be null.
     * @return the Fusion list, not null.
     */
    static Object checkListArg(Evaluator eval,
                               Procedure who,
                               String    expectation,
                               int       argNum,
                               Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseList)
        {
            return arg;
        }

        throw who.argFailure(expectation, argNum, args);
    }


    /**
     * @return the Fusion list, not null (but maybe {@code null.list}).
     */
    static Object checkNullableListArg(Evaluator eval,
                                       Procedure who,
                                       int       argNum,
                                       Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable list";
        return checkListArg(eval, who, expectation, argNum, args);
    }


    /**
     * @return not null or {@code null.list}.
     */
    static Object checkActualListArg(Evaluator eval,
                                     Procedure who,
                                     int       argNum,
                                     Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-null list";
        Object result = checkListArg(eval, who, expectation, argNum, args);
        if (isNullList(eval, result))
        {
            throw who.argFailure(expectation, argNum, args);
        }
        return result;
    }


    //========================================================================
    // Procedures


    static final class IsListProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object value)
            throws FusionException
        {
            boolean result = isList(eval, value);
            return makeBool(eval, result);
        }
    }


    static final class IsImmutableListProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object value)
            throws FusionException
        {
            boolean result = isImmutableList(eval, value);
            return makeBool(eval, result);
        }
    }


    static final class IsMutableListProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object value)
            throws FusionException
        {
            boolean result = isMutableList(eval, value);
            return makeBool(eval, result);
        }
    }


    static final class IsStretchyListProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object value)
            throws FusionException
        {
            boolean result = isStretchyList(eval, value);
            return makeBool(eval, result);
        }
    }


    static final class ImmutableListProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            return immutableList(eval, args);
        }
    }


    static final class MutableListProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            return mutableList(eval, args);
        }
    }


    static final class StretchyListProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            return stretchyList(eval, args);
        }
    }


    static final class UnsafeListToImmutableListProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object list)
            throws FusionException
        {
            return ((BaseList) list).toImmutableList(eval);
        }
    }


    static final class UnsafeListSizeProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object list)
            throws FusionException
        {
            int result = unsafeListSize(eval, list);
            return makeInt(eval, result);
        }
    }


    static final class UnsafeListElementProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object list, Object p)
            throws FusionException
        {
            int pos = unsafeTruncateIntToJavaInt(eval, p);

            return unsafeListElement(eval, list, pos);
        }
    }


    static final class UnsafeListSubseqProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            int from = unsafeTruncateIntToJavaInt(eval, args[1]);
            int to   = unsafeTruncateIntToJavaInt(eval, args[2]);
            int len  = to - from;

            return unsafeListSubseq(eval, args[0], from, len);
        }
    }


    static final class UnsafeListSetProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            int pos = unsafeTruncateIntToJavaInt(eval, args[1]);

            unsafeListSet(eval, args[0], pos, args[2]);

            return voidValue(eval);
        }
    }


    static final class UnsafeListAddProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object list, Object value)
            throws FusionException
        {
            return unsafeListAdd(eval, list, value);
        }
    }


    static final class UnsafeListAddMProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object list, Object value)
            throws FusionException
        {
            return unsafeListAddM(eval, list, value);
        }
    }


    static final class UnsafeListIterateProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object list)
            throws FusionException
        {
            return unsafeListIterator(eval, list);
        }
    }
}
