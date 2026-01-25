// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionBool.trueBool;
import static dev.ionfusion.fusion.FusionIo.safeWriteToString;
import static dev.ionfusion.fusion.FusionValue.sameAnnotations;

import com.amazon.ion.IonException;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import com.amazon.ion.system.IonTextWriterBuilder;
import com.amazon.ion.util.IonTextUtils;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.embed.FusionRuntime;
import java.io.IOException;

/**
 * Root class for most (if not all) Fusion values.
 * <p>
 * This class, and all subclasses, are <b>not for application use.</b>
 * Any aspect of this class hierarchy can and will change without notice!
 * <p>
 * The following capabilities are aggregated here:
 * <ul>
 *     <li>Checks for nullness, truthiness</li>
 *     <li>Equality</li>
 *     <li>Syntax Object construction</li>
 *     <li>Serialization (ionize/write/display)</li>
 * </ul>
 * Some of these should probably be pushed down into separate interfaces.
 */
abstract class BaseValue
{
    BaseValue() {}


    boolean isAnyNull()
    {
        return false;
    }


    boolean isTruthy(Evaluator eval)
    {
        return ! isAnyNull();
    }


    BaseBool looseEquals(Evaluator eval, Object right)
        throws FusionException
    {
        return falseBool(eval);
    }

    BaseBool tightEquals(Evaluator eval, Object right)
        throws FusionException
    {
        return looseEquals(eval, right);
    }

    /**
     * Implementation of {@code ===}, <em>without</em> checking annotations.
     */
    BaseBool strictEquals(Evaluator eval, Object right)
        throws FusionException
    {
        return tightEquals(eval, right);
    }


    SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
    {
        return SimpleSyntaxValue.makeOriginalSyntax(eval, loc, this);
    }


    /**
     * Contained {@link SyntaxValue}s must be left unchanged, so the context
     * is pushed eagerly.
     * <p>
     * TODO This needs to do cycle detection.
     *
     * @return null if something in this datum can't be converted into syntax.
     *
     * @see <a href="https://github.com/ion-fusion/fusion-java/issues/65">#65</a>
     */
    SyntaxValue datumToSyntaxMaybe(Evaluator      eval,
                                   SyntaxSymbol   context,
                                   SourceLocation loc)
        throws FusionException
    {
        SyntaxValue stx = datumToSyntaxMaybe(eval, loc);
        if (stx == null) return null;

        return Syntax.applyContext(eval, context, stx);
    }

    /**
     * @return null if something in the datum can't be converted into syntax.
     */
    SyntaxValue datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
        throws FusionException
    {
        return null;
    }


    /**
     * Writes an Ion representation of a value.
     * An exception is thrown if the value contains any non-Ion data
     * like closures.
     *
     * @param eval may be null, in which case output may fall back to default
     * format of some kind.
     * @param out the output stream; not null.
     *
     * @throws IOException Propagated from the output stream.
     * @throws IonizeFailure if the data cannot be ionized.
     */
    void ionize(Evaluator eval, IonWriter out)
        throws IOException, IonException, FusionException, IonizeFailure
    {
        throw new IonizeFailure(this);
    }


    /**
     * Writes a representation of this value, following Ion syntax where
     * possible.
     * <p>
     * Most code shouldn't call this method, and should prefer
     * {@link FusionIo#write(Evaluator, Appendable, Object)}.
     *
     * @param eval may be null!
     * @param out the output stream; not null.
     *
     * @throws IOException Propagated from the output stream.
     * @throws FusionException
     */
    abstract void write(Evaluator eval, Appendable out)
        throws IOException, FusionException;


    /**
     * Builder for temporary IonWriters needed for {@link #write}ing
     * lazily injected lists and structs.
     *
     * @see <a href="https://github.com/ion-fusion/fusion-java/issues/96">#96</a>
     *
     * @deprecated Try to avoid this.
     */
    @Deprecated
    static final IonTextWriterBuilder WRITER_BUILDER =
        IonTextWriterBuilder.minimal().immutable();


    /** Helper method for subclasses. */
    static void writeAnnotations(Appendable out, BaseSymbol[] annotations)
        throws IOException
    {
        for (BaseSymbol ann : annotations)
        {
            IonTextUtils.printSymbol(out, ann.stringValue());
            out.append("::");
        }
    }


    /**
     * Prints a representation of this value for human consumption, generally
     * translating character/string data to it's content without using Ion
     * quotes or escapes. Non-character data is output as per
     * {@link #write(Evaluator, Appendable)}.
     *
     * @param out the output stream; not null.
     *
     * @throws IOException Propagated from the output stream.
     */
    void display(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        write(eval, out);
    }


    /**
     * Returns a representation of this value for debugging and diagnostics.
     * Currently, it behaves like {@link FusionIo#write} but the behavior may
     * change at any time.
     */
    @Override
    public final String toString()
    {
        return safeWriteToString((Evaluator) null, this);
    }


    /**
     * @throws IonizeFailure (when {@code throwOnConversionFailure})
     * if this value cannot be ionized.
     */
    IonValue copyToIonValue(ValueFactory factory,
                            boolean throwOnConversionFailure)
        throws FusionException, IonizeFailure
    {
        if (throwOnConversionFailure)
        {
            throw new IonizeFailure(this);
        }

        return null;
    }


    //========================================================================
    // Helper methods to work around name conflicts between methods on this
    // class and static imports from FusionValue (for example).
    // The problem is that virtual methods hide same-named static imports.


    static BaseBool isAnyNull(Evaluator eval, Object value)
        throws FusionException
    {
        boolean r = FusionValue.isAnyNull(eval, value);
        return makeBool(eval, r);
    }


    static BaseBool looseEquals(Evaluator eval, Object left, Object right)
        throws FusionException
    {
        if (left == right) return trueBool(eval);

        if (left instanceof BaseValue)
        {
            return ((BaseValue) left).looseEquals(eval, right);
        }

        return falseBool(eval);
    }

    static BaseBool tightEquals(Evaluator eval, Object left, Object right)
        throws FusionException
    {
        if (left == right) return trueBool(eval);

        if (left instanceof BaseValue)
        {
            return ((BaseValue) left).tightEquals(eval, right);
        }

        return falseBool(eval);
    }

    static BaseBool strictEquals(Evaluator eval, Object left, Object right)
        throws FusionException
    {
        if (left == right) return trueBool(eval);

        if (left instanceof BaseValue)
        {
            BaseValue lv = (BaseValue) left;
            // TODO check annotations first, to fail faster.
            BaseBool b = lv.strictEquals(eval, right);
            if (b.isTrue())
            {
                boolean result = sameAnnotations(eval, left, right);
                return makeBool(eval, result);
            }
        }

        return falseBool(eval);
    }


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
     * @see FusionValue#copyToIonValueMaybe(Object, ValueFactory)
     */
    static IonValue copyToIonValueMaybe(Object value, ValueFactory factory)
        throws FusionException
    {
        return FusionValue.copyToIonValue(value, factory, false);
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
     *
     * @see FusionValue#copyToIonValue(Object, ValueFactory)
     */
    static IonValue copyToIonValue(Object value, ValueFactory factory)
        throws FusionException
    {
        return FusionValue.copyToIonValue(value, factory, true);
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
     * @see FusionValue#copyToIonValue(Object, ValueFactory, boolean)
     */
    static IonValue copyToIonValue(Object value, ValueFactory factory,
                                   boolean throwOnConversionFailure)
        throws FusionException, IonizeFailure
    {
        return FusionValue.copyToIonValue(value,
                                          factory,
                                          throwOnConversionFailure);
    }
}
