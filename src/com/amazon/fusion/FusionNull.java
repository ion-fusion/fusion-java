// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.makeBool;
import static com.amazon.fusion.FusionSymbol.BaseSymbol.internSymbols;
import static com.amazon.fusion.SimpleSyntaxValue.makeSyntax;
import com.amazon.fusion.FusionBool.BaseBool;
import com.amazon.fusion.FusionSymbol.BaseSymbol;
import com.amazon.ion.IonException;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import java.io.IOException;

/**
 * Utilities for working with Fusion {@code null} values.
 *
 * @see FusionValue
 */
public final class FusionNull
{
    /** Not for application use. */
    private FusionNull() {}


    static class NullNull
        extends BaseValue
    {
        private NullNull() {}

        @Override
        boolean isAnyNull()
        {
            return true;
        }

        @Override
        final boolean isAnnotatable()
            throws FusionException
        {
            return true;
        }

        @Override
        NullNull annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return NULL_NULL;
            return new AnnotatedNullNull(annotations);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return makeBool(eval, right instanceof NullNull);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
        {
            return makeSyntax(eval, loc, this);
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newNull();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeNull();
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("null");
        }
    }


    private static final class AnnotatedNullNull
        extends NullNull
    {
        /** Not null or empty */
        final BaseSymbol[] myAnnotations;

        AnnotatedNullNull(BaseSymbol[] annotations)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
        }

        @Override
        boolean isAnnotated()
        {
            return true;
        }

        @Override
        BaseSymbol[] getAnnotations()
        {
            return myAnnotations;
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            IonValue iv = factory.newNull();
            iv.setTypeAnnotations(getAnnotationsAsJavaStrings());
            return iv;
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());
            out.writeNull();
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            writeAnnotations(out, myAnnotations);
            out.append("null");
        }
    }


    //========================================================================
    // Constructors


    private static final NullNull NULL_NULL = new NullNull();


    static NullNull makeNullNull(Evaluator eval)
    {
        return NULL_NULL;
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static NullNull makeNullNull(Evaluator eval, String[] annotations)
    {
        if (annotations.length == 0)
        {
            return NULL_NULL;
        }

        return new AnnotatedNullNull(internSymbols(annotations));
    }


    //========================================================================
    // Predicates


    /**
     * Determines whether a Fusion value has type {@code null}; that is, is it
     * {@code null.null}?
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value the value to test
     *
     * @return {@code true} if the value is a Fusion {@code null.null} value,
     * otherwise {@code false}
     *
     * @see FusionValue#isAnyNull(TopLevel, Object)
     */
    public static boolean isNullNull(TopLevel top, Object value)
    {
        return (value instanceof NullNull);
    }

    static boolean isNullNull(Evaluator eval, Object value)
    {
        return (value instanceof NullNull);
    }


    //========================================================================
    // Procedures


    static final class IsNullNullProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean isNull = isNullNull(eval, arg);
            return makeBool(eval, isNull);
        }
    }
}
