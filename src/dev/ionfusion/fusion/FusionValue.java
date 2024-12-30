// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionSymbol.BaseSymbol.internSymbols;
import static dev.ionfusion.fusion.FusionUtils.EMPTY_OBJECT_ARRAY;
import static dev.ionfusion.fusion.FusionUtils.EMPTY_STRING_ARRAY;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import com.amazon.ion.IonValue;
import com.amazon.ion.ValueFactory;
import java.io.IOException;

/**
 * Utilities for working with arbitrary Fusion runtime values.
 * Note that the set of Fusion values is a superset of the Ion values, so not
 * all of them are representable as Ion data values.
 */
public final class FusionValue
{
    private FusionValue() {}


    private static final class Undef
        extends BaseValue
    {
        @Override
        void write(Evaluator eval, Appendable out) throws IOException
        {
            out.append("{{{undefined}}}");
        }
    }


    /** The singular {@code undef} value. */
    static final BaseValue UNDEF = new Undef();


    //========================================================================


    /**
     * Determines whether a Fusion value is a null of any type; that is, is it
     * {@code null.null}, {@code null.bool}, {@code null.int}, <em>etc.</em>?
     * This is equivalent to the Fusion procedure {@code is_null}.
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value the value to test
     *
     * @see FusionNull#isNullNull(Evaluator, Object)
     *
     * @return {@code true} if the value is a null of any type,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isAnyNull(TopLevel top, Object value)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return isAnyNull(eval, value);
    }

    static boolean isAnyNull(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseValue)
        {
            return ((BaseValue) value).isAnyNull();
        }

        return false;
    }


    /**
     * Determines whether a given Fusion value is "truthy".
     * Fusion defines truthiness as follows:
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
     *
     * @deprecated As of R15 in March 2014.
     * Moved to {@link FusionBool#isTruthy(TopLevel, Object)}.
     */
    @Deprecated
    public static boolean isTruthy(TopLevel top, Object value)
        throws FusionException
    {
        return FusionBool.isTruthy(top, value);
    }

    /**
     * @deprecated As of R15 in March 2014.
     * Moved to {@link FusionBool#isTruthy(Evaluator, Object)}.
     */
    @Deprecated
    static BaseBool isTruthy(Evaluator eval, Object value)
        throws FusionException
    {
        return FusionBool.isTruthy(eval, value);
    }


    static BaseBool not(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseValue)
        {
            return ((BaseValue) value).not(eval);
        }

        return falseBool(eval);
    }


    //========================================================================
    // Annotations


    static boolean isAnnotatable(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseValue)
        {
            return ((BaseValue) value).isAnnotatable();
        }
        return false;
    }


    /**
     * Replaces or removes annotations on a Fusion value, generally making a
     * (shallow) copy of the value while doing so.
     *
     * @param value must be an annotatable Fusion value.
     * @param annotations must not be null and must not contain elements
     * that are null or empty.
     *
     * @return a Fusion value.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    static Object annotate(Evaluator eval, Object value, String[] annotations)
        throws FusionException
    {
        return ((BaseValue) value).annotate(eval, internSymbols(annotations));
    }

    /**
     * Replaces or removes annotations on a Fusion value, generally making a
     * (shallow) copy of the value while doing so.
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value must be an annotatable Fusion value.
     * @param annotations must not be null and must not contain elements
     * that are null or empty.
     *
     * @return a Fusion value.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static Object annotate(TopLevel top, Object value,
                                  String[] annotations)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return annotate(eval, value, annotations);
    }


    /**
     * Determines whether a Fusion value has any annotations.
     */
    static boolean isAnnotated(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseValue)
        {
            return ((BaseValue) value).isAnnotated();
        }
        return false;
    }


    /**
     * Gets the annotations on a Fusion value as Fusion symbols.
     *
     * @return not null, but possibly empty, array of Fusion symbols.
     * <b>Must not be modified by the caller!</b>
     */
    static Object[] annotations(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseValue)
        {
            return ((BaseValue) value).getAnnotations();
        }
        return EMPTY_OBJECT_ARRAY;
    }


    /**
     * Determines whether two Fusion values have the same annotations in the
     * same order.
     */
    static boolean sameAnnotations(Evaluator eval, Object left, Object right)
        throws FusionException
    {
        Object[] thisAnn = annotations(eval, left);
        Object[] thatAnn = annotations(eval, right);

        int len = thisAnn.length;
        if (thatAnn.length != len) return false;

        for (int i = 0; i < len; i++)
        {
            // Annotations are interned symbols.
            if (thisAnn[i] != thatAnn[i]) return false;
        }
        return true;
    }


    /**
     * Gets the annotations on a Fusion value as Java strings.
     *
     * @return not null, but possibly empty.
     */
    static String[] annotationsAsJavaStrings(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof BaseValue)
        {
            return ((BaseValue) value).getAnnotationsAsJavaStrings();
        }
        return EMPTY_STRING_ARRAY;
    }

    /**
     * Determines whether a Fusion value has a specific annotation.
     *
     * @param eval must not be null.
     * @param value is a Fusion value.
     * @param annotation must not be null.
     *
     * @return whether the {@code value} has the {@code annotation}.
     */
    static boolean hasAnnotation(Evaluator eval, Object value, String annotation)
        throws FusionException
    {
        if (value instanceof BaseValue)
        {
            for (BaseSymbol a : ((BaseValue) value).getAnnotations())
            {
                if (a.stringValue().equals(annotation)) return true;
            }
        }

        return false;
    }


    //========================================================================
    // Output methods




    //========================================================================
    // Static IonValue methods


    /**
     * Returns a new {@link IonValue} representation of a Fusion value,
     * if its type falls within the Ion type system.
     * The {@link IonValue} will use the given factory and will not have a
     * container.
     *
     * @param factory must not be null.
     *
     * @return a fresh instance, without a container, or null if the value is
     * not handled by the default ionization strategy.
     *
     * @throws FusionException if something goes wrong during ionization.
     *
     * @see FusionRuntime#ionizeMaybe(Object, ValueFactory)
     */
    static IonValue copyToIonValueMaybe(Object value, ValueFactory factory)
        throws FusionException
    {
        return copyToIonValue(value, factory, false);
    }


    /**
     * Returns a new {@link IonValue} representation of a Fusion value,
     * if its type falls within the Ion type system.
     * The {@link IonValue} will use the given factory and will not have a
     * container.
     *
     * @param factory must not be null.
     *
     * @throws FusionException if the value cannot be converted to Ion.
     */
    static IonValue copyToIonValue(Object value, ValueFactory factory)
        throws FusionException
    {
        return copyToIonValue(value, factory, true);
    }


    /**
     * Returns a new {@link IonValue} representation of a Fusion value,
     * if its type falls within the Ion type system.
     * The {@link IonValue} will use the given factory and will not have a
     * container.
     *
     * @param value may be an {@link IonValue}, in which case it is cloned.
     * @param factory must not be null.
     *
     * @throws FusionException if the value cannot be converted to Ion.
     *
     * @see FusionRuntime#ionize(Object, ValueFactory)
     */
    static IonValue copyToIonValue(Object       value,
                                   ValueFactory factory,
                                   boolean      throwOnConversionFailure)
        throws FusionException, IonizeFailure
    {
        if (value instanceof BaseValue)
        {
            BaseValue fv = (BaseValue) value;
            return fv.copyToIonValue(factory, throwOnConversionFailure);
        }

        if (throwOnConversionFailure)
        {
            throw new IonizeFailure(value);
        }

        return null;
    }
}
