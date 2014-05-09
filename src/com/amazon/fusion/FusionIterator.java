// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.boolToJavaBoolean;
import static com.amazon.fusion.FusionBool.makeBool;
import static com.amazon.fusion.FusionList.isList;
import static com.amazon.fusion.FusionList.unsafeListIterator;
import static com.amazon.fusion.FusionSexp.isSexp;
import static com.amazon.fusion.FusionSexp.unsafeSexpIterator;
import static com.amazon.fusion.FusionUtils.EMPTY_OBJECT_ARRAY;
import com.amazon.ion.IonValue;
import java.io.IOException;
import java.util.Iterator;

// TODO Add abstract class so subclasses don't have blank procs?

class FusionIterator
    extends BaseValue
{
    static FusionIterator checkArg(Procedure who, int argNum, Object... args)
        throws ArgumentException
    {
        return who.checkArg(FusionIterator.class, "iterator", argNum, args);
    }


    /**
     * @param value may be an iterator, list, or sexp.
     */
    static FusionIterator iterate(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof FusionIterator)
        {
            return (FusionIterator) value;
        }

        if (isList(eval, value))
        {
            return unsafeListIterator(eval, value);
        }

        if (isSexp(eval, value))
        {
            return unsafeSexpIterator(eval, value);
        }

        throw new ArgumentException("iterate", "iterable", 0, value);
    }


    /**
     * Builds a Fusion iterator from a Java iterator, without injecting its
     * elements.
     *
     * @see #injectIterator(Evaluator, Iterator)
     * @see #injectIonIterator(Evaluator, Iterator)
     */
    static FusionIterator iterate(Evaluator eval, Iterator<?> iterator)
    {
        return new IteratorAdaptor(iterator);
    }



    /**
     * Builds a Fusion iterator from a Java iterator, lazily injecting each
     * value.
     *
     * @see #iterate(Evaluator, Iterator)
     * @see #injectIonIterator(Evaluator, Iterator)
     */
    static Object injectIterator(Evaluator eval, Iterator<?> iterator)
    {
        return new InjectingIteratorAdaptor(iterator);
    }


    /**
     * Builds a Fusion iterator from an IonValue iterator, lazily injecting
     * each {@code IonValue}.  This is slightly more efficient than
     * {@link FusionIterator#injectIterator(Evaluator, Iterator)}.
     */
    static Object injectIonIterator(Evaluator eval,
                                    Iterator<IonValue> iterator)
    {
        return new IonIteratorAdaptor(iterator);
    }


    static boolean allHaveNext(Evaluator eval, FusionIterator... streams)
        throws FusionException
    {
        for (FusionIterator s : streams)
        {
            if (! s.hasNext(eval)) return false;
        }
        return true;
    }


    //========================================================================


    private final Procedure myHasNextProc;
    private final Procedure myNextProc;

    FusionIterator(Procedure hasNextProc, Procedure nextProc)
    {
        myHasNextProc = hasNextProc;
        myNextProc    = nextProc;
    }

    /** Public so it can be called from java_new */
    public FusionIterator(Object hasNextProc, Object nextProc)
    {
        myHasNextProc = (Procedure) hasNextProc;
        myNextProc    = (Procedure) nextProc;
    }


    /**
     * For calling from Procedure implementations
     * @param eval
     * @return Fusion true or false (not null.boolean)
     */
    Object doHasNextTail(Evaluator eval)
        throws FusionException
    {
        Object o = eval.callNonTail(myHasNextProc);
        if (boolToJavaBoolean(eval, o) == null)
        {
            throw new ResultFailure("iterator has_next_proc",
                                    "true or false", o);
        }
        return o;
    }

    boolean hasNext(Evaluator eval) throws FusionException
    {
        Object  o = eval.callNonTail(myHasNextProc, EMPTY_OBJECT_ARRAY);
        Boolean b = boolToJavaBoolean(eval, o);
        if (b == null)
        {
            throw new ResultFailure("iterator has_next_proc", "true or false",
                                    o);
        }
        return b;
    }

    Object doNextTail(Evaluator eval)
        throws FusionException
    {
        return eval.bounceTailCall(myNextProc);
    }

    Object next(Evaluator eval)
        throws FusionException
    {
        return eval.callNonTail(myNextProc);
    }


    @Override
    void write(Evaluator eval, Appendable out) throws IOException
    {
        out.append("{{{ iterator }}}");
    }


    //========================================================================


    /**
     * Base class for Fusion iterators implemented in Java.
     * Subclasses just need to implement hasNext and next, and next must
     * return Fusion values.
     */
    abstract static class AbstractIterator
        extends FusionIterator
    {
        AbstractIterator()
        {
            super(null, null);
        }

        @Override
        Object doHasNextTail(Evaluator eval)
            throws FusionException
        {
            return makeBool(eval, hasNext(eval));
        }

        @Override
        Object doNextTail(Evaluator eval)
            throws FusionException
        {
            return next(eval);
        }
    }


    private static class IteratorAdaptor
        extends AbstractIterator
    {
        final Iterator<?> myIterator;

        /** Iterated values must not need injecting. */
        IteratorAdaptor(Iterator<?> iter)
        {
            myIterator = iter;
        }

        @Override
        public boolean hasNext(Evaluator eval)
        {
            return myIterator.hasNext();
        }

        @Override
        public Object next(Evaluator eval)
            throws FusionException
        {
            return myIterator.next();
        }
    }


    /** Custom class avoid some dynamic dispatch. */
    private static final class InjectingIteratorAdaptor
        extends IteratorAdaptor
    {
        InjectingIteratorAdaptor(Iterator<?> iter)
        {
            super(iter);
        }

        @Override
        public Object next(Evaluator eval)
            throws FusionException
        {
            Object orig = myIterator.next();
            Object injected = eval.injectMaybe(orig);
            if (injected == null)
            {
                throw new ResultFailure("iterator_next",
                                        "injectable Java type",
                                        0, orig);
            }
            return injected;
        }
    }


    /** Custom class avoid some dynamic dispatch. */
    private static final class IonIteratorAdaptor
        extends AbstractIterator
    {
        private final Iterator<IonValue> myIonIterator;

        IonIteratorAdaptor(Iterator<IonValue> iter)
        {
            myIonIterator = iter;
        }

        @Override
        public boolean hasNext(Evaluator eval)
        {
            return myIonIterator.hasNext();
        }

        @Override
        public Object next(Evaluator eval)
        {
            // Don't assume that IonValue is a Fusion value.
            // It may need converting to another form.
            return eval.inject(myIonIterator.next());
        }
    }


    //========================================================================


    static final class IsIteratorProc
        extends Procedure1
    {
        IsIteratorProc()
        {
            //    "                                                                               |
            super("Checks if the `value` is an iterator.  Returns `true` or `false`.",
                  "value");
        }

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean b = (arg instanceof FusionIterator);
            return makeBool(eval, b);
        }
    }


    static final class HasNextProc
        extends Procedure1
    {
        HasNextProc()
        {
            //    "                                                                               |
            super("Checks if the `iterator` will provide another value when `iterator_next` is\n" +
                  "called.  Returns `true` or `false`.\n" +
                  "\n" +
                  "After this procedure returns `false` for some iterator instance, all\n" +
                  "subsequent calls of `iterator_has_next` on the same instance are expected to\n" +
                  "return `false`, and all subsequent calls of `iterator_next` are expected to\n" +
                  "throw an exception.",
                  "iterator");
        }

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            FusionIterator iter = FusionIterator.checkArg(this, 0, arg);
            return iter.doHasNextTail(eval);
        }
    }


    static final class NextProc
        extends Procedure1
    {
        NextProc()
        {
            //    "                                                                               |
            super("Returns the next element of the `iterator`.  When the iterator has no more\n" +
                  "elements, an exception is thrown.",
                  "iterator");
        }

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            FusionIterator iter = FusionIterator.checkArg(this, 0, arg);
            return iter.doNextTail(eval);
        }
    }
}
