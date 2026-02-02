// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.ArgumentException.makeArgumentError;

import com.amazon.ion.util.IonTextUtils;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import java.io.IOException;

/**
 * Base class for invocable procedures, both built-in and user-defined.
 * This implements the evaluation of arguments and prevents the procedure from
 * access to the caller's environment.
 */
abstract class Procedure
    extends BaseValue
    implements NamedValue<BaseSymbol>
{
    final static String DOTDOTDOT = "...";
    final static String DOTDOTDOTPLUS = "...+";


    private BaseSymbol myName;
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
    public final void inferObjectName(BaseSymbol name)
    {
        if (myName == null)
        {
            myName = name;
        }
    }

    @Override
    public final BaseSymbol objectName()
    {
        return myName;
    }

    final String getInferredName()
    {
        return myName == null ? null : myName.stringValue();
    }

    final String identify()
    {
        if (myName == null)
        {
            return "anonymous procedure";
        }

        return "procedure " + IonTextUtils.printQuotedSymbol(myName.stringValue());
    }

    @Override
    public final void write(Evaluator eval, Appendable out)
        throws IOException
    {
        out.append("{{{");
        out.append(identify());
        out.append("}}}");
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
     * Creates, but does not throw, a Fusion {@code argument_error} indicating
     * a contract failure with a given argument.
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
    final FusionException argError(Evaluator eval,
                                   String expectation,
                                   int badPos,
                                   Object... actuals)
    {
        return makeArgumentError(eval, identify(), expectation, badPos, actuals);
    }


    final <T> T checkArg(Evaluator eval, Class<T> klass, String desc, int argNum,
                         Object... args)
        throws FusionException
    {
        try
        {
            return klass.cast(args[argNum]);
        }
        catch (ClassCastException e)
        {
            throw argError(eval, desc, argNum, args);
        }
    }


    @Deprecated
    final SyntaxValue checkSyntaxArg(Evaluator eval, int argNum, Object... args)
        throws FusionException
    {
        try
        {
            return (SyntaxValue) args[argNum];
        }
        catch (ClassCastException e)
        {
            throw argError(eval, "Syntax value", argNum, args);
        }
    }

    private <T extends SyntaxValue> T checkSyntaxArg(Evaluator eval,
                                                     Class<T> klass,
                                                     String typeName,
                                                     boolean nullable,
                                                     int argNum,
                                                     Object... args)
        throws FusionException
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

        throw argError(eval, typeName, argNum, args);
    }


    @Deprecated
    final SyntaxContainer checkSyntaxContainerArg(Evaluator eval, int argNum, Object... args)
        throws FusionException
    {
        return checkSyntaxArg(eval,
                              SyntaxContainer.class,
                              "syntax_list, sexp, or struct",
                              true /* nullable */, argNum, args);
    }


    @Deprecated
    final SyntaxSequence checkSyntaxSequenceArg(Evaluator eval, int argNum, Object... args)
        throws FusionException
    {
        return checkSyntaxArg(eval,
                              SyntaxSequence.class,
                              "syntax_list or syntax_sexp",
                              true /* nullable */, argNum, args);
    }


    /** Ensures that an argument is a {@link Procedure}. */
    final Procedure checkProcArg(Evaluator eval, int argNum, Object... args)
        throws FusionException
    {
        try
        {
            return (Procedure) args[argNum];
        }
        catch (ClassCastException e)
        {
            throw argError(eval, "procedure", argNum, args);
        }
    }

}
