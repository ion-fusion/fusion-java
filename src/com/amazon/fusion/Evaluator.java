// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.makeBool;
import static com.amazon.fusion.FusionList.listFromIonSequence;
import static com.amazon.fusion.FusionNull.makeNullNull;
import static com.amazon.fusion.FusionNumber.makeDecimal;
import static com.amazon.fusion.FusionNumber.makeInt;
import static com.amazon.fusion.FusionSexp.sexpFromIonSequence;
import static com.amazon.fusion.FusionString.makeString;
import static com.amazon.fusion.FusionStruct.structFromIonStruct;
import static com.amazon.fusion.FusionSymbol.makeSymbol;
import static com.amazon.fusion.FusionTimestamp.makeTimestamp;
import static com.amazon.fusion.FusionVoid.voidValue;
import com.amazon.ion.IonBool;
import com.amazon.ion.IonDecimal;
import com.amazon.ion.IonInt;
import com.amazon.ion.IonList;
import com.amazon.ion.IonSexp;
import com.amazon.ion.IonString;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSymbol;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonTimestamp;
import com.amazon.ion.IonValue;
import com.amazon.ion.Timestamp;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Main entry point to the Fusion evaluation engine.
 */
final class Evaluator
{
    private final GlobalState myGlobalState;
    private final IonSystem mySystem;
    private final Evaluator myOuterFrame;
    private final Map<Object, Object> myContinuationMarks;


    Evaluator(GlobalState globalState)
    {
        myGlobalState = globalState;
        mySystem      = globalState.myIonSystem;
        myOuterFrame = null;
        myContinuationMarks = null;
    }

    private Evaluator(Evaluator outerBindings)
    {
        myGlobalState     = outerBindings.myGlobalState;
        mySystem          = outerBindings.mySystem;
        myOuterFrame      = outerBindings;
        myContinuationMarks = new HashMap<Object, Object>();
    }

    IonSystem getSystem()
    {
        return mySystem;
    }


    GlobalState getGlobalState()
    {
        return myGlobalState;
    }


    //========================================================================


    ModuleInstance findKernel()
    {
        return myGlobalState.myKernelModule;
    }


    //========================================================================


    /**
     * Injects an Ion DOM into the equivalent Fusion runtime objects.
     * It is an error for modifications to be made to the argument instance
     * (or anything it refers to) after it is passed to this method.
     *
     * @param value may be null to inject Fusion's void value.
     */
    Object inject(IonValue value)
    {
        if (value == null)
        {
            return voidValue(this);
        }

        switch (value.getType())
        {
            case NULL:
            {
                return makeNullNull(this, value.getTypeAnnotations());
            }
            case BOOL:
            {
                Boolean b = (value.isNullValue()
                                 ? null
                                 : ((IonBool)value).booleanValue());
                return makeBool(this, value.getTypeAnnotations(), b);
            }
            case INT:
            {
                BigInteger big = ((IonInt)value).bigIntegerValue();
                return makeInt(this, value.getTypeAnnotations(), big);
            }
            case DECIMAL:
            {
                BigDecimal big = ((IonDecimal)value).decimalValue();
                return makeDecimal(this, value.getTypeAnnotations(), big);
            }
            case TIMESTAMP:
            {
                Timestamp t = ((IonTimestamp)value).timestampValue();
                return makeTimestamp(this, value.getTypeAnnotations(), t);
            }
            case SYMBOL:
            {
                String text = ((IonSymbol)value).stringValue();
                return makeSymbol(this, value.getTypeAnnotations(), text);
            }
            case STRING:
            {
                String text = ((IonString)value).stringValue();
                return makeString(this, value.getTypeAnnotations(), text);
            }
            case LIST:
            {
                IonList list = (IonList) value;
                return listFromIonSequence(this, list);
            }
            case SEXP:
            {
                IonSexp sexp = (IonSexp) value;
                return sexpFromIonSequence(this, sexp);
            }
            case STRUCT:
            {
                return structFromIonStruct(this, (IonStruct) value);
            }
        }
        return value;
    }

    Object injectMaybe(Number value)
    {
        if (   value instanceof Long
            || value instanceof Integer
            || value instanceof Short
            || value instanceof Byte)
        {
            return makeInt(this, value.longValue());
        }
        else if (value instanceof BigInteger)
        {
            return makeInt(this, (BigInteger) value);
        }
        else if (value instanceof BigDecimal)
        {
            return newDecimal((BigDecimal) value);
        }

        // TODO this API forces us to use a non-null object for VOID!
        return null;
    }

    /**
     * Transforms a Java value to a Fusion value, where possible.
     * It is an error for modifications to be made to the argument instance
     * (or anything it refers to) after it is passed to this method.
     *
     * @param javaValue may be null to inject Fusion's void value.
     *
     * @return the injected value, or null if the value cannot be injected.
     */
    Object injectMaybe(Object javaValue)
    {
        // Check for null first, on the off chance this will help the compiler
        // optimize the later instanceof tests.
        if (javaValue == null)
        {
            return voidValue(this);
        }
        else if (javaValue instanceof FusionValue)
        {
            return javaValue;
        }
        else if (javaValue instanceof IonValue)
        {
            return inject((IonValue) javaValue);
        }
        else if (javaValue instanceof String)
        {
            return newString((String) javaValue);
        }
        else if (javaValue instanceof Number)
        {
            return injectMaybe((Number) javaValue);
        }
        else if (javaValue instanceof Boolean)
        {
            return makeBool(this, (Boolean) javaValue);
        }

        // ******** Be sure to document types as they are added! ********


        // TODO FUSION-206 should handle Double, Timestamp, Object[], ArrayList

        // TODO this API forces us to use a non-null object for VOID!
        return null;
    }


    //========================================================================


    /**
     * @deprecated Use {@link FusionNull#makeNullNull(Evaluator)} or
     * {@link FusionNull#makeNullNull(Evaluator, String[])}.
     */
    @Deprecated
    Object newNull(String... annotations)
    {
        return makeNullNull(this, annotations);
    }

    /**
     * @deprecated Use {@link FusionBool#makeBool(Evaluator,boolean)}.
     */
    @Deprecated
    Object newBool(boolean value)
    {
        return makeBool(this, value);
    }

    /**
     * @deprecated Use {@link FusionBool#makeBool(Evaluator,String[],boolean)}.
     */
    @Deprecated
    Object newBool(boolean value, String... annotations)
    {
        return makeBool(this, annotations, value);
    }

    /**
     * @deprecated Use {@link FusionBool#makeBool(Evaluator,Boolean)}.
     */
    @Deprecated
    Object newBool(Boolean value)
    {
        return makeBool(this, value);
    }

    /**
     * @deprecated Use {@link FusionBool#makeBool(Evaluator,String[],Boolean)}.
     */
    @Deprecated
    Object newBool(Boolean value, String... annotations)
    {
        return makeBool(this, annotations, value);
    }

    /**
     * @deprecated Use {@link FusionNumber#makeInt(Evaluator,long)}.
     */
    @Deprecated
    Object newInt(long value)
    {
        return makeInt(this, value);
    }

    /**
     * @deprecated Use {@link FusionNumber#makeInt(Evaluator,String[],long)}.
     */
    @Deprecated
    Object newInt(long value, String... annotations)
    {
        return makeInt(this, annotations, value);
    }

    /**
     * @deprecated Use {@link FusionNumber#makeInt(Evaluator,BigInteger)}.
     */
    @Deprecated
    Object newInt(BigInteger value)
    {
        return makeInt(this, value);
    }

    /**
     * @deprecated Use
     * {@link FusionNumber#makeInt(Evaluator,String[],BigInteger)}.
     */
    @Deprecated
    Object newInt(BigInteger value, String... annotations)
    {
        return makeInt(this, annotations, value);
    }

    /**
     * @deprecated Use {@link FusionString#makeString(Evaluator, String)}.
     */
    @Deprecated
    Object newString(String value)
    {
        return makeString(this, value);
    }

    /**
     * @deprecated Use {@link FusionSymbol#makeSymbol(Evaluator, String)}.
     */
    @Deprecated
    Object newSymbol(String value)
    {
        return makeSymbol(this, value);
    }

    /**
     * @deprecated Use
     * {@link FusionSymbol#makeSymbol(Evaluator, String[], String)}.
     */
    @Deprecated
    Object newSymbol(String value, String... annotations)
    {
        return makeSymbol(this, annotations, value);
    }

    /**
     * @deprecated Use helpers in {@link FusionNumber}.
     */
    @Deprecated
    Object newDecimal(BigDecimal value)
    {
        return makeDecimal(this, value);
    }

    /**
     * @deprecated Use helpers in {@link FusionNumber}.
     */
    @Deprecated
    Object newDecimal(BigDecimal value, String... annotations)
    {
        return makeDecimal(this, annotations, value);
    }

    Object newFloat(double value)
    {
        IonValue dom = mySystem.newFloat(value);
        return inject(dom);
    }

    Object newFloat(double value, String... annotations)
    {
        IonValue dom = mySystem.newFloat(value);
        if (annotations != null) dom.setTypeAnnotations(annotations);
        return inject(dom);
    }

    Object newFloat(Double value)
    {
        IonValue dom = (value == null
                           ? mySystem.newNullFloat()
                           : mySystem.newFloat(value));
        return inject(dom);
    }

    Object newFloat(Double value, String... annotations)
    {
        IonValue dom = (value == null
                           ? mySystem.newNullFloat()
                           : mySystem.newFloat(value));
        if (annotations != null) dom.setTypeAnnotations(annotations);
        return inject(dom);
    }

    /**
     * @deprecated Use
     * {@link FusionTimestamp#makeTimestamp(Evaluator, Timestamp)}.
     */
    @Deprecated
    Object newTimestamp(Timestamp value)
    {
        return makeTimestamp(this, value);
    }

    /**
     * @deprecated Use
     * {@link FusionTimestamp#makeTimestamp(Evaluator, String[], Timestamp)}.
     */
    @Deprecated
    Object newTimestamp(Timestamp value, String... annotations)
    {
        return makeTimestamp(this, annotations, value);
    }

    Object newBlob(byte[] value, String... annotations)
    {
        IonValue dom = mySystem.newBlob(value);
        if (annotations != null) dom.setTypeAnnotations(annotations);
        return inject(dom);
    }

    Object newClob(byte[] value, String... annotations)
    {
        IonValue dom = mySystem.newClob(value);
        if (annotations != null) dom.setTypeAnnotations(annotations);
        return inject(dom);
    }


    //========================================================================


    /**
     * Casts or copies as necessary.
     * Returns null if value contains non-Ionizable data!
     *
     * @deprecated This is of questionable design, and should probably be
     * removed. Please limit use carefully.
     */
    @Deprecated
    IonValue convertToIonValueMaybe(Object value)
        throws FusionException
    {
        if (value instanceof IonValue)
        {
            return (IonValue) value;
        }

        if (value instanceof FusionValue)
        {
            FusionValue fv = (FusionValue) value;
            return fv.copyToIonValue(mySystem, false);
        }

        return null;
    }


    //========================================================================

    // This is a shady implementation of Racket's continuation marks.
    // It's not full featured: we don't create every continuation frame, so we
    // can't implement the primitive with-continuation-mark.

    Object firstContinuationMark(Object key)
    {
        // The keys must be hashable and equals-able!
        assert key instanceof DynamicParameter;

        Evaluator e = this;
        while (e.myOuterFrame != null)
        {
            Object value = e.myContinuationMarks.get(key);
            if (value != null) return value;
            e = e.myOuterFrame;
        }
        return null;
    }


    /**
     * Collects all marks for {@code key} in the current continuation,
     * with the most recent mark first.
     *
     * @return a non-null list.
     */
    ArrayList<Object> continuationMarks(Object key)
    {
        // The keys must be hashable and equals-able!
        assert key instanceof DynamicParameter;

        ArrayList<Object> results = new ArrayList<>();

        Evaluator e = this;
        while (e.myOuterFrame != null)
        {
            Object value = e.myContinuationMarks.get(key);
            if (value != null)
            {
                results.add(value);
            }
            e = e.myOuterFrame;
        }

        return results;
    }


    Evaluator markedContinuation(Object key, Object mark)
    {
        // The keys must be hashable and equals-able!
        assert key instanceof DynamicParameter;

        Evaluator innerFrame = new Evaluator(this);
        innerFrame.myContinuationMarks.put(key, mark);
        return innerFrame;
    }

    Evaluator markedContinuation(Object[] keys, Object[] marks)
    {
        assert keys.length == marks.length;

        Evaluator innerFrame = new Evaluator(this);
        for (int i = 0; i < keys.length; i++)
        {
            // The keys must be hashable and equals-able!
            assert keys[i] instanceof DynamicParameter;

            innerFrame.myContinuationMarks.put(keys[i], marks[i]);
        }
        return innerFrame;
    }


    //========================================================================

    Namespace findCurrentNamespace()
    {
        DynamicParameter param = getGlobalState().myCurrentNamespaceParam;
        return (Namespace) param.currentValue(this);
    }


    /**
     * @param ns may be null, having no effect.
     * @return a parameterized evaluator.
     */
    Evaluator parameterizeCurrentNamespace(Namespace ns)
    {
        if (ns == null) return this;

        DynamicParameter param = getGlobalState().myCurrentNamespaceParam;
        return markedContinuation(param, ns);
    }



    CompiledForm compile(Environment env, SyntaxValue source)
        throws FusionException
    {
        return source.doCompile(this, env);
    }

    /**
     * @return not null, but perhaps {@link CompiledForm#EMPTY_ARRAY}.
     */
    CompiledForm[] compile(Environment env, SyntaxSequence source,
                           int from, int to)
        throws FusionException
    {
        int size = to - from;

        if (size == 0) return CompiledForm.EMPTY_ARRAY;

        CompiledForm[] forms = new CompiledForm[size];
        for (int i = from; i < to; i++)
        {
            SyntaxValue form = source.get(this, i);
            forms[i - from] = compile(env, form);
        }

        return forms;
    }

    /**
     * @return not null, but perhaps {@link CompiledForm#EMPTY_ARRAY}.
     */
    CompiledForm[] compile(Environment env, SyntaxSequence source, int from)
        throws FusionException
    {
        return compile(env, source, from, source.size());
    }


    /**
     * @return not null
     */
    Object eval(Store store, CompiledForm form)
        throws FusionException
    {
        evaluating: while (true)
        {
            Object result = form.doEval(this, store);

            checkingResult: while (true)
            {
                if (result instanceof TailForm)
                {
                    TailForm tail = (TailForm) result;
                    store = tail.myStore;
                    form  = tail.myForm;
                    continue evaluating;
                }
                if (result instanceof TailCall)
                {
                    TailCall tail = (TailCall) result;
                    Object[] args = tail.myArgs;
                    checkSingleArgResults(args);
                    result = tail.myProc.doApply(this, args);
                    continue checkingResult;
                }
                if (result == null)
                {
                    result = voidValue(this);
                }
                return result;
            }
        }
    }


    /**
     * Makes a <b>non-tail</b> procedure call.
     * Whenever possible, you should use tail calls instead.
     *
     * @return not null
     *
     * @see #bounceTailCall(Procedure, Object...)
     */
    Object callNonTail(Procedure proc, Object... args)
        throws FusionException
    {
        calling: while (true)
        {
            checkSingleArgResults(args);
            Object result = proc.doApply(this, args);

            checkingResult: while (true)
            {
                if (result instanceof TailForm)
                {
                    TailForm tail = (TailForm) result;
                    result = tail.myForm.doEval(this, tail.myStore);
                    continue checkingResult;
                }
                if (result instanceof TailCall)
                {
                    TailCall tail = (TailCall) result;
                    proc = tail.myProc;
                    args = tail.myArgs;
                    continue calling;
                }
                if (result == null)
                {
                    result = voidValue(this);
                }
                return result;
            }
        }
    }


    final void checkSingleResult(Object values, String formIdentifier)
        throws FusionException
    {
        if (values instanceof Object[])
        {
            Object[] valuesArray = (Object[]) values;
            String expectation =
                "1 result but received " + valuesArray.length;
            throw new ResultFailure(formIdentifier,
                                    expectation, -1, valuesArray);
        }
    }


    private final void checkSingleArgResults(Object[] args)
        throws FusionException
    {
        for (Object o : args)
        {
            checkSingleResult(o, "procedure argument");
        }
    }


    //========================================================================


    /**
     * Wraps an expression for evaluation in tail position.
     * Must be returned back to this {@link Evaluator} for proper behavior.
     */
    Object bounceTailForm(Store store, CompiledForm form)
    {
        return new TailForm(store, form);
    }


    /**
     * Returned from evaluation of a form when evaluation needs to continue in
     * a tail position. This allows the {@link Evaluator} to trampoline into
     * the tail call without growing the stack.  Not the most efficient
     * implementation, but it works.
     */
    private static final class TailForm
    {
        final Store        myStore;
        final CompiledForm myForm;

        TailForm(Store store, CompiledForm form)
        {
            myStore = store;
            myForm  = form;
        }
    }


    /**
     * Makes a procedure call from tail position.
     * The result MUST be immediately returned to the evaluator,
     * it's not a normal value!
     *
     * @return not null
     */
    Object bounceTailCall(Procedure proc, Object... args)
        throws FusionException
    {
        return new TailCall(proc, args);

    }

    /**
     * Returned from evaluation of a form when evaluation needs to continue in
     * a tail position. This allows the {@link Evaluator} to trampoline into
     * the tail call without growing the stack.  Not the most efficient
     * implementation, but it works.
     */
    private static final class TailCall
    {
        final Procedure myProc;
        final Object[]  myArgs;

        TailCall(Procedure proc, Object... args)
        {
            myProc = proc;
            myArgs = args;
        }
    }
}
