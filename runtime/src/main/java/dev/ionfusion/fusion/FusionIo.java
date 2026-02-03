// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionLob.isLob;
import static dev.ionfusion.fusion.FusionLob.unsafeLobBytesNoCopy;
import static dev.ionfusion.fusion.FusionString.checkNonEmptyStringArg;
import static dev.ionfusion.fusion.FusionString.checkRequiredStringArg;
import static dev.ionfusion.fusion.FusionString.makeString;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.amazon.ion.IonException;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonTextWriterBuilder;
import dev.ionfusion.runtime.embed.TopLevel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * Utilities for input and output of Fusion data.
 *
 * <h2>EOF</h2>
 * Fusion defines a unique {@code eof} value for denoting the end of an input
 * stream. This value can be detected via {@link #isEof(TopLevel, Object)}.
 */
public final class FusionIo
{
    private FusionIo() {}


    /** The singular {@code eof} value. */
    private final static BaseValue EOF =
        new BaseValue()
        {
            @Override
            void write(Evaluator eval, Appendable out) throws IOException
            {
                out.append("{{{eof}}}");
            }
        };


    static Object eof(Evaluator eval)
    {
        return EOF;
    }


    static boolean isEof(Evaluator eval, Object v)
    {
        assert eval != null;
        return (v == EOF);
    }

    /**
     * Determines whether a value is Fusion's unique EOF object.
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value the value to test
     *
     * @return {@code true} if the value is the Fusion EOF sentinel,
     * otherwise {@code false}
     */
    public static boolean isEof(TopLevel top, Object value)
    {
        return (value == EOF);
    }


    static final class IsEofProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
        {
            return makeBool(eval, arg == EOF);
        }
    }


    //========================================================================
    // "Internal" dispatching procedures that throw exceptions


    static void dispatchIonize(Evaluator eval, IonWriter out, Object value)
        throws IOException, FusionException, IonException
    {
        // Optimized to avoid instanceof, we are going to fail anyway and that
        // doesn't need to be fast.
        BaseValue fv;
        try
        {
            fv = (BaseValue) value;
        }
        catch (ClassCastException e)
        {
            throw makeIonizeError(eval, value);
        }

        fv.ionize(eval, out);
    }


    /**
     *
     * @param eval may be null!
     * @param value may be an {@link IonValue}.
     */
    static void dispatchWrite(Evaluator eval, Appendable out, Object value)
        throws IOException, FusionException
    {
        if (value instanceof BaseValue)
        {
            ((BaseValue) value).write(eval, out);
        }
        else
        {
            out.append(value.getClass().getName());
            out.append("::{{{");
            out.append(value.toString());
            out.append("}}}");
        }
    }


    static void dispatchDisplay(Evaluator eval, Appendable out, Object value)
        throws IOException, FusionException
    {
        if (value instanceof BaseValue)
        {
            ((BaseValue) value).display(eval, out);
        }
        else
        {
            out.append(value.getClass().getName());
            out.append("::{{{");
            out.append(value.toString());
            out.append("}}}");
        }
    }


    //========================================================================
    // Basic input procedures


    /**
     * Reads a single Fusion value from an Ion stream.
     * If the reader is positioned on a value, that value is read and returned.
     * Otherwise, the reader's {@link IonReader#next()} method is called;
     * if there's not another value then the result is {@code eof}.
     * <p>
     * After consuming the value, the reader is moved to the next
     * value by calling {@link IonReader#next()}.
     *
     * @param top the {@link TopLevel} to use for evaluation
     * @param reader must be positioned on the value to read.
     *
     * @return an immutable Fusion value.
     *
     * @throws FusionException if an error occurs during evaluation
     *
     * @see #isEof(TopLevel, Object)
     */
    public static Object read(TopLevel top, IonReader reader)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return read(eval, reader);
    }

    static Object read(Evaluator eval, IonReader reader)
        throws FusionException
    {
        try
        {
            if (reader.getType() == null)
            {
                if (reader.next() == null)
                {
                    return eof(eval);
                }
            }

            Object fv = StandardReader.read(eval, reader);
            reader.next();
            return fv;
        }
        catch (IonException e)
        {
            throw new FusionException("Error reading data: " + e.getMessage(),
                                      e);
        }
    }


    //========================================================================
    // Basic output procedures


    static ContractException makeIonizeError(Evaluator eval, Object unIonizableValue)
    {
        String message = "Cannot ionize non-Ionizable data: " +
                         safeWriteToString(eval, unIonizableValue);
        return new ContractException(message);
    }


    /**
     * Writes an Ion representation of a value.
     * An exception is thrown if the value contains any non-Ion data
     * like closures.
     *
     * @param eval may be null, in which case output may fall back to default
     * format of some kind.
     * @param out the output stream; not null.
     * @param value must not be null.
     *
     * @throws FusionException if some part of the value cannot be ionized, or
     * if there's an exception thrown by the output stream.
     */
    static void ionize(Evaluator eval, IonWriter out, Object value)
        throws FusionException
    {
        try
        {
            dispatchIonize(eval, out, value);
        }
        catch (IOException e)
        {
            throw new FusionException("I/O exception", e);
        }
        catch (IonException e)
        {
            throw new FusionException(e);
        }
    }


    /**
     * <a href="{@docRoot}/../fusion/io.html#ionize">Ionizes</a> a Fusion
     * value, throwing an exception when any part of the value is outside the
     * Ion type system.
     *
     * @param top the {@link TopLevel} to use for evaluation
     * @param value the value to write; must not be null.
     * @param out the output stream; not null.
     *
     * @throws FusionException if some part of the value cannot be ionized,
     * or if there's an exception thrown by the output stream.
     */
    public static void ionize(TopLevel top, Object value, IonWriter out)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        ionize(eval, out, value);
    }


    /**
     * <a href="{@docRoot}/../fusion/io.html#write">Writes</a> a text
     * representation of a Fusion value, following Ion syntax
     * where possible, including for strings.
     * The result will be unreadable (by the Fusion and Ion readers) if the
     * value contains any non-Ionizable data (void, closures, etc.).
     *
     * @param top the {@link TopLevel} to use for evaluation
     * @param value the value to write; must not be null.
     * @param out the output stream; not null.
     *
     * @throws FusionException if there's an exception thrown by the output
     * stream.
     */
    public static void write(TopLevel top, Object value, Appendable out)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        write(eval, out, value);
    }


    /**
     * Writes a text representation of a value, following Ion syntax where
     * possible, including for strings.
     * The result will be unreadable (by the Fusion and Ion readers) if the
     * value contains any non-Ionizable data like closures.
     *
     * @param eval may be null, in which case output may fall back to default
     * format of some kind.
     * @param out the output stream; not null.
     * @param value must not be null.
     *
     * @throws FusionException if there's an exception thrown by the output
     * stream.
     */
    static void write(Evaluator eval, Appendable out, Object value)
        throws FusionException
    {
        try
        {
            dispatchWrite(eval, out, value);
        }
        catch (IOException e)
        {
            throw new FusionException("I/O exception", e);
        }
    }


    static void display(Evaluator eval, Appendable out, Object value)
        throws FusionException
    {
        try
        {
            dispatchDisplay(eval, out, value);
        }
        catch (IOException e)
        {
            throw new FusionException("I/O exception", e);
        }
    }


    //========================================================================


    /**
     * Returns the output of {@link #write} as a
     * {@link String}.
     *
     * @return not null.
     */
    static String writeToString(Evaluator eval, Object value)
        throws FusionException
    {
        StringBuilder out = new StringBuilder();
        write(eval, out, value);
        return out.toString();
    }


    /**
     * Returns the output of {@link #write(TopLevel, Object, Appendable)}
     * as a {@link String}.
     *
     * @param top the {@link TopLevel} to use for evaluation
     * @param value the value to write; must not be null.
     *
     * @return not null.
     *
     * @throws FusionException if an error occurs during evaluation
     *
     * @see #safeWriteToString(TopLevel, Object)
     */
    public static String writeToString(TopLevel top, Object value)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return writeToString(eval, value);
    }


    //========================================================================


    /**
     * {@linkplain #write Writes}
     * several values, injecting a string between each pair of values.
     *
     * @param out must not be null.
     * @param values must not be null.
     * @param join must not be null.
     */
    static void writeMany(Evaluator eval, Appendable out,
                          Object[] values, String join)
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

                dispatchWrite(eval, out, values[i]);
            }
        }
        catch (IOException e)
        {
            throw new FusionException("I/O exception", e);
        }
    }


    /**
     * {@linkplain #display Displays}
     * several values, injecting a string between each pair of values.
     *
     * @param out must not be null.
     * @param values must not be null.
     * @param join must not be null.
     */
    static void displayMany(Evaluator eval, Appendable out,
                            Object[] values, String join)
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

                dispatchDisplay(eval, out, values[i]);
            }
        }
        catch (IOException e)
        {
            throw new FusionException("I/O exception", e);
        }
    }

    static void displayMany(Evaluator eval,
                            Appendable out,
                            Object[] values,
                            int first)
        throws FusionException
    {
        for (int i = first; i < values.length; i++)
        {
            display(eval, out, values[i]);
        }
    }


    //========================================================================

    /**
     * Returns the output of {@link #writeMany} as a {@link String}.
     *
     * @return not null.
     */
    static String writeManyToString(Evaluator eval, Object[] values,
                                    String join)
        throws FusionException
    {
        StringBuilder out = new StringBuilder();
        writeMany(eval, out, values, join);
        return out.toString();
    }


    /**
     * Returns the output of {@link #displayMany} as a {@link String}.
     *
     * @return not null.
     */
    static String displayManyToString(Evaluator eval, Object[] values,
                                      String join)
        throws FusionException
    {
        StringBuilder out = new StringBuilder();
        displayMany(eval, out, values, join);
        return out.toString();
    }


    static String displayManyToString(Evaluator eval, Object[] values,
                                      int first)
        throws FusionException
    {
        StringBuilder out = new StringBuilder();
        displayMany(eval, out, values, first);
        return out.toString();
    }


    //========================================================================
    // "Safe" (no-throw) variants


    static void safeWrite(Evaluator eval, Appendable out, Object value)
    {
        try
        {
            write(eval, out, value);
        }
        catch (Exception e)
        {
            displayFailure(out, e);
        }
    }

    /**
     * Like {@link #writeMany} but catches any exceptions.
     */
    static void safeWriteMany(Evaluator eval, Appendable out, Object[] values, String join)
    {
        try
        {
            writeMany(eval, out, values, join);
        }
        catch (Exception e)
        {
            displayFailure(out, e);
        }
    }


    static void displayFailure(Appendable out, Exception e)
    {
        try
        {
            out.append("{{{ FAILURE: ");
            String message = e.getMessage(); // TODO Don't include Fusion stack trace
            if (message == null)
            {
                message = e.getClass().getName();
            }
            out.append(message);
            out.append(" }}}");
        }
        catch (IOException ioe)
        {
            // Give up!
        }
    }


    static void safeDisplay(Evaluator eval, Appendable out, Object value)
    {
        try
        {
            display(eval, out, value);
        }
        catch (Exception e)
        {
            displayFailure(out, e);
        }
    }


    /**
     * Returns the output of {@link #write(TopLevel, Object, Appendable)} as a
     * {@link String}, handling any {@link Exception}s by writing their
     * message into the output.
     *
     * @return not null.
     */
    static String safeWriteToString(Evaluator eval, Object value)
    {
        StringBuilder out = new StringBuilder();
        safeWrite(eval, out, value);
        return out.toString();
    }


    /**
     * Returns the output of {@link #write(TopLevel, Object, Appendable)} as a
     * {@link String}, handling any {@link Exception}s by writing their
     * message into the output.
     *
     * @param top the {@link TopLevel} to use for evaluation
     * @param value the value to write; must not be null.
     *
     * @return not null.
     *
     * @see #writeToString(TopLevel, Object)
     */
    public static String safeWriteToString(TopLevel top, Object value)
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return safeWriteToString(eval, value);
    }


    static String safeDisplayManyToString(Evaluator eval, Object[] values,
                                          int first)
    {
        StringBuilder out = new StringBuilder();
        try
        {
            displayMany(eval, out, values, first);
        }
        catch (Exception e)
        {
            displayFailure(out, e);
        }
        return out.toString();
    }


    //========================================================================


    static final class ReadProc
        extends Procedure0
    {
        private final DynamicParameter myCurrentIonReaderParam;

        public ReadProc(Object currentIonReaderParam)
        {
            myCurrentIonReaderParam = (DynamicParameter) currentIonReaderParam;
        }

        @Override
        Object doApply(Evaluator eval)
            throws FusionException
        {
            IonReader r = myCurrentIonReaderParam.currentValue(eval);

            return FusionIo.read(eval, r);
        }
    }


    static final class IonizeProc
        extends Procedure1
    {
        private final DynamicParameter     myCurrentOutputPort;
        private final IonTextWriterBuilder myBuilder;

        IonizeProc(Object currentOutputPort)
        {
            myCurrentOutputPort = (DynamicParameter) currentOutputPort;
            myBuilder = IonTextWriterBuilder.pretty().immutable();
        }

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            OutputStream port = myCurrentOutputPort.currentValue(eval);

            // Be careful not to close the output stream.
            IonWriter writer = myBuilder.build(port);

            FusionIo.ionize(eval, writer, arg);

            try
            {
                writer.flush();
            }
            catch (IOException e)
            {
                throw new FusionException("I/O Exception", e);
            }

            return voidValue(eval);
        }
    }


    static final class IonizeToBlobProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (IonWriter writer = eval.getSystem().newBinaryWriter(buffer))
            {
                FusionIo.ionize(eval, writer, arg);
                writer.finish();
                byte[] bytes = buffer.toByteArray();

                return FusionBlob.forBytesNoCopy(eval, bytes);
            }
            catch (IOException e)
            {
                throw new FusionException("I/O Exception", e);
            }
        }
    }


    static class MakeOutputBufferProc
        extends Procedure0
    {
        @Override
        Object doApply(Evaluator eval)
            throws FusionException
        {
            return new ByteArrayOutputStream(1024);
        }
    }

    static class OutputBufferToStringProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            try
            {
                String jString = ((ByteArrayOutputStream) arg).toString(UTF_8.name());
                return makeString(eval, jString);
            }
            catch (UnsupportedEncodingException e)
            {
                throw new FusionErrorException("JRE doesn't have UTF-8?", e);
            }
        }
    }

    static class IonizeToStringProc
        extends Procedure1
    {
        private final IonTextWriterBuilder myBuilder;

        IonizeToStringProc()
        {
            this(IonTextWriterBuilder.minimal());
        }

        IonizeToStringProc(IonTextWriterBuilder builder)
        {
            myBuilder = builder.immutable();
        }

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            StringBuilder buf = new StringBuilder(512);

            try (IonWriter writer = myBuilder.build(buf))
            {
                FusionIo.ionize(eval, writer, arg);
            }
            catch (IOException e)
            {
                throw new FusionException("I/O Exception", e);
            }

            return makeString(eval, buf.toString());
        }
    }

    static final class JsonizeToStringProc
        extends IonizeToStringProc
    {
        JsonizeToStringProc()
        {
            super(IonTextWriterBuilder.json());
        }
    }

    static final class WriteProc
        extends Procedure1
    {
        private final DynamicParameter myCurrentOutputPort;

        WriteProc(Object currentOutputPort)
        {
            myCurrentOutputPort = (DynamicParameter) currentOutputPort;
        }

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            try
            {
                OutputStream port = myCurrentOutputPort.currentValue(eval);

                // Be careful not to close the output stream.
                OutputStreamWriter out = new OutputStreamWriter(port, UTF_8);
                try
                {
                    FusionIo.write(eval, out, arg);
                }
                finally
                {
                    out.flush();
                }
            }
            catch (IOException e)
            {
                throw new FusionException(e);
            }

            return voidValue(eval);
        }
    }


    static final class DisplayProc
        extends Procedure
    {
        private final DynamicParameter myCurrentOutputPort;

        DisplayProc(Object currentOutputPort)
        {
            myCurrentOutputPort = (DynamicParameter) currentOutputPort;
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            try
            {
                OutputStream port = myCurrentOutputPort.currentValue(eval);

                // Be careful not to close the output stream.
                OutputStreamWriter out = new OutputStreamWriter(port, UTF_8);
                try
                {
                    for (Object arg : args)
                    {
                        FusionIo.display(eval, out, arg);
                    }
                }
                finally
                {
                    out.flush();
                }
            }
            catch (IOException e)
            {
                throw new FusionException(e);
            }

            return voidValue(eval);
        }
    }


    static final class DisplayToStringProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
                throws FusionException
        {
            String output = displayManyToString(eval, args, 0);
            return makeString(eval, output);
        }
    }


    //========================================================================


    private abstract static class AbstractWithIonFromProc
        extends Procedure
    {
        private final DynamicParameter myCurrentIonReaderParam;

        public AbstractWithIonFromProc(Object currentIonReaderParam)
        {
            myCurrentIonReaderParam = (DynamicParameter) currentIonReaderParam;
        }

        abstract IonReader makeReader(Evaluator eval, Object[] args)
            throws FusionException, IOException;

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(eval, 2, args);

            Procedure thunk = checkProcArg(eval, 1, args);
            // TODO check thunk arity
            //  https://github.com/ion-fusion/fusion-java/issues/76

            try (IonReader reader = makeReader(eval, args))
            {
                Evaluator parameterized =
                    eval.markedContinuation(myCurrentIonReaderParam, reader);

                // We cannot use a tail call here, since we must not close the
                // stream until after the call returns.
                return parameterized.callNonTail(thunk);
            }
            catch (IOException | IonException e)
            {
                // TODO improve error message
                throw new FusionException(e);
            }
        }
    }


    static final class WithIonFromStringProc
        extends AbstractWithIonFromProc
    {
        public WithIonFromStringProc(Object currentIonReaderParam)
        {
            super(currentIonReaderParam);
        }

        @Override
        IonReader makeReader(Evaluator eval, Object[] args)
            throws FusionException, IOException
        {
            String string = checkRequiredStringArg(eval, this, 0, args);

            return eval.getIonReaderBuilder().build(string);
        }
    }


    static final class WithIonFromLobProc
        extends AbstractWithIonFromProc
    {
        public WithIonFromLobProc(Object currentIonReaderParam)
        {
            super(currentIonReaderParam);
        }

        @Override
        IonReader makeReader(Evaluator eval, Object[] args)
            throws FusionException, IOException
        {
            Object lob = args[0];
            if (! isLob(eval, lob) || isAnyNull(eval, lob).isTrue())
            {
                throw argError(eval, "non-null blob or clob", 0, args);
            }

            byte[] bytes = unsafeLobBytesNoCopy(eval, lob);

            return eval.getIonReaderBuilder().build(bytes);
        }
    }


    static final class WithIonFromFileProc
        extends AbstractWithIonFromProc
    {
        public WithIonFromFileProc(Object currentIonReaderParam)
        {
            super(currentIonReaderParam);
        }

        @Override
        IonReader makeReader(Evaluator eval, Object[] args)
            throws FusionException, IOException
        {
            String path = checkNonEmptyStringArg(eval, this, 0, args);

            FileSystemSpecialist fs = eval.getGlobalState().myFileSystemSpecialist;
            InputStream in = fs.openInputFile(eval, getInferredName(), new File(path));
            try
            {
                return eval.getIonReaderBuilder().build(in);
            }
            catch (IonException e)
            {
                // Make sure the InputStream is closed when we fail to pass
                // that responsibility to an IonReader.
                in.close();
                throw e;
            }
        }
    }
}
