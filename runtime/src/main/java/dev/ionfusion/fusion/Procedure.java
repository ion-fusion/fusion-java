// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionCollection.checkNullableCollectionArg;
import static dev.ionfusion.fusion.FusionList.checkNullableListArg;
import static dev.ionfusion.fusion.FusionNumber.checkIntArgToJavaInt;
import static dev.ionfusion.fusion.FusionNumber.checkIntArgToJavaLong;
import static dev.ionfusion.fusion.FusionNumber.checkNullableIntArg;
import static dev.ionfusion.fusion.FusionNumber.checkRequiredIntArg;
import static dev.ionfusion.fusion.FusionSequence.checkNullableSequenceArg;
import static dev.ionfusion.fusion.FusionStruct.checkNullableStructArg;

import com.amazon.ion.util.IonTextUtils;
import java.io.IOException;
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

    private final int myArity;


    /**
     * @param argNames are used to determine the result
     *   of {@link #checkArityExact(Object[])}.
     *
     * @deprecated since Release 17, 2014-06-18.
     */
    @Deprecated
    Procedure(String... argNames)
    {
        myArity = argNames.length;
    }

    Procedure()
    {
        myArity = -1;
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


    final void checkArityExact(int arity, Object[] args)
        throws ArityFailure
    {
        if (args.length != arity)
        {
            throw new ArityFailure(this, arity, arity, args);
        }
    }


    /**
     * Checks arity against the documented argument names.
     *
     * @deprecated since Release 17, 2014-06-18.
     * Use {@link #checkArityExact(int, Object[])} instead.
     */
    @Deprecated
    final void checkArityExact(Object[] args)
        throws ArityFailure
    {
        if (myArity < 0)
        {
            String message =
                "This Procedure instance used the no-args constructor from " +
                "which arity cannot be inferred. It must use one of the " +
                "un-deprecated checkArity methods.";
            throw new IllegalStateException(message);
        }
        checkArityExact(myArity, args);
    }


    final void checkArityAtLeast(int atLeast, Object[] args)
        throws ArityFailure
    {
        if (args.length < atLeast)
        {
            throw new ArityFailure(this, atLeast, Integer.MAX_VALUE, args);
        }
    }


    final void checkArityRange(int atLeast, int atMost, Object[] args)
        throws ArityFailure
    {
        if (args.length < atLeast || args.length > atMost)
        {
            throw new ArityFailure(this, atLeast, atMost, args);
        }
    }


    /**
     * Returns a new {@link ContractException} with the given message and
     * the identification of this value. This is preferable to creating
     * the exception directly, since this method can annotate it with location
     * information.
     * <p>
     * Expected usage:
     * <pre>
     * if (somethingBadHappened)
     * {
     *   throw contractFailure("somebody screwed up");
     * }
     * </pre>
     *
     * @param message the message to render in the exception.
     * Must not be null.
     *
     * @return a new exception
     */
    ContractException contractFailure(String message)
    {
        return new ContractException(identify() + ": " + message);
    }

    /**
     * Returns a new {@link ContractException} with the given message and
     * cause, and with
     * the identification of this value. This is preferable to creating
     * the exception directly, since this method can annotate it with location
     * information.
     * <p>
     * Expected usage:
     * <pre>
     * catch (SomeException e)
     * {
     *   throw contractFailure("somebody screwed up", e);
     * }
     * </pre>
     *
     * @param message the message to render in the exception.
     * Must not be null.
     * @param cause may be null.
     *
     * @return a new exception
     */
    ContractException contractFailure(String message, Throwable cause)
    {
        return new ContractException(identify() + ": " + message, cause);
    }


    /**
     * Creates, but does not throw, an exception that indicates a contract
     * failure with a given argument.
     *
     * @param expectation describes the expectation that was not met.  When
     * displayed, this string is prefixed by "procedure <i>p</i> expects",
     * so the content should be a noun phrase.
     * @param badPos the zero-based index of the problematic value.
     * -1 means a specific position isn't implicated.
     * @param actuals the provided procedure arguments;
     * must not be null or zero-length.
     *
     * @return a new exception.
     */
    final ArgumentException argFailure(String expectation,
                                       int badPos,
                                       Object... actuals)
    {
        return new ArgumentException(this, expectation, badPos, actuals);
    }


    final <T> T checkArg(Class<T> klass, String desc, int argNum,
                         Object... args)
        throws ArgumentException
    {
        try
        {
            return klass.cast(args[argNum]);
        }
        catch (ClassCastException e)
        {
            throw new ArgumentException(this, desc, argNum, args);
        }
    }



    /**
     * Checks that an argument fits safely into Java's {@code int} type.
     *
     * @deprecated Use helpers in {@link FusionNumber}.
     */
    @Deprecated
    final int checkIntArg(int argNum, Object... args)
        throws FusionException, ArgumentException
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
    final long checkLongArg(int argNum, Object... args)
        throws FusionException, ArgumentException
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
    final BigInteger checkBigIntArg(int argNum, Object... args)
        throws FusionException, ArgumentException
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
    final BigInteger checkBigIntArg(Evaluator eval, int argNum, Object... args)
        throws FusionException, ArgumentException
    {
        return checkNullableIntArg(eval, this, argNum, args);
    }


    /**
     * Expects a collection argument, including typed nulls.
     *
     * @deprecated Use
     * {@link FusionCollection#checkNullableCollectionArg(Evaluator, Procedure, int, Object...)}
     */
    @Deprecated
    final Object checkCollectionArg(Evaluator eval, int argNum, Object... args)
        throws FusionException, ArgumentException
    {
        return checkNullableCollectionArg(eval, this, argNum, args);
    }


    /**
     * Expects a sequence argument, including typed nulls.
     *
     * @deprecated Use
     * {@link FusionSequence#checkNullableSequenceArg(Evaluator, Procedure, int, Object...)}
     */
    @Deprecated
    final Object checkSequenceArg(Evaluator eval, int argNum, Object... args)
        throws FusionException, ArgumentException
    {
        return checkNullableSequenceArg(eval, this, argNum, args);
    }

    /**
     * Expects a list argument, including null.list.
     *
     * @deprecated Use
     * {@link FusionList#checkNullableListArg(Evaluator, Procedure, int, Object...)}
     */
    @Deprecated
    final Object checkListArg(Evaluator eval, int argNum, Object... args)
        throws FusionException, ArgumentException
    {
        return checkNullableListArg(eval, this, argNum, args);
    }


    /**
     * Expects a struct argument, including null.struct.
     *
     * @deprecated Use
     * {@link FusionStruct#checkNullableStructArg(Evaluator, Procedure, int, Object...)}
     */
    @Deprecated
    final Object checkStructArg(Evaluator eval, int argNum, Object... args)
        throws FusionException, ArgumentException
    {
        return checkNullableStructArg(eval, this, argNum, args);
    }


    @Deprecated
    final SyntaxValue checkSyntaxArg(int argNum, Object... args)
        throws ArgumentException
    {
        try
        {
            return (SyntaxValue) args[argNum];
        }
        catch (ClassCastException e)
        {
            throw new ArgumentException(this, "Syntax value", argNum, args);
        }
    }

    private <T extends SyntaxValue> T checkSyntaxArg(Class<T> klass,
                                                     String typeName,
                                                     boolean nullable,
                                                     int argNum,
                                                     Object... args)
        throws ArgumentException
    {
        Object arg = args[argNum];

        try
        {
            SyntaxValue stx = (SyntaxValue) arg;
            if (nullable || ! stx.isAnyNull())
            {
                return klass.cast(stx);
            }
        }
        catch (ClassCastException e) {}

        throw new ArgumentException(this, typeName, argNum, args);
    }


    @Deprecated
    final SyntaxContainer checkSyntaxContainerArg(int argNum, Object... args)
        throws ArgumentException
    {
        return checkSyntaxArg(SyntaxContainer.class,
                              "syntax_list, sexp, or struct",
                              true /* nullable */, argNum, args);
    }


    @Deprecated
    final SyntaxSequence checkSyntaxSequenceArg(int argNum, Object... args)
        throws ArgumentException
    {
        return checkSyntaxArg(SyntaxSequence.class,
                              "syntax_list or syntax_sexp",
                              true /* nullable */, argNum, args);
    }


    /** Ensures that an argument is a {@link Procedure}. */
    final Procedure checkProcArg(int argNum, Object... args)
        throws ArgumentException
    {
        try
        {
            return (Procedure) args[argNum];
        }
        catch (ClassCastException e)
        {
            throw new ArgumentException(this, "procedure", argNum, args);
        }
    }

}
