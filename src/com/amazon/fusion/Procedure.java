// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionCollection.isCollection;
import static com.amazon.fusion.FusionList.isList;
import static com.amazon.fusion.FusionNumber.checkIntArgToJavaInt;
import static com.amazon.fusion.FusionNumber.checkIntArgToJavaLong;
import static com.amazon.fusion.FusionNumber.checkNullableIntArg;
import static com.amazon.fusion.FusionNumber.checkRequiredIntArg;
import static com.amazon.fusion.FusionSequence.isSequence;
import static com.amazon.fusion.FusionStruct.isStruct;
import com.amazon.fusion.BindingDoc.Kind;
import com.amazon.ion.util.IonTextUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Base class for invocable procedures, both built-in and user-defined.
 * This implements the evaluation of arguments and prevents the procedure from
 * access to the caller's environment.
 */
abstract class Procedure
    extends NamedValue
{
    final static String DOTDOTDOT = "...";
    final static String DOTDOTDOTPLUS = "...+";

    private final BindingDoc myDocs;
    private final int myArity;


    /**
     * @param argNames are used purely for documentation
     */
    Procedure(String doc, String... argNames)
    {
        assert doc == null || ! doc.endsWith("\n");
        myArity = argNames.length;

        StringBuilder buf = new StringBuilder();
        for (String formal : argNames)
        {
            buf.append(' ');
            buf.append(formal);
        }
        String usage = buf.toString();

        myDocs = new BindingDoc(null, Kind.PROCEDURE, usage, doc);
    }


    @Override
    final void nameInferred(String name)
    {
        myDocs.setName(name);
    }


    @Override
    final void identify(Appendable out)
        throws IOException
    {
        String name = getInferredName();
        if (name == null)
        {
            out.append("anonymous procedure");
        }
        else
        {
            out.append("procedure ");
            IonTextUtils.printQuotedSymbol(out, name);
        }
    }


    @Override
    BindingDoc document()
    {
        return myDocs;
    }


    /**
     * Executes a procedure's logic; <b>DO NOT CALL DIRECTLY!</b>
     *
     * @param args must not be null, and none of its elements may be null.
     * @return null is a synonym for {@code void}.
     */
    abstract Object doApply(Evaluator eval, Object[] args)
        throws FusionException;


    //========================================================================
    // Type-checking helpers


    void checkArityExact(int arity, Object[] args)
        throws ArityFailure
    {
        if (args.length != arity)
        {
            throw new ArityFailure(this, arity, arity, args);
        }
    }


    /**
     * Checks arity against the documented argument names.
     */
    void checkArityExact(Object[] args)
        throws ArityFailure
    {
        checkArityExact(myArity, args);
    }


    void checkArityAtLeast(int atLeast, Object[] args)
        throws ArityFailure
    {
        if (args.length < atLeast)
        {
            throw new ArityFailure(this, atLeast, Integer.MAX_VALUE, args);
        }
    }


    void checkArityRange(int atLeast, int atMost, Object[] args)
        throws ArityFailure
    {
        if (args.length < atLeast || args.length > atMost)
        {
            throw new ArityFailure(this, atLeast, atMost, args);
        }
    }


    ArgTypeFailure argFailure(String expectation, int badPos, Object... actuals)
    {
        return new ArgTypeFailure(this, expectation, badPos, actuals);
    }


    <T> T checkArg(Class<T> klass, String desc, int argNum, Object... args)
        throws ArgTypeFailure
    {
        try
        {
            T arg = klass.cast(args[argNum]);
            return klass.cast(arg);
        }
        catch (ClassCastException e) {}

        throw new ArgTypeFailure(this, desc, argNum, args);
    }



    /**
     * Checks that an argument fits safely into Java's {@code int} type.
     *
     * @deprecated Use helpers in {@link FusionNumber}.
     */
    @Deprecated
    int checkIntArg(int argNum, Object... args)
        throws FusionException, ArgTypeFailure
    {
        return checkIntArgToJavaInt(/*eval*/ null,           // NOT SUPPORTED!
                                    this, argNum, args);
    }


    /**
     * Checks that an argument fits safely into Java's {@code long} type.
     *
     * @deprecated Use helpers in {@link FusionNumber}.
     */
    @Deprecated
    long checkLongArg(int argNum, Object... args)
        throws FusionException, ArgTypeFailure
    {
        return checkIntArgToJavaLong(/*eval*/ null,          // NOT SUPPORTED!
                                     this, argNum, args);
    }


    /**
     * @return not null.
     *
     * @deprecated Use helpers in {@link FusionNumber}.
     */
    @Deprecated
    BigInteger checkBigIntArg(int argNum, Object... args)
        throws FusionException, ArgTypeFailure
    {
        return checkRequiredIntArg(/*eval*/ null,            // NOT SUPPORTED!
                                   this, argNum, args);
    }

    /**
     * @return may be null.
     *
     * @deprecated Use helpers in {@link FusionNumber}.
     */
    @Deprecated
    BigInteger checkBigIntArg(Evaluator eval, int argNum, Object... args)
        throws FusionException, ArgTypeFailure
    {
        return checkNullableIntArg(eval, this, argNum, args);
    }


    /**
     * @return not null.
     *
     * @deprecated Use helpers in {@link FusionNumber}.
     */
    @Deprecated
    BigDecimal checkRequiredDecimalArg(Evaluator eval, // Newer name+signature
                                       int argNum,
                                       Object... args)
        throws FusionException, ArgTypeFailure
    {
        return FusionNumber.checkRequiredDecimalArg(eval, this, argNum, args);
    }


    /**
     * Expects a collection argument, including typed nulls.
     */
    Object checkCollectionArg(Evaluator eval, int argNum, Object... args)
        throws ArgTypeFailure
    {
        Object arg = args[argNum];
        if (isCollection(eval, arg)) return arg;

        throw argFailure("collection", argNum, args);
    }


    /**
     * Expects a sequence argument, including typed nulls.
     */
    Object checkSequenceArg(Evaluator eval, int argNum, Object... args)
        throws ArgTypeFailure
    {
        Object arg = args[argNum];
        if (isSequence(eval, arg)) return arg;

        throw argFailure("sequence", argNum, args);
    }

    /**
     * Expects a list argument, including null.list.
     */
    Object checkListArg(Evaluator eval, int argNum, Object... args)
        throws ArgTypeFailure
    {
        Object arg = args[argNum];
        if (isList(eval, arg)) return arg;

        throw argFailure("list", argNum, args);
    }


    /**
     * Expects a struct argument, including null.struct.
     */
    Object checkStructArg(Evaluator eval, int argNum, Object... args)
        throws ArgTypeFailure
    {
        Object arg = args[argNum];
        if (isStruct(eval, arg)) return arg;

        throw argFailure("struct", argNum, args);
    }


    SyntaxValue checkSyntaxArg(int argNum, Object... args)
        throws ArgTypeFailure
    {
        try
        {
            return (SyntaxValue) args[argNum];
        }
        catch (ClassCastException e) {}

        throw new ArgTypeFailure(this, "Syntax value", argNum, args);
    }

    <T extends SyntaxValue> T checkSyntaxArg(Class<T> klass, String typeName,
                                             boolean nullable,
                                             int argNum, Object... args)
        throws ArgTypeFailure
    {
        Object arg = args[argNum];

        try
        {
            SyntaxValue stx = (SyntaxValue) arg;
            if (nullable || ! stx.isNullValue())
            {
                return klass.cast(stx);
            }
        }
        catch (ClassCastException e) {}

        throw new ArgTypeFailure(this, typeName, argNum, args);
    }


    SyntaxSymbol checkSyntaxSymbolArg(int argNum, Object... args)
        throws ArgTypeFailure
    {
        return checkSyntaxArg(SyntaxSymbol.class, "syntax symbol",
                              true /* nullable */, argNum, args);
    }


    SyntaxContainer checkSyntaxContainerArg(int argNum, Object... args)
        throws ArgTypeFailure
    {
        return checkSyntaxArg(SyntaxContainer.class,
                              "syntax_list, sexp, or struct",
                              true /* nullable */, argNum, args);
    }



    SyntaxSequence checkSyntaxSequenceArg(int argNum, Object... args)
        throws ArgTypeFailure
    {
        return checkSyntaxArg(SyntaxSequence.class,
                              "syntax_list or syntax_sexp",
                              true /* nullable */, argNum, args);
    }

    /** Ensures that an argument is a {@link Procedure}. */
    Procedure checkProcArg(int argNum, Object... args)
        throws ArgTypeFailure
    {
        try
        {
            return (Procedure) args[argNum];
        }
        catch (ClassCastException e) {}

        throw new ArgTypeFailure(this, "procedure", argNum, args);
    }

}
