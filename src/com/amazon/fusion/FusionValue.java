// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionSexp.isNullSexp;
import static com.amazon.fusion.FusionSexp.isSexp;
import static com.amazon.fusion.FusionVector.isNullVector;
import static com.amazon.fusion.FusionVector.isVector;
import static com.amazon.fusion.FusionVoid.isVoid;
import com.amazon.ion.IonBool;
import com.amazon.ion.IonContainer;
import com.amazon.ion.IonInt;
import com.amazon.ion.IonString;
import com.amazon.ion.IonText;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import com.amazon.ion.util.IonTextUtils;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

/**
 * The core features of a Fusion run-time value.  Note that the set of Fusion
 * values is a superset of the Ion values, so not all {@link FusionValue}s are
 * Ion data values.
 */
public abstract class FusionValue
{
    private static final class Undef
        extends FusionValue
    {
        @Override
        void write(Appendable out) throws IOException
        {
            out.append("/* undef */");
        }
    }


    /** The singular {@code undef} value. */
    public final static FusionValue UNDEF = new Undef();


    //========================================================================

    /** Not for application use. */
    FusionValue()
    {
    }


    String getInferredName()
    {
        return null;
    }

    void inferName(String name)
    {
    }


    static boolean isNullNull(Evaluator eval, Object value)
    {
        IonValue iv = castToIonValueMaybe(value);
        return (iv != null && iv.getType() == IonType.NULL);
    }


    static boolean isTruthy(Evaluator eval, Object result)
        throws FusionException
    {
        if (isVoid(eval, result)) return false;

        if (isNullVector(eval, result)) return false;

        if (isNullSexp(eval, result)) return false;

        IonValue iv = FusionValue.castToIonValueMaybe(result);
        if (iv == null) return true;

        if (iv.isNullValue()) return false;

        if (iv instanceof IonBool)
        {
            IonBool bv = (IonBool) iv;
            return bv.booleanValue();
        }

        return true;
    }


    //========================================================================

    /** Helper method for subclasses. */
    void writeAnnotations(Appendable out, String[] annotations)
        throws IOException
    {
        for (String ann : annotations)
        {
            IonTextUtils.printSymbol(out, ann);
            out.append("::");
        }
    }


    /**
     * Writes a representation of this value, following Ion syntax where
     * possible, including for strings.
     * The result will be invalid if the value contains any non-Ion data like
     * closures.
     *
     * @param out the output stream; not null.
     *
     * @throws IOException Propagated from the output stream.
     */
    abstract void write(Appendable out)
        throws IOException;


    /**
     * Returns the output of {@link #write(Appendable)} as a {@link String}.
     *
     * @return not null.
     */
    String write()
    {
        StringBuilder out = new StringBuilder();
        try
        {
            write(out);
        }
        catch (IOException e) {}
        return out.toString();
    }


    /**
     * Returns a representation of this value for debugging and diagnostics.
     * Currently, it behaves like {@link #write()} but the behavior may change
     * at any time.
     */
    @Override
    public final String toString()
    {
        return write();
    }


    /**
     * Prints a representation of this value for human consumption, generally
     * translating character/string data to it's content without using Ion
     * quotes or escapes. Non-character data is output as per
     * {@link #write(Appendable)}.
     *
     * @param out the output stream; not null.
     *
     * @throws IOException Propagated from the output stream.
     */
    void display(Appendable out)
        throws IOException
    {
        write(out);
    }


    /**
     * Returns the output of {@link #display(Appendable)} as a {@link String}
     */
    final String display()
    {
        StringWriter out = new StringWriter();
        try
        {
            display(out);
        }
        catch (IOException e) {}
        return out.toString();
    }


    /**
     * Returns the documentation of this value.
     *
     * @return the documentation model, or null if there's no documentation.
     */
    BindingDoc document()
    {
        return null;
    }


    //========================================================================
    // Static write methods


    static void dispatchWrite(IonWriter out, Object value)
        throws IOException, FusionException
    {
        if (value instanceof Writeable)
        {
            ((Writeable) value).write(out);
        }
        else if (value instanceof IonValue)
        {
            ((IonValue)value).writeTo(out);
        }
        else
        {
            throw new ContractFailure("Cannot write non-Ion data " + value);
        }
    }


    static void write(IonWriter out, Object value)
        throws FusionException
    {
        try
        {
            dispatchWrite(out, value);
        }
        catch (IOException e)
        {
            throw new FusionException("I/O exception", e);
        }
    }


    static void dispatchWrite(Appendable out, Object value)
        throws IOException
    {
        if (value instanceof FusionValue)
        {
            ((FusionValue) value).write(out);
        }
        else if (value instanceof IonValue)
        {
            FusionUtils.writeIon(out, (IonValue) value);
        }
        else
        {
            out.append("/* ");
            out.append(value.toString());
            out.append(" */");
        }
    }


    /**
     * Writes a representation of a value, following Ion syntax where
     * possible, including for strings.
     * The result will be invalid if the value contains any non-Ion data like
     * closures.
     *
     * @param out the output stream; not null.
     * @param value must not be null.
     *
     * @throws FusionException if there's an exception thrown by the output
     * stream.
     */
    public static void write(Appendable out, Object value)
        throws FusionException
    {
        try
        {
            dispatchWrite(out, value);
        }
        catch (IOException e)
        {
            throw new FusionException("I/O exception", e);
        }
    }


    /**
     * Writes a representation of a value, following Ion syntax where
     * possible, including for strings.
     * The result will be invalid if the value contains any non-Ion data like
     * closures.
     *
     * @param out the output buffer; not null.
     */
    public static void write(StringBuilder out, Object value)
    {
        try
        {
            dispatchWrite(out, value);
        }
        catch (IOException e)
        {
            // This shouldn't happen
            throw new IllegalStateException("I/O exception", e);
        }
    }


    // TODO FUSION-52 (write val) that goes to current_output_stream


    /**
     * Returns the output of {@link #write(StringBuilder,Object)} as a
     * {@link String}.
     *
     * @return not null.
     */
    public static String writeToString(Object value)
    {
        StringBuilder out = new StringBuilder();
        write(out, value);
        return out.toString();
    }


    /**
     * {@linkplain #write(Appendable, Object) Writes} several values,
     * injecting a string between each pair of values.
     *
     * @param out must not be null.
     * @param values must not be null.
     * @param join must not be null.
     */
    public static void writeMany(Appendable out, Object[] values, String join)
        throws FusionException
    {
        try
        {
            for (int i = 0; i < values.length; i++)
            {
                if (i != 0)
                {
                    out.append(join);
                }

                write(out, values[i]);
            }
        }
        catch (IOException e)
        {
            throw new FusionException("I/O exception", e);
        }
    }


    /**
     * {@linkplain #write(StringBuilder, Object) Writes} several values,
     * injecting a string between each pair of values.
     *
     * @param out must not be null.
     * @param values must not be null.
     * @param join must not be null.
     */
    public static void writeMany(StringBuilder out, Object[] values,
                                 String join)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (i != 0)
            {
                out.append(join);
            }

            write(out, values[i]);
        }
    }


    /**
     * Returns the output of {@link #writeMany(StringBuilder,Object[],String)}
     * as a {@link String}.
     *
     * @return not null.
     */
    public static String writeManyToString(Object[] values, String join)
    {
        StringBuilder out = new StringBuilder();
        writeMany(out, values, join);
        return out.toString();
    }


    //========================================================================
    // Static display methods


    private static void dispatchDisplay(Appendable out, Object value)
        throws IOException
    {
        if (value instanceof FusionValue)
        {
            ((FusionValue) value).display(out);
        }
        else if (value instanceof IonValue)
        {
            IonValue iv = (IonValue) value;
            if (iv instanceof IonText)
            {
                String text = ((IonText) iv).stringValue();
                out.append(text);
            }
            else
            {
                FusionUtils.writeIon(out, iv);
            }
        }
        else if (value instanceof SyntaxValue)
        {
            ((SyntaxValue) value).write(out);
        }
        else
        {
            out.append("/* ");
            out.append(value.toString());
            out.append(" */");
        }
    }


    public static void display(Appendable out, Object value)
        throws FusionException
    {
        try
        {
            dispatchDisplay(out, value);
        }
        catch (IOException e)
        {
            throw new FusionException("I/O exception", e);
        }
    }


    public static void display(StringBuilder out, Object value)
    {
        try
        {
            dispatchDisplay(out, value);
        }
        catch (IOException e)
        {
            // This shouldn't happen
            throw new IllegalStateException("I/O exception", e);
        }
    }


    /**
     * Returns the output of {@link #write(StringBuilder,Object)} as a
     * {@link String}.
     *
     * @return not null.
     */
    public static String displayToString(Object value)
    {
        StringBuilder out = new StringBuilder();
        display(out, value);
        return out.toString();
    }



    /**
     * {@linkplain #display(Appendable, Object) Displays} several values,
     * injecting a string between each pair of values.
     *
     * @param out must not be null.
     * @param values must not be null.
     * @param join must not be null.
     */
    public static void displayMany(Appendable out, Object[] values, String join)
        throws FusionException
    {
        try
        {
            for (int i = 0; i < values.length; i++)
            {
                if (i != 0)
                {
                    out.append(join);
                }

                display(out, values[i]);
            }
        }
        catch (IOException e)
        {
            throw new FusionException("I/O exception", e);
        }
    }


    /**
     * {@linkplain #display(StringBuilder, Object) Displays} several values,
     * injecting a string between each pair of values.
     *
     * @param out must not be null.
     * @param values must not be null.
     * @param join must not be null.
     */
    public static void displayMany(StringBuilder out, Object[] values, String join)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (i != 0)
            {
                out.append(join);
            }

            display(out, values[i]);
        }
    }

    public static void displayMany(StringBuilder out, Object[] values, int first)
    {
        for (int i = first; i < values.length; i++)
        {
            display(out, values[i]);
        }
    }


    public static String displayManyToString(Object[] values, String join)
    {
        StringBuilder out = new StringBuilder();
        displayMany(out, values, join);
        return out.toString();
    }

    public static String displayManyToString(Object[] values, int first)
    {
        StringBuilder out = new StringBuilder();
        displayMany(out, values, first);
        return out.toString();
    }


    //========================================================================
    // Static IonValue methods


    /**
     * @param value must not be null
     * @return not null.
     */
    static IonValue unsafeCastToIonValue(Object value)
    {
        return (IonValue) value;
    }


    /**
     * Performs an immediate cast (not conversion) of the given Fusion value
     * to an IonValue. The result may have a container!
     * <p>
     * This isn't public because I'm not convinced that the runtime should have
     * IonValues at all.
     *
     * @return null if the value's type isn't an Ion type.
     */
    static IonValue castToIonValueMaybe(Object value)
    {
        if (value instanceof IonValue)
        {
            return (IonValue) value;
        }

        return null;
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
     *
     * @deprecated Use {@link FusionRuntime#ionize(Object, ValueFactory)}
     */
    @Deprecated // for public access
    public static IonValue copyToIonValue(Object value, ValueFactory factory)
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
     * @param factory must not be null.
     *
     * @throws FusionException if the value cannot be converted to Ion.
     *
     * @see FusionRuntime#ionize(Object, ValueFactory)
     */
    static IonValue copyToIonValue(Object value, ValueFactory factory,
                                   boolean throwOnConversionFailure)
        throws FusionException
    {
        if (value instanceof IonValue)
        {
            IonValue iv = (IonValue) value;
            return factory.clone(iv);
        }

        if (isVector(value))
        {
            return FusionVector.unsafeCopyToIonList(value, factory,
                                                    throwOnConversionFailure);
        }

        if (isSexp(value))
        {
            return FusionSexp.unsafeCopyToIonSexp(value, factory,
                                                  throwOnConversionFailure);
        }

        if (throwOnConversionFailure)
        {
            String message =
                "Value is not convertable to Ion: " + writeToString(value);
            throw new ContractFailure(message);
        }

        return null;
    }


    /**
     * FOR INTERNAL USE ONLY!
     *
     * @param dom must not be null.
     * @deprecated DO NOT USE! Use {@link Evaluator#inject(IonValue)} instead
     */
    @Deprecated
    static Object forIonValue(IonValue dom)
    {
        dom.getClass();  // Forces a null check
        return dom;
    }


    static boolean isAnyIonNull(Object value)
    {
        IonValue iv = castToIonValueMaybe(value);
        return (iv != null ? iv.isNullValue() : false);
    }

    /**
     * Returns a Fusion string as a Java string.
     * @return null if the value isn't a string.
     */
    static String asJavaString(Object value)
    {
        IonValue iv = castToIonValueMaybe(value);
        if (iv != null && iv.getType() == IonType.STRING)
        {
            return ((IonString) iv).stringValue();
        }
        return null;
    }

    static Long asJavaLong(Object value)
    {
        IonValue iv = castToIonValueMaybe(value);
        if (iv != null && iv.getType() == IonType.INT)
        {
            return ((IonInt) iv).longValue();
        }
        return null;
    }


    /**
     * Returns a Fusion bool as a Java Boolean.
     * @return null if the value isn't true or false.
     */
    static Boolean asBoolean(Object value)
    {
        IonValue iv = castToIonValueMaybe(value);
        if (iv != null && iv.getType() == IonType.BOOL && ! iv.isNullValue())
        {
            return ((IonBool) iv).booleanValue();
        }
        return null;
    }


    /**
     * @param value must be vector or IonContainer
     */
    static Iterator<?> unsafeJavaIterate(Evaluator eval, Object value)
    {
        if (isVector(value))
        {
            return FusionVector.unsafeJavaIterate(eval, value);
        }
        return ((IonContainer)value).iterator();
    }
}
