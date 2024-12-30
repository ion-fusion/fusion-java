// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionSymbol.BaseSymbol.internSymbols;
import static dev.ionfusion.fusion.SimpleSyntaxValue.makeSyntax;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import java.io.IOException;


/**
 * Utilities for working with Fusion {@code bool} values.
 *
 * @see FusionValue
 * @see <a href="{@docRoot}/../fusion/bool.html"><code>/fusion/bool</code></a>
 */
public final class FusionBool
{
    private FusionBool() {}


    //========================================================================
    // Representation Classes


    abstract static class BaseBool
        extends BaseValue
    {
        private BaseBool() {}

        abstract boolean isTrue();
        abstract boolean isFalse();
        abstract Boolean asJavaBoolean();

        @Override
        final boolean isAnnotatable()
        {
            return true;
        }

        @Override
        BaseBool annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return this;
            return new AnnotatedBool(annotations, this);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
        {
            return makeSyntax(eval, loc, this);
        }
    }


    private static class NullBool
        extends BaseBool
    {
        private NullBool() {}

        @Override
        boolean isAnyNull() { return true; }

        @Override
        boolean isTrue()    { return false; }

        @Override
        boolean isFalse()   { return false; }

        @Override
        BaseBool isTruthy(Evaluator eval)  { return FALSE_BOOL; }

        @Override
        BaseBool not(Evaluator eval) { return TRUE_BOOL; }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            boolean b = (right instanceof BaseBool
                         && ((BaseBool) right).isAnyNull());
            return makeBool(eval, b);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        Boolean asJavaBoolean() { return null; }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newNullBool();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeNull(IonType.BOOL);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("null.bool");
        }
    }


    private static class TrueBool
        extends BaseBool
    {
        private TrueBool() {}

        @Override
        boolean isTrue()    { return true; }

        @Override
        boolean isFalse()   { return false; }

        @Override
        BaseBool isTruthy(Evaluator eval)  { return TRUE_BOOL; }

        @Override
        BaseBool not(Evaluator eval) { return FALSE_BOOL; }

        @Override
        Boolean asJavaBoolean() { return TRUE; }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseBool)
            {
                if (((BaseBool) right).isTrue())
                {
                    return TRUE_BOOL;
                }
            }

            return FALSE_BOOL;
        }


        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newBool(true);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeBool(true);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("true");
        }
    }


    private static class FalseBool
        extends BaseBool
    {
        private FalseBool() {}

        @Override
        boolean isTrue()    { return false; }

        @Override
        boolean isFalse()   { return true; }

        @Override
        BaseBool isTruthy(Evaluator eval)  { return FALSE_BOOL; }

        @Override
        BaseBool not(Evaluator eval) { return TRUE_BOOL; }

        @Override
        Boolean asJavaBoolean() { return FALSE; }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseBool)
            {
                if (((BaseBool) right).isFalse())
                {
                    return TRUE_BOOL;
                }
            }

            return FALSE_BOOL;
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newBool(false);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeBool(false);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("false");
        }
    }


    private static final class AnnotatedBool
        extends BaseBool
    {
        /** Not null or empty */
        final BaseSymbol[] myAnnotations;

        /** Not null, and not AnnotatedBool */
        final BaseBool  myValue;

        private AnnotatedBool(BaseSymbol[] annotations, BaseBool value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
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
        BaseBool annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return myValue.annotate(eval, annotations);
        }

        @Override
        boolean isAnyNull() { return myValue.isAnyNull(); }

        @Override
        boolean isTrue()    { return myValue.isTrue(); }

        @Override
        boolean isFalse()   { return myValue.isFalse(); }

        @Override
        BaseBool isTruthy(Evaluator eval)  { return myValue.isTruthy(eval); }

        @Override
        BaseBool not(Evaluator eval) { return myValue.not(eval); }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return myValue.tightEquals(eval, right);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return myValue.looseEquals(eval, right);
        }

        @Override
        Boolean asJavaBoolean() { return myValue.asJavaBoolean(); }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            IonValue iv = myValue.copyToIonValue(factory,
                                                 throwOnConversionFailure);
            iv.setTypeAnnotations(getAnnotationsAsJavaStrings());
            return iv;
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());
            myValue.ionize(eval, out);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            writeAnnotations(out, myAnnotations);
            myValue.write(eval, out);
        }
    }


    //========================================================================
    // Constructors


    private static final BaseBool NULL_BOOL  = new NullBool();
    private static final TrueBool TRUE_BOOL  = new TrueBool();
    private static final FalseBool FALSE_BOOL = new FalseBool();


    static TrueBool trueBool(Evaluator eval)
    {
        return TRUE_BOOL;
    }

    static FalseBool falseBool(Evaluator eval)
    {
        return FALSE_BOOL;
    }


    static BaseBool makeBool(Evaluator eval, boolean value)
    {
        return (value ? TRUE_BOOL : FALSE_BOOL);
    }


    /**
     * @param value may be null to make {@code null.string}.
     *
     * @return not null.
     */
    static BaseBool makeBool(Evaluator eval, Boolean value)
    {
        return (value == null ? NULL_BOOL : makeBool(eval, (boolean) value));
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseBool makeBool(Evaluator eval,
                             String[]  annotations,
                             boolean   value)
    {
        BaseBool base = makeBool(eval, value);
        return base.annotate(eval, internSymbols(annotations));
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     * @param value may be null to make {@code null.string}.
     *
     * @return not null.
     */
    static BaseBool makeBool(Evaluator eval,
                             String[]  annotations,
                             Boolean   value)
    {
        BaseBool base = makeBool(eval, value);
        return base.annotate(eval, internSymbols(annotations));
    }


    /**
     * @param fusionBool must be a Fusion bool.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseBool unsafeBoolAnnotate(Evaluator eval,
                                       Object    fusionBool,
                                       String[]  annotations)
    {
        BaseBool base = (BaseBool) fusionBool;
        return base.annotate(eval, internSymbols(annotations));
    }


    //========================================================================
    // Predicates


    /**
     * Determines whether a Fusion value has type {@code bool}.
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value the value to test
     *
     * @return {@code true} if the value is a Fusion bool,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isBool(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseBool);
    }

    static boolean isBool(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseBool);
    }


    /**
     * Determines whether a Fusion {@code bool} value is {@code true}.
     *
     * @param value must be a Fusion {@code bool}.
     */
    static boolean unsafeBoolIsTrue(Evaluator eval, Object value)
        throws FusionException
    {
        BaseBool b = (BaseBool) value;
        return b.isTrue();
    }

    /**
     * Determines whether a Fusion value is {@code true}.
     * Equivalent to {@code (== true value)}.
     * <p>
     * This is <em>not</em> a
     * <a href="{@docRoot}/../fusion/bool.html#truthiness">truthiness</a>
     * test; use {@link #isTruthy(TopLevel, Object)} for that purpose.
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value the value to test
     *
     * @return {@code true} if the value is Fusion's {@code true} value,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isTrue(TopLevel top, Object value)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return isTrue(eval, value);
    }

    static boolean isTrue(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseBool)
        {
            BaseBool b = (BaseBool) value;
            return b.isTrue();
        }
        return false;
    }


    /**
     * Determines whether a Fusion value is {@code false}.
     * Equivalent to {@code (== false value)}.
     * <p>
     * This is <em>not</em> a
     * <a href="{@docRoot}/../fusion/bool.html#truthiness">truthiness</a>
     * test; use {@link #isTruthy(TopLevel, Object)} for that purpose.
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value the value to test
     *
     * @return {@code true} if the value is Fusion's {@code false} value,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isFalse(TopLevel top, Object value)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return isFalse(eval, value);
    }

    static boolean isFalse(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseBool)
        {
            BaseBool b = (BaseBool) value;
            return b.isFalse();
        }

        return false;
    }


    /**
     * Determines whether a given Fusion value is "truthy".
     * Fusion defines
     * <a href="{@docRoot}/../fusion/bool.html#truthiness">truthiness</a>
     * as follows:
     * <ul>
     *   <li>
     *     Every value is truthy except for {@code false}, void, and any
     *     variant of {@code null}.
     *   </li>
     * </ul>
     * This definition is more lax (and hopefully more convenient) than Java,
     * but less lenient (and hopefully less error-prone) than C or C++.
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value the value to test
     *
     * @return {@code true} if the value is truthy,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     *
     * @see <a href="{@docRoot}/../fusion/bool.html#truthiness">Truthiness</a>
     * @see FusionBool#isTrue(TopLevel, Object)
     */
    public static boolean isTruthy(TopLevel top, Object value)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return isTruthy(eval, value).isTrue();
    }

    static BaseBool isTruthy(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseValue)
        {
            return ((BaseValue) value).isTruthy(eval);
        }

        return trueBool(eval);
    }


    //========================================================================
    // Conversions


    /**
     * @param fusionBool must be a Fusion bool.
     *
     * @return null if given {@code null.bool}.
     */
    static Boolean unsafeBoolToJavaBoolean(Evaluator eval, Object fusionBool)
    {
        return ((BaseBool) fusionBool).asJavaBoolean();
    }


    /**
     * Converts a Fusion bool to a Java {@link Boolean}.
     *
     * @return null if the value isn't Fusion true or false.
     */
    static Boolean boolToJavaBoolean(Evaluator eval, Object value)
        throws FusionException
    {
        if (isBool(eval, value))
        {
            return unsafeBoolToJavaBoolean(eval, value);
        }
        return null;
    }


    //========================================================================
    // Procedures


    static final class IsBoolProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isBool(eval, arg);
            return makeBool(eval, r);
        }
    }
}
