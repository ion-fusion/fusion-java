// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionList.listFromIonSequence;
import static dev.ionfusion.fusion.FusionNull.makeNullNull;
import static dev.ionfusion.fusion.FusionNumber.makeDecimal;
import static dev.ionfusion.fusion.FusionNumber.makeFloat;
import static dev.ionfusion.fusion.FusionNumber.makeInt;
import static dev.ionfusion.fusion.FusionSexp.emptySexp;
import static dev.ionfusion.fusion.FusionSexp.pair;
import static dev.ionfusion.fusion.FusionSexp.sexpFromIonSequence;
import static dev.ionfusion.fusion.FusionString.makeString;
import static dev.ionfusion.fusion.FusionStruct.structFromIonStruct;
import static dev.ionfusion.fusion.FusionSymbol.makeSymbol;
import static dev.ionfusion.fusion.FusionTimestamp.makeTimestamp;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion.ResultFailure.makeResultError;
import static dev.ionfusion.fusion._private.FusionUtils.friendlyIndex;

import com.amazon.ion.IonBool;
import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonDecimal;
import com.amazon.ion.IonFloat;
import com.amazon.ion.IonInt;
import com.amazon.ion.IonList;
import com.amazon.ion.IonLob;
import com.amazon.ion.IonSexp;
import com.amazon.ion.IonString;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSymbol;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonTimestamp;
import com.amazon.ion.IonValue;
import com.amazon.ion.Timestamp;
import com.amazon.ion.system.IonReaderBuilder;
import dev.ionfusion.runtime.base.SourceLocation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Main entry point to the Fusion evaluation engine.
 */
class Evaluator
{
    private final GlobalState myGlobalState;
    private final IonSystem mySystem;
    private final Evaluator myOuterFrame;
    private final Map<Object, Object> myContinuationMarks = new HashMap<>();


    Evaluator(GlobalState globalState)
    {
        myGlobalState = globalState;
        mySystem      = globalState.myIonSystem;
        myOuterFrame  = null;
    }

    Evaluator(Evaluator outerBindings)
    {
        myGlobalState = outerBindings.myGlobalState;
        mySystem      = outerBindings.mySystem;
        myOuterFrame  = outerBindings;
    }


    IonSystem getSystem()
    {
        return mySystem;
    }


    IonReaderBuilder getIonReaderBuilder()
    {
        return myGlobalState.myIonReaderBuilder;
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

        // TODO this copied array is wasted for containers
        String[] annotations = value.getTypeAnnotations();

        switch (value.getType())
        {
            case NULL:
            {
                return makeNullNull(this, annotations);
            }
            case BOOL:
            {
                Boolean b = (value.isNullValue()
                                 ? null
                                 : ((IonBool)value).booleanValue());
                return makeBool(this, annotations, b);
            }
            case INT:
            {
                BigInteger big = ((IonInt)value).bigIntegerValue();
                return makeInt(this, annotations, big);
            }
            case FLOAT:
            {
                if (value.isNullValue())
                {
                    return makeFloat(this, annotations, null);
                }
                double d = ((IonFloat)value).doubleValue();
                return makeFloat(this, annotations, d);
            }
            case DECIMAL:
            {
                BigDecimal big = ((IonDecimal)value).decimalValue();
                return makeDecimal(this, annotations, big);
            }
            case TIMESTAMP:
            {
                Timestamp t = ((IonTimestamp)value).timestampValue();
                return makeTimestamp(this, annotations, t);
            }
            case SYMBOL:
            {
                String text = ((IonSymbol)value).stringValue();
                return makeSymbol(this, annotations, text);
            }
            case STRING:
            {
                String text = ((IonString)value).stringValue();
                return makeString(this, annotations, text);
            }
            case CLOB:
            {
                byte[] bytes = ((IonLob)value).getBytes();
                return FusionClob.forBytesNoCopy(this, annotations, bytes);
            }
            case BLOB:
            {
                byte[] bytes = ((IonLob)value).getBytes();
                return FusionBlob.forBytesNoCopy(this, annotations, bytes);
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
            case DATAGRAM:
            {
                IonDatagram dg = (IonDatagram) value;
                return listFromIonSequence(this, dg);
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
            return makeDecimal(this, (BigDecimal) value);
        }
        else if (value instanceof Double)
        {
            return makeFloat(this, value.doubleValue());
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
        else if (javaValue instanceof BaseValue)
        {
            return javaValue;
        }
        else if (javaValue instanceof IonValue)
        {
            return inject((IonValue) javaValue);
        }
        else if (javaValue instanceof String)
        {
            return makeString(this, (String) javaValue);
        }
        else if (javaValue instanceof Number)
        {
            return injectMaybe((Number) javaValue);
        }
        else if (javaValue instanceof Boolean)
        {
            return makeBool(this, (Boolean) javaValue);
        }
        else if (javaValue instanceof byte[])
        {
            return FusionBlob.forBytesNoCopy(this, (byte[]) javaValue);
        }

        // ******** Be sure to document types as they are added! ********


        // TODO should handle Timestamp, Object[], ArrayList, ...
        //  https://github.com/ion-fusion/fusion-java/issues/70

        // TODO this API forces us to use a non-null object for VOID!
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
     * Collects all marks for {@code key} in the current continuation into a
     * sexp, with the most recent mark first.
     *
     * @return a non-null sexp.
     */
    Object continuationMarkSexp(Object key)
    {
        Object sexp;
        if (myOuterFrame != null)
        {
            sexp = myOuterFrame.continuationMarkSexp(key);
        }
        else
        {
            sexp = emptySexp(this);
        }

        Object value = myContinuationMarks.get(key);
        if (value != null)
        {
            sexp = pair(this, value, sexp);
        }

        return sexp;
    }


    Evaluator addContinuationFrame()
    {
        return new Evaluator(this);
    }

    private void setMark(Object key, Object mark)
    {
        // The keys must be hashable and equals-able!
        assert key instanceof DynamicParameter;
        assert mark != null;
        myContinuationMarks.put(key, mark);
    }

    final Evaluator markedContinuation(Object key, Object mark)
    {
        Evaluator e = addContinuationFrame();
        e.setMark(key, mark);
        return e;
    }

    final Evaluator markedContinuation(Object[] keys, Object[] marks)
    {
        Evaluator e = addContinuationFrame();

        assert keys.length == marks.length;
        for (int i = 0; i < keys.length; i++)
        {
            e.setMark(keys[i], marks[i]);
        }

        return e;
    }

    final Evaluator markedContinuation(Object[] keyMarkPairs)
    {
        Evaluator e = addContinuationFrame();

        assert keyMarkPairs.length %2 == 0;
        for (int i = 0; i < keyMarkPairs.length; i++)
        {
            Object key  = keyMarkPairs[i++];
            Object mark = keyMarkPairs[i];
            e.setMark(key, mark);
        }

        return e;
    }


    //========================================================================

    ModuleNameResolver findResolver()
    {
        // TODO This should be a parameter.
        return myGlobalState.myModuleNameResolver;
    }

    Namespace findCurrentNamespace()
    {
        DynamicParameter param = getGlobalState().myCurrentNamespaceParam;
        return param.currentValue(this);
    }


    /**
     * Returns an evaluator with {@code current_namespace} parameterized to a
     * given namespace.
     *
     * @param ns may be null, having no effect.
     * @return a parameterized evaluator.
     */
    Evaluator parameterizeCurrentNamespace(Namespace ns)
    {
        if (ns == null) return this;

        DynamicParameter param = getGlobalState().myCurrentNamespaceParam;
        return markedContinuation(param, ns);
    }


    Compiler makeCompiler()
    {
        return new Compiler(this);
    }

    final CompiledForm compile(Environment env, SyntaxValue source)
        throws FusionException
    {
        Compiler comp = makeCompiler();
        return comp.compileExpression(env, source);
    }

    final Object evalExpandedStx(Namespace ns, SyntaxValue expanded)
        throws FusionException
    {
        CompiledForm compiled = compile(ns, expanded);
        return eval(ns, compiled);
    }

    /**
     * @see FusionEval#evalCompileTimePartOfTopLevel
     */
    void evalCompileTimePart(TopLevelNamespace topNs, SyntaxValue topStx)
        throws FusionException
    {
        Compiler comp = makeCompiler();
        comp.evalCompileTimePart(topNs, topStx);
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
                if (Thread.currentThread().isInterrupted())
                {
                    throw new FusionInterrupt();
                }

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
                    try
                    {
                        Object[] args = tail.myArgs;
                        checkSingleArgResults(args);
                        result = tail.myProc.doApply(this, args);
                    }
                    catch (FusionException e)
                    {
                        e.addContext(tail.myLoc);
                        throw e;
                    }

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


    Object eval(Store store, CompiledForm form, SourceLocation loc)
        throws FusionException
    {
        try
        {
            return eval(store, form);
        }
        catch (FusionException e)
        {
            e.addContext(loc);
            throw e;
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
        SourceLocation callLocation = null;

        calling: while (true)
        {
            Object result;
            try
            {
                checkSingleArgResults(args);
                result = proc.doApply(this, args);
            }
            catch (FusionException e)
            {
                e.addContext(callLocation);
                throw e;
            }

            checkingResult: while (true)
            {
                if (Thread.currentThread().isInterrupted())
                {
                    throw new FusionInterrupt();
                }

                if (result instanceof TailForm)
                {
                    TailForm tail = (TailForm) result;
                    result = tail.myForm.doEval(this, tail.myStore);
                    continue checkingResult;
                }
                if (result instanceof TailCall)
                {
                    TailCall tail = (TailCall) result;
                    callLocation = tail.myLoc;
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


    final void checkSingleResult(Object values,
                                 long   index,
                                 String formIdentifier)
        throws FusionException
    {
        if (values instanceof Object[])
        {
            if (index >= 0)
            {
                formIdentifier = friendlyIndex(index) + ' ' + formIdentifier;
            }

            Object[] valuesArray = (Object[]) values;
            String expectation =
                "1 result but received " + valuesArray.length;
            throw makeResultError(this, formIdentifier, expectation, valuesArray);
        }
    }

    final void checkSingleResult(Object values, String formIdentifier)
        throws FusionException
    {
        checkSingleResult(values, -1, formIdentifier);
    }


    private void checkSingleArgResults(Object[] args)
        throws FusionException
    {
        int len = args.length;
        for (int i = 0; i < len; i++)
        {
            checkSingleResult(args[i], i, "procedure argument");
        }
    }


    //========================================================================


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
    private static final class TailCall
    {
        final SourceLocation myLoc;
        final Procedure myProc;
        final Object[]  myArgs;

        TailCall(SourceLocation loc, Procedure proc, Object... args)
        {
            myLoc  = loc;
            myProc = proc;
            myArgs = args;
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
        return new TailCall(null, proc, args);
    }


    /**
     * Makes a procedure call from tail position.
     * The result MUST be immediately returned to the evaluator,
     * it's not a normal value!
     *
     * @return not null
     */
    Object bounceTailCall(SourceLocation loc, Procedure proc, Object... args)
        throws FusionException
    {
        return new TailCall(loc, proc, args);
    }
}
