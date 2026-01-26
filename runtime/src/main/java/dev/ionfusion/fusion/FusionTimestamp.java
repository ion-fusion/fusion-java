// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.Timestamp.UTC_OFFSET;
import static com.amazon.ion.Timestamp.forDay;
import static com.amazon.ion.Timestamp.forMinute;
import static com.amazon.ion.Timestamp.forMonth;
import static com.amazon.ion.Timestamp.forSecond;
import static com.amazon.ion.Timestamp.forYear;
import static com.amazon.ion.util.IonTextUtils.isDigit;
import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionBool.trueBool;
import static dev.ionfusion.fusion.FusionNumber.checkIntArgToJavaInt;
import static dev.ionfusion.fusion.FusionNumber.isIntOrDecimal;
import static dev.ionfusion.fusion.FusionNumber.makeDecimal;
import static dev.ionfusion.fusion.FusionNumber.makeInt;
import static dev.ionfusion.fusion.FusionNumber.unsafeNumberToBigDecimal;
import static dev.ionfusion.fusion.FusionString.checkNullableStringArg;
import static dev.ionfusion.fusion.FusionString.makeString;
import static dev.ionfusion.fusion.FusionVoid.isVoid;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion.SimpleSyntaxValue.makeSyntax;

import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.Timestamp;
import com.amazon.ion.Timestamp.Precision;
import com.amazon.ion.ValueFactory;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionNumber.BaseDecimal;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.embed.TopLevel;
import java.io.IOException;
import java.math.BigDecimal;


final class FusionTimestamp
{
    private FusionTimestamp() {}



    //========================================================================
    // Representation


    abstract static class BaseTimestamp
        extends BaseValue
        implements AnnotatableValue<BaseTimestamp>
    {
        private BaseTimestamp() {}

        abstract Timestamp timestampValue();

        @Override
        public BaseTimestamp annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return this;
            return new AnnotatedTimestamp(annotations, this);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
        {
            return makeSyntax(eval, loc, this);
        }
    }


    private static class NullTimestamp
        extends BaseTimestamp
    {
        private NullTimestamp() {}

        @Override
        Timestamp timestampValue()
        {
            return null;
        }

        @Override
        boolean isAnyNull()
        {
            return true;
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            boolean b = (right instanceof BaseTimestamp
                         && ((BaseTimestamp) right).isAnyNull());
            return makeBool(eval, b);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
        {
            return factory.newNullTimestamp();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException
        {
            out.writeNull(IonType.TIMESTAMP);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException
        {
            out.append("null.timestamp");
        }
    }


    private static class ActualTimestamp
        extends BaseTimestamp
    {
        private final Timestamp myContent;

        private ActualTimestamp(Timestamp content)
        {
            assert content != null;
            myContent = content;
        }

        @Override
        Timestamp timestampValue()
        {
            return myContent;
        }

        @Override
        BaseBool strictEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseTimestamp)
            {
                Timestamp rt = ((BaseTimestamp) right).timestampValue();
                if (myContent.equals(rt))
                {
                    return trueBool(eval);
                }
            }

            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseTimestamp)
            {
                Timestamp rt = ((BaseTimestamp) right).timestampValue();
                if (rt != null && myContent.compareTo(rt) == 0)
                {
                    return trueBool(eval);
                }
            }

            return falseBool(eval);
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
        {
            return factory.newTimestamp(myContent);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException
        {
            out.writeTimestamp(myContent);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException
        {
            myContent.print(out);
        }
    }


    private static class AnnotatedTimestamp
        extends BaseTimestamp
    {
        /** Not null or empty */
        final BaseSymbol[] myAnnotations;

        /** Not null, and not AnnotatedTimestamp */
        final BaseTimestamp  myValue;

        private AnnotatedTimestamp(BaseSymbol[] annotations,
                                   BaseTimestamp value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
        }

        @Override
        public final boolean isAnnotated()
        {
            return true;
        }

        @Override
        public BaseSymbol[] getAnnotations()
        {
            return myAnnotations;
        }

        @Override
        public BaseTimestamp annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return myValue.annotate(eval, annotations);
        }

        @Override
        boolean isAnyNull() { return myValue.isAnyNull(); }

        @Override
        Timestamp timestampValue()
        {
            return myValue.timestampValue();
        }

        @Override
        BaseBool strictEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return myValue.strictEquals(eval, right);
        }

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
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            IonValue iv = myValue.copyToIonValue(eval, factory,
                                                 throwOnConversionFailure);
            iv.setTypeAnnotations(getAnnotationsAsJavaStrings());
            return iv;
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
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


    private static final BaseTimestamp NULL_TIMESTAMP = new NullTimestamp();


    /**
     * @param value may be null to make {@code null.timestamp}.
     *
     * @return not null.
     */
    static BaseTimestamp makeTimestamp(Evaluator eval, Timestamp value)
    {
        return (value == null ? NULL_TIMESTAMP : new ActualTimestamp(value));
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     * @param value may be null to make {@code null.timestamp}.
     *
     * @return not null.
     */
    static BaseTimestamp makeTimestamp(Evaluator eval,
                                       String[]  annotations,
                                       Timestamp value)
    {
        return makeTimestamp(eval, value).annotate(eval, annotations);
    }


    /**
     * @param fusionTimestamp must be a Fusion timestamp.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseTimestamp unsafeTimestampAnnotate(Evaluator eval,
                                                 Object    fusionTimestamp,
                                                 String[]  annotations)
    {
        return ((BaseTimestamp) fusionTimestamp).annotate(eval, annotations);
    }


    //========================================================================
    // Predicates


    public static boolean isTimestamp(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseTimestamp);
    }

    static boolean isTimestamp(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseTimestamp);
    }


    //========================================================================
    // Conversions


    /**
     * @param fusionTimestamp must be a Fusion timestamp.
     *
     * @return null if given {@code null.timestamp}.
     */
    static Timestamp unsafeTimestampToJavaTimestamp(Evaluator eval,
                                                    Object fusionTimestamp)
        throws FusionException
    {
        return ((BaseTimestamp) fusionTimestamp).timestampValue();
    }


    /**
     * Converts a Fusion timestamp to a {@link Timestamp}.
     *
     * @return null if the value isn't a Fusion timestamp.
     */
    static Timestamp timestampToJavaTimestamp(Evaluator eval, Object value)
        throws FusionException
    {
        if (isTimestamp(eval, value))
        {
            return unsafeTimestampToJavaTimestamp(eval, value);
        }
        return null;
    }


    //========================================================================
    // Procedure Helpers


    /**
     * @param expectation must not be null.
     * @return may be null
     */
    static Timestamp checkTimestampArg(Evaluator eval,
                                       Procedure who,
                                       String    expectation,
                                       int       argNum,
                                       Object... args)
        throws FusionException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseTimestamp)
        {
            return ((BaseTimestamp) arg).timestampValue();
        }

        throw who.argError(eval, expectation, argNum, args);
    }


    static Timestamp checkNullableTimestampArg(Evaluator eval,
                                               Procedure who,
                                               int argNum,
                                               Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable timestamp";
        return checkTimestampArg(eval, who, expectation, argNum, args);
    }


    @Deprecated
    static Timestamp checkNullableArg(Procedure who, int argNum,
                                      Object... args)
        throws FusionException
    {
        return checkNullableTimestampArg(null, who, argNum, args);
    }


    /**
     * @return not null
     */
    static Timestamp checkRequiredTimestampArg(Evaluator eval,
                                               Procedure who,
                                               int       argNum,
                                               Object... args)
        throws FusionException
    {
        String expectation = "non-null timestamp";
        Timestamp result =
            checkTimestampArg(eval, who, expectation, argNum, args);
        if (result == null)
        {
            throw who.argError(eval, expectation, argNum, args);
        }
        return result;
    }


    private static Timestamp makeIonTimestamp(Procedure who, Evaluator eval, Object[] args, Integer offset, int nonVoidArity)
        throws FusionException
    {

        int year = 1, month = 1, day = 1, hour = 0, minute = 0, second = 0;
        BigDecimal fractionalSecond = null;
        Timestamp ionTimestamp = null;

        switch (nonVoidArity)
        {
            case 4:
                throw who.argError(eval, "minute field if given hour field", -1, args);

            case 6:
                if (args[5] instanceof BaseDecimal)
                {
                    fractionalSecond = ((BaseDecimal) args[5]).toBigDecimal();

                    if (fractionalSecond.compareTo(BigDecimal.ZERO) < 0 ||
                        fractionalSecond.compareTo(new BigDecimal(60)) >= 0)
                    {
                        throw who.argError(eval, "non-negative second value less than 60", 5, args);
                    }
                }
                else
                {
                    second = checkIntArgToJavaInt(eval, who, 5, args);

                    if (second < 0 || second >= 60)
                    {
                        throw who.argError(eval, "non-negative second value less than 60", 5, args);
                    }
                }

            case 5:
                minute = checkIntArgToJavaInt(eval, who, 4, args);

                if (minute < 0 || minute > 59)
                {
                    throw who.argError(eval, "minute field between 0 and 59 (inclusive)", 4, args);
                }

                hour = checkIntArgToJavaInt(eval, who, 3, args);

                if (hour < 0 || hour > 23)
                {
                    throw who.argError(eval, "hour field between 0 and 23 (inclusive)", 3, args);
                }

            case 3:
                day = checkIntArgToJavaInt(eval, who, 2, args);

            case 2:
                month = checkIntArgToJavaInt(eval, who, 1, args);

                if (month < 1 || month > 12)
                {
                    throw who.argError(eval, "month field between 1 and 12 (inclusive)", 1, args);
                }

            case 1:
                year = checkIntArgToJavaInt(eval, who, 0, args);

                if (year < 1 || year > 9999)
                {
                    throw who.argError(eval, "year field between 1 and 9999 (inclusive)", 0, args);
                }
        }

        try
        {
            switch (nonVoidArity)
            {
            case 6:
                if (args[5] instanceof BaseDecimal)
                {
                    ionTimestamp = forSecond(year, month, day, hour, minute, fractionalSecond, offset);
                }
                else
                {
                    ionTimestamp = forSecond(year, month, day, hour, minute, second, offset);
                }
                break;

            case 5:
                ionTimestamp = forMinute(year, month, day, hour, minute, offset);
                break;

            case 3:
                ionTimestamp = forDay(year, month, day);
                break;

            case 2:
                ionTimestamp = forMonth(year, month);
                break;

            case 1:
                ionTimestamp = forYear(year);
                break;
            }
        }
        catch (IllegalArgumentException e)
        {
            throw who.argError(eval, "valid day field for the given month", 2, args);
        }

        return ionTimestamp;
    }


    private static int getNumArgs(Evaluator eval, Procedure proc, int arity, Object[] args)
        throws FusionException
    {
        int argNum = 0;
        while (argNum < arity && !isVoid(eval, args[argNum]))
        {
            argNum++;
        }

        int nonVoidArgs = argNum;
        while (argNum < arity)
        {
            if (!isVoid(eval, args[argNum]))
            {
                throw proc.argError(eval, "all void arguments following first void value", -1, args);
            }
            argNum++;
        }

        return nonVoidArgs;
    }


    //========================================================================
    // Procedures


    static final class IsTimestampProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isTimestamp(eval, arg);
            return makeBool(eval, r);
        }
    }


    static final class StringToTimestampProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            String input = checkNullableStringArg(eval, this, 0, arg);

            Timestamp result;
            if (input == null)
            {
                result = null;
            }
            else
            {
                try
                {
                    result = Timestamp.valueOf(input);

                    // Don't accept input "null.timestamp". That's shady.
                    if (result == null)
                    {
                        throw argError(eval, "timestamp-formatted string", 0, arg);
                    }

                    // Hack around Timestamp.valueOf() accepting stoppers.
                    char last = input.charAt(input.length() - 1);
                    if (! (last == 'Z' || last == 'T' || isDigit(last, 10)))
                    {
                        throw argError(eval, "timestamp-formatted string", 0, arg);
                    }
                }
                catch (IllegalArgumentException e)
                {
                    throw argError(eval, "timestamp-formatted string", 0, arg);
                }
            }

            return makeTimestamp(eval, result);
        }
    }


    static final class TimestampToStringProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            Timestamp input = checkNullableTimestampArg(eval, this, 0, arg);

            String result = (input == null ? null : input.toString());
            return makeString(eval, result);
        }
    }


    static final class TimestampProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityRange(1, 7, args);

            Integer offset = null;
            int arity = args.length;
            if (arity == 7) {
                arity = 6;

                if (!isVoid(eval, args[6]))
                {
                    offset = checkIntArgToJavaInt(eval, this, 6, args);
                }
            }

            int nonVoidArity = getNumArgs(eval, this, arity, args);

            if (nonVoidArity < 1)
            {
                throw argError(eval, "at least one non-void argument", -1, args);
            }

            Timestamp ionTimestamp = makeIonTimestamp(this, eval, args, offset, nonVoidArity);
            return makeTimestamp(eval, ionTimestamp);
        }
    }


    static final class TimestampYearProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            Timestamp input = checkRequiredTimestampArg(eval, this, 0, arg);
            int year = input.getYear();

            return makeInt(eval, year);
        }
    }


    static final class TimestampMonthProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            Timestamp input = checkRequiredTimestampArg(eval, this, 0, arg);

            if (input.getPrecision().ordinal() <= Precision.YEAR.ordinal())
            {
                return voidValue(eval);
            }

            int month = input.getMonth();

            return makeInt(eval, month);
        }
    }


    static final class TimestampDayProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            Timestamp input = checkRequiredTimestampArg(eval, this, 0, arg);

            if (input.getPrecision().ordinal() <= Precision.MONTH.ordinal())
            {
                return voidValue(eval);
            }

            int day = input.getDay();

            return makeInt(eval, day);
        }
    }


    static final class TimestampHourProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            Timestamp input = checkRequiredTimestampArg(eval, this, 0, arg);

            if (input.getPrecision().ordinal() <= Precision.DAY.ordinal())
            {
                return voidValue(eval);
            }

            int hours = input.getHour();

            return makeInt(eval, hours);
        }
    }


    static final class TimestampMinuteProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            Timestamp input = checkRequiredTimestampArg(eval, this, 0, arg);

            if (input.getPrecision().ordinal() <= Precision.DAY.ordinal())
            {
                return voidValue(eval);
            }

            int minutes = input.getMinute();

            return makeInt(eval, minutes);
        }
    }


    static final class TimestampSecondProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            Timestamp input = checkRequiredTimestampArg(eval, this, 0, arg);

            if (input.getPrecision().ordinal() <= Precision.MINUTE.ordinal())
            {
                return voidValue(eval);
            }

            int seconds = input.getSecond();

            return makeInt(eval, seconds);
        }
    }


    static final class TimestampWithOffsetProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);

            Timestamp base = checkRequiredTimestampArg(eval, this, 0, args);
            if (base.getPrecision().ordinal() <= Precision.DAY.ordinal())
            {
                throw argError(eval, "timestamp with precision MINUTE or finer", 0, args);
            }

            Integer offset = isVoid(eval, args[1]) ? null : checkIntArgToJavaInt(eval, this, 1, args);

            if (offset != null && (offset <= -1440 || offset >= 1440))
            {
                throw argError(eval, "offset between -1440 and 1440 (exclusive)", 1, args);
            }

            return makeTimestamp(eval, base.withLocalOffset(offset));
        }
    }


    static final class TimestampOffsetProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            Timestamp input = checkRequiredTimestampArg(eval, this, 0, arg);

            Integer offset = input.getLocalOffset();

            return (offset == null) ? voidValue(eval) : makeInt(eval, offset);
        }
    }


    static final class TimestampNowProc
        extends Procedure0
    {
        @Override
        Object doApply(Evaluator eval)
            throws FusionException
        {
            Timestamp now = Timestamp.now();
            return makeTimestamp(eval, now);
        }
    }


    static final class TimestampToEpochMillisProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            Timestamp ts = checkRequiredTimestampArg(eval, this, 0, arg);
            BigDecimal millis = ts.getDecimalMillis();
            return makeDecimal(eval, millis);
        }
    }


    static final class EpochMillisToTimestampProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            if (! isIntOrDecimal(eval, arg) || isAnyNull(eval, arg).isTrue())
            {
                throw argError(eval, "non-null int or decimal", 0, arg);
            }

            BigDecimal epochMillis = unsafeNumberToBigDecimal(eval, arg);
            Timestamp ts = Timestamp.forMillis(epochMillis, UTC_OFFSET);
            return makeTimestamp(eval, ts);
        }
     }


    private abstract static class UnsafeTimestampAddProc
        extends Procedure2
    {
        abstract Timestamp add(Timestamp timestamp, int period);

        @Override
        final Object doApply(Evaluator eval, Object timestamp, Object period)
            throws FusionException
        {
            Timestamp ts = unsafeTimestampToJavaTimestamp(eval, timestamp);
            int p = checkIntArgToJavaInt(eval, this, 1, timestamp, period);
            ts = add(ts, p);
            return makeTimestamp(eval, ts);
        }
    }

    static final class UnsafeTimestampAddYearProc
        extends UnsafeTimestampAddProc
    {
        @Override
        Timestamp add(Timestamp timestamp, int period)
        {
            return timestamp.adjustYear(period);
        }
    }

    static final class UnsafeTimestampAddMonthProc
        extends UnsafeTimestampAddProc
    {
        @Override
        Timestamp add(Timestamp timestamp, int period)
        {
            return timestamp.adjustMonth(period);
        }
    }

    static final class UnsafeTimestampAddDayProc
        extends UnsafeTimestampAddProc
    {
        @Override
        Timestamp add(Timestamp timestamp, int period)
        {
            return timestamp.adjustDay(period);
        }
    }

    static final class UnsafeTimestampAddHourProc
        extends UnsafeTimestampAddProc
    {
        @Override
        Timestamp add(Timestamp timestamp, int period)
        {
            return timestamp.adjustHour(period);
        }
    }

    static final class UnsafeTimestampAddMinuteProc
        extends UnsafeTimestampAddProc
    {
        @Override
        Timestamp add(Timestamp timestamp, int period)
        {
            return timestamp.adjustMinute(period);
        }
    }

    static final class UnsafeTimestampAddSecondProc
        extends UnsafeTimestampAddProc
    {
        @Override
        Timestamp add(Timestamp timestamp, int period)
        {
            return timestamp.adjustSecond(period);
        }
    }
}
