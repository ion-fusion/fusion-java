// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionBool.trueBool;
import static dev.ionfusion.fusion.FusionIo.safeWriteToString;
import static dev.ionfusion.fusion.FusionString.checkNullableStringArg;
import static dev.ionfusion.fusion.FusionString.makeString;
import static dev.ionfusion.fusion.SimpleSyntaxValue.makeSyntax;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.HALF_EVEN;

import com.amazon.ion.Decimal;
import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import com.amazon.ion.util.IonTextUtils;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.embed.TopLevel;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Random;

/**
 * Utilities for working with Fusion numbers.
 *
 * @see FusionValue
 */
public final class FusionNumber
{
    private FusionNumber() {}


    abstract static class BaseNumber<Self extends BaseNumber<Self>>
        extends BaseValue
        implements AnnotatableValue<Self>
    {
        private BaseNumber() {}

        int truncateToJavaInt()
        {
            throw new UnsupportedOperationException();
        }

        long truncateToJavaLong()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * @return null if this is a Fusion null value.
         */
        BigInteger truncateToBigInteger()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * @return null if this is a Fusion null value.
         */
        abstract BigDecimal toBigDecimal();

        /** First part of double-dispatch. */
        abstract BaseNumber add(BaseNumber right);
        /** Second part of double-dispatch. */
        abstract BaseNumber add(BaseInt left);

        /** First part of double-dispatch. */
        abstract BaseNumber subtract(BaseNumber right);
        /** Second part of double-dispatch. */
        abstract BaseNumber subtractFrom(BaseInt left);

        /** First part of double-dispatch. */
        abstract BaseNumber multiply(BaseNumber right);
        /** Second part of double-dispatch. */
        abstract BaseNumber multiply(BaseInt left);

        /**
         * Compare value strictly, annotations cannot match.
         * Instance identity has already been performed by the caller.
         */
        BaseBool isSame(Evaluator eval, Object right)
            throws FusionException
        {
            return falseBool(eval);
        }

        /**
         * Second part of double-dispatch.
         * @param left is not a null value
         */
        BaseBool looseEquals2(Evaluator eval, BaseInt left)
            throws FusionException
        {
            return falseBool(eval);
        }

        BaseBool looseEquals2(Evaluator eval, ActualFloat left)
            throws FusionException
        {
            return falseBool(eval);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
        {
            return makeSyntax(eval, loc, this);
        }
    }


    private static BaseBool looseEqualsAsDecimal(Evaluator eval,
                                                 BaseNumber left,
                                                 BaseNumber right)
        throws FusionException
    {
        BigDecimal leftDec  = left.toBigDecimal();
        BigDecimal rightDec = right.toBigDecimal();
        boolean result = leftDec.compareTo(rightDec) == 0;
        return makeBool(eval, result);
    }


    //========================================================================
    // Int Representation


    abstract static class BaseInt
        extends BaseNumber<BaseInt>
    {
        private BaseInt() {}

        @Override
        public BaseInt annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return this;
            return new AnnotatedInt(annotations, this);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseNumber)
            {
                return ((BaseNumber) right).looseEquals2(eval, this);
            }

            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseInt left)
            throws FusionException
        {
            // Both left and right (this) are ints, so "truncation" doesn't
            // affect the value.  Also, neither are null.int due to override
            // of this method by NullInt.
            BigInteger leftInt  = left.truncateToBigInteger();
            BigInteger rightInt = this.truncateToBigInteger();
            boolean result = leftInt.equals(rightInt);
            return makeBool(eval, result);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, ActualFloat left)
            throws FusionException
        {
            return looseEqualsAsDecimal(eval, left, this);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseInt)
            {
                return ((BaseInt) right).looseEquals2(eval, this);
            }

            return falseBool(eval);
        }

        @Override
        BaseNumber add(BaseNumber right)
        {
            return right.add(this);
        }

        @Override
        BaseNumber add(BaseInt left)
        {
            BigInteger leftInt  = left.truncateToBigInteger();
            BigInteger rightInt = this.truncateToBigInteger();
            BigInteger result = leftInt.add(rightInt);
            return makeInt(null, result);
        }

        @Override
        BaseNumber subtract(BaseNumber right)
        {
            return right.subtractFrom(this);
        }

        @Override
        BaseNumber subtractFrom(BaseInt left)
        {
            BigInteger leftInt  = left.truncateToBigInteger();
            BigInteger rightInt = this.truncateToBigInteger();
            BigInteger result = leftInt.subtract(rightInt);
            return makeInt(null, result);
        }

        @Override
        BaseNumber multiply(BaseNumber right)
        {
            return right.multiply(this);
        }

        @Override
        BaseNumber multiply(BaseInt left)
        {
            BigInteger leftInt  = left.truncateToBigInteger();
            BigInteger rightInt = this.truncateToBigInteger();
            BigInteger result = leftInt.multiply(rightInt);
            return makeInt(null, result);
        }

    }


    private static class NullInt
        extends BaseInt
    {
        private NullInt() {}

        @Override
        boolean isAnyNull()
        {
            return true;
        }

        @Override
        BigInteger truncateToBigInteger()
        {
            return null;
        }

        @Override
        BigDecimal toBigDecimal()
        {
            return null;
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            boolean b = (right instanceof BaseInt
                         && ((BaseInt) right).isAnyNull());
            return makeBool(eval, b);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseInt left)
            throws FusionException
        {
            // left is not null, but right (this) is null.
            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, ActualFloat left)
            throws FusionException
        {
            // left is not null, but right (this) is null.
            return falseBool(eval);
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            return factory.newNullInt();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
        {
            out.writeNull(IonType.INT);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("null.int");
        }
    }


    private static class LongInt
        extends BaseInt
    {
        private final long myContent;

        private LongInt(long content)
        {
            myContent = content;
        }

        @Override
        int truncateToJavaInt()
        {
            return (int) myContent;
        }

        @Override
        long truncateToJavaLong()
        {
            return myContent;
        }

        @Override
        BigDecimal toBigDecimal()
        {
            return BigDecimal.valueOf(myContent);
        }

        @Override
        BigInteger truncateToBigInteger()
        {
            return BigInteger.valueOf(myContent);
        }

        @Override
        BaseBool isSame(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof LongInt)
            {
                if (myContent == ((LongInt) right).myContent)
                {
                    return trueBool(eval);
                }
            }
            return falseBool(eval);
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            return factory.newInt(myContent);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
        {
            out.writeInt(myContent);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append(Long.toString(myContent));
        }
    }


    /**
     * Truncation methods aren't implemented since we don't use this class for
     * small values.
     */
    private static class BigInt
        extends BaseInt
    {
        private final BigInteger myContent;

        /**
         * @param content must not be null.
         */
        private BigInt(BigInteger content)
        {
            myContent = content;
        }

        @Override
        BigInteger truncateToBigInteger()
        {
            return myContent;
        }

        @Override
        BigDecimal toBigDecimal()
        {
            return new BigDecimal(myContent);
        }

        @Override
        BaseBool isSame(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BigInt)
            {
                BigInteger rightInt = ((BigInt) right).myContent;
                if (myContent.equals(rightInt))
                {
                    return trueBool(eval);
                }
            }
            return falseBool(eval);
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            return factory.newInt(myContent);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
        {
            out.writeInt(myContent);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append(myContent.toString());
        }
    }


    private static class AnnotatedInt
        extends BaseInt
    {
        /** Not null or empty */
        final BaseSymbol[] myAnnotations;

        /** Not null, and not AnnotatedInt */
        final BaseInt  myValue;

        private AnnotatedInt(BaseSymbol[] annotations, BaseInt value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
        }

        @Override
        public boolean isAnnotated()
        {
            return true;
        }

        @Override
        public BaseSymbol[] getAnnotations()
        {
            return myAnnotations;
        }

        @Override
        public BaseInt annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return myValue.annotate(eval, annotations);
        }

        @Override
        int truncateToJavaInt()
        {
            return myValue.truncateToJavaInt();
        }

        @Override
        long truncateToJavaLong()
        {
            return myValue.truncateToJavaLong();
        }

        @Override
        BigDecimal toBigDecimal()
        {
            return myValue.toBigDecimal();
        }

        @Override
        BigInteger truncateToBigInteger()
        {
            return myValue.truncateToBigInteger();
        }

        @Override
        boolean isAnyNull() { return myValue.isAnyNull(); }

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
        BaseBool looseEquals2(Evaluator eval, ActualFloat left)
            throws FusionException
        {
            return myValue.looseEquals2(eval, left);
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
    // Decimal Representation


    abstract static class BaseDecimal
        extends BaseNumber<BaseDecimal>
    {
        private BaseDecimal() {}

        @Override
        public BaseDecimal annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return this;
            return new AnnotatedDecimal(annotations, this);
        }

        @Override
        BaseNumber add(BaseNumber right)
        {
            BigDecimal leftDec  = this.toBigDecimal();
            BigDecimal rightDec = right.toBigDecimal();
            BigDecimal result = leftDec.add(rightDec);
            return makeDecimal(null, result);
        }

        @Override
        BaseNumber add(BaseInt left)
        {
            BigDecimal leftDec  = left.toBigDecimal();
            BigDecimal rightDec = this.toBigDecimal();
            BigDecimal result = leftDec.add(rightDec);
            return makeDecimal(null, result);
        }

        @Override
        BaseNumber subtract(BaseNumber right)
        {
            BigDecimal leftDec  = this.toBigDecimal();
            BigDecimal rightDec = right.toBigDecimal();
            BigDecimal result = leftDec.subtract(rightDec);
            return makeDecimal(null, result);
        }

        @Override
        BaseNumber subtractFrom(BaseInt left)
        {
            BigDecimal leftDec  = left.toBigDecimal();
            BigDecimal rightDec = this.toBigDecimal();
            BigDecimal result = leftDec.subtract(rightDec);
            return makeDecimal(null, result);
        }

        @Override
        BaseNumber multiply(BaseNumber right)
        {
            BigDecimal leftDec  = this.toBigDecimal();
            BigDecimal rightDec = right.toBigDecimal();
            BigDecimal result = leftDec.multiply(rightDec);
            return makeDecimal(null, result);
        }

        @Override
        BaseNumber multiply(BaseInt left)
        {
            BigDecimal leftDec  = left.toBigDecimal();
            BigDecimal rightDec = this.toBigDecimal();
            BigDecimal result = leftDec.multiply(rightDec);
            return makeDecimal(null, result);
        }
    }


    private static class NullDecimal
        extends BaseDecimal
    {
        private NullDecimal() {}

        @Override
        boolean isAnyNull()
        {
            return true;
        }

        @Override
        BigDecimal toBigDecimal()
        {
            return null;
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            boolean b = (right instanceof BaseDecimal
                         && ((BaseDecimal) right).isAnyNull());
            return makeBool(eval, b);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseInt left)
            throws FusionException
        {
            // left is not null, but right (this) is null.
            return falseBool(eval);
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            return factory.newNullDecimal();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
        {
            out.writeNull(IonType.DECIMAL);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("null.decimal");
        }
    }


    private static class ActualDecimal
        extends BaseDecimal
    {
        private final BigDecimal myContent;

        private ActualDecimal(BigDecimal content)
        {
            assert content != null;
            myContent = content;
        }

        @Override
        BigDecimal toBigDecimal()
        {
            return myContent;
        }

        @Override
        BaseBool isSame(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof ActualDecimal)
            {
                BigDecimal rightDec = ((ActualDecimal) right).myContent;
                if (Decimal.equals(myContent, rightDec))
                {
                    return trueBool(eval);
                }
            }
            return falseBool(eval);
        }

        @Override
        BaseBool strictEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseDecimal)
            {
                BigDecimal rightDec = ((BaseDecimal) right).toBigDecimal();
                if (rightDec != null && Decimal.equals(myContent, rightDec))
                {
                    return trueBool(eval);
                }
            }
            return falseBool(eval);
        }

        private BaseBool tightEquals(Evaluator eval, BigDecimal rightDec)
        {
            if (rightDec != null)
            {
                if (myContent.compareTo(rightDec) == 0)
                {
                    return trueBool(eval);
                }
            }
            return falseBool(eval);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseDecimal)
            {
                BigDecimal rightDec = ((BaseDecimal) right).toBigDecimal();
                return tightEquals(eval, rightDec);
            }
            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseNumber)
            {
                BigDecimal rightDec = ((BaseNumber) right).toBigDecimal();
                return tightEquals(eval, rightDec);
            }

            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseInt left)
            throws FusionException
        {
            BigDecimal leftDec  = left.toBigDecimal();
            boolean result = leftDec.compareTo(myContent) == 0;
            return makeBool(eval, result);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, ActualFloat left)
            throws FusionException
        {
            BigDecimal leftDec  = left.toBigDecimal();
            boolean result = leftDec.compareTo(myContent) == 0;
            return makeBool(eval, result);
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            return factory.newDecimal(myContent);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
        {
            out.writeDecimal(myContent);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            IonTextUtils.printDecimal(out, myContent);
        }
    }


    private static class AnnotatedDecimal
        extends BaseDecimal
    {
        /** Not null or empty */
        final BaseSymbol[] myAnnotations;

        /** Not null, and not AnnotatedDecimal */
        final BaseDecimal  myValue;

        private AnnotatedDecimal(BaseSymbol[] annotations, BaseDecimal value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
        }

        @Override
        public boolean isAnnotated()
        {
            return true;
        }

        @Override
        public BaseSymbol[] getAnnotations()
        {
            return myAnnotations;
        }

        @Override
        public BaseDecimal annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return myValue.annotate(eval, annotations);
        }

        @Override
        boolean isAnyNull() { return myValue.isAnyNull(); }

        @Override
        BigDecimal toBigDecimal()
        {
            return myValue.toBigDecimal();
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
        BaseBool looseEquals2(Evaluator eval, BaseInt right)
            throws FusionException
        {
            return myValue.looseEquals(eval, right);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, ActualFloat left)
            throws FusionException
        {
            return myValue.looseEquals2(eval, left);
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
    // Float Representation

    abstract static class BaseFloat
        extends BaseNumber<BaseFloat>
    {
        private BaseFloat() {}

        /**
         * This must not be {@code null.float}.
         */
        abstract double primitiveDoubleValue();

        abstract Double objectDoubleValue();

        abstract BaseBool tightEquals2(Evaluator eval, ActualFloat left)
            throws FusionException;

        @Override
        public BaseFloat annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return this;
            return new AnnotatedFloat(annotations, this);
        }
    }


    private static class NullFloat
        extends BaseFloat
    {
        private NullFloat() {}

        @Override
        double primitiveDoubleValue()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        Double objectDoubleValue()
        {
            return null;
        }

        @Override
        BigDecimal toBigDecimal()
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
            boolean b = (right instanceof BaseFloat
                         && ((BaseFloat) right).isAnyNull());
            return makeBool(eval, b);
        }

        @Override
        BaseBool tightEquals2(Evaluator eval, ActualFloat right)
            throws FusionException
        {
            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        BaseNumber add(BaseNumber right)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber add(BaseInt left)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber subtract(BaseNumber right)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber subtractFrom(BaseInt left)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber multiply(BaseNumber right)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber multiply(BaseInt left)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            return factory.newNullFloat();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
        {
            out.writeNull(IonType.FLOAT);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("null.float");
        }
    }


    private static class ActualFloat
        extends BaseFloat
    {
        private final double myContent;

        private ActualFloat(double content)
        {
            myContent = content;
        }

        @Override
        double primitiveDoubleValue()
        {
            return myContent;
        }

        @Override
        Double objectDoubleValue()
        {
            return myContent;
        }

        @Override
        BigDecimal toBigDecimal()
        {
            return Decimal.valueOf(myContent);
        }

        @Override
        BaseBool isSame(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof ActualFloat)
            {
                double rightDouble = ((ActualFloat) right).myContent;
                if (Double.compare(myContent, rightDouble) == 0)
                {
                    return trueBool(eval);
                }
            }
            return falseBool(eval);
        }

        @Override
        BaseBool strictEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseFloat)
            {
                Double rightDouble = ((BaseFloat) right).objectDoubleValue();
                if (rightDouble != null
                    && Double.compare(myContent, rightDouble) == 0)
                {
                    return trueBool(eval);
                }
            }
            return falseBool(eval);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseFloat)
            {
                return ((BaseFloat) right).tightEquals2(eval, this);
            }
            return falseBool(eval);
        }

        @Override
        BaseBool tightEquals2(Evaluator eval, ActualFloat left)
            throws FusionException
        {
            return makeBool(eval, left.myContent == this.myContent);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseNumber)
            {
                return ((BaseNumber) right).looseEquals2(eval, this);
            }

            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseInt left)
            throws FusionException
        {
            return looseEqualsAsDecimal(eval, left, this);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, ActualFloat left)
            throws FusionException
        {
            return makeBool(eval, myContent == left.myContent);
        }

        @Override
        BaseNumber add(BaseNumber right)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber add(BaseInt left)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber subtract(BaseNumber right)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber subtractFrom(BaseInt left)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber multiply(BaseNumber right)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber multiply(BaseInt left)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            return factory.newFloat(myContent);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
        {
            out.writeFloat(myContent);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            IonTextUtils.printFloat(out, myContent);
        }
    }


    private static class AnnotatedFloat
        extends BaseFloat
    {
        /** Not null or empty */
        final BaseSymbol[] myAnnotations;

        /** Not null, and not AnnotatedFloat */
        final BaseFloat  myValue;

        private AnnotatedFloat(BaseSymbol[] annotations, BaseFloat value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
        }

        @Override
        public boolean isAnnotated()
        {
            return true;
        }

        @Override
        public BaseSymbol[] getAnnotations()
        {
            return myAnnotations;
        }

        @Override
        public BaseFloat annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return myValue.annotate(eval, annotations);
        }

        @Override
        boolean isAnyNull() { return myValue.isAnyNull(); }

        @Override
        double primitiveDoubleValue()
        {
            return myValue.primitiveDoubleValue();
        }

        @Override
        Double objectDoubleValue()
        {
            return myValue.objectDoubleValue();
        }

        @Override
        BigDecimal toBigDecimal()
        {
            return myValue.toBigDecimal();
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
        BaseBool tightEquals2(Evaluator eval, ActualFloat left)
            throws FusionException
        {
            return myValue.tightEquals2(eval, left);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return myValue.looseEquals(eval, right);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, BaseInt left)
            throws FusionException
        {
            return myValue.looseEquals2(eval, left);
        }

        @Override
        BaseBool looseEquals2(Evaluator eval, ActualFloat left)
            throws FusionException
        {
            return myValue.looseEquals2(eval, left);
        }

        @Override
        BaseNumber add(BaseNumber right)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber add(BaseInt left)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber subtract(BaseNumber right)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber subtractFrom(BaseInt left)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber multiply(BaseNumber right)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        BaseNumber multiply(BaseInt left)
        {
            throw new UnsupportedOperationException();
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
    // Int Constructors


    private static final BaseInt NULL_INT = new NullInt();

    static final BaseInt ZERO_INT = new LongInt(0);
    static final BaseInt ONE_INT  = new LongInt(1);


    private static final BigInteger MIN_LONG_VALUE =
        BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX_LONG_VALUE =
        BigInteger.valueOf(Long.MAX_VALUE);


    /**
     * @return not null.
     */
    static BaseInt makeInt(Evaluator eval, long value)
    {
        if (value == 0) return ZERO_INT;
        if (value == 1) return ONE_INT;

        return new LongInt(value);
    }


    // We may want to optimize this further
    static BaseInt makeInt(Evaluator eval, int value)
    {
        return makeInt(eval, (long) value);
    }


    /**
     * @param value may be null to make {@code null.int}.
     *
     * @return not null.
     */
    static BaseInt makeInt(Evaluator eval, BigInteger value)
    {
        if (value == null) return NULL_INT;

        if (value.compareTo(MIN_LONG_VALUE) >= 0 &&
            value.compareTo(MAX_LONG_VALUE) <= 0)
        {
            long i = value.longValue();
            return new LongInt(i);
        }

        return new BigInt(value);
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseInt makeInt(Evaluator eval, String[] annotations, long value)
    {
        return makeInt(eval, value).annotate(eval, annotations);
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     * @param value may be null to make {@code null.int}.
     *
     * @return not null.
     */
    static BaseInt makeInt(Evaluator  eval,
                           String[]   annotations,
                           BigInteger value)
    {
        return makeInt(eval, value).annotate(eval, annotations);
    }


    /**
     * @param fusionInt must be a Fusion int.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseInt unsafeIntAnnotate(Evaluator eval,
                                     Object fusionInt,
                                     String[] annotations)
    {
        return ((BaseInt) fusionInt).annotate(eval, annotations);
    }


    //========================================================================
    // Decimal Constructors


    private static final BaseDecimal NULL_DECIMAL = new NullDecimal();


    /**
     * @param value may be null to make {@code null.decimal}. An instance of
     * {@link Decimal} should be used to create a negative zero value.
     *
     * @return not null.
     */
    static BaseDecimal makeDecimal(Evaluator eval, BigDecimal value)
    {
        return (value == null ? NULL_DECIMAL : new ActualDecimal(value));
    }


    /**
     * @param value may be null to make {@code null.decimal}. An instance of
     * {@link Decimal} should be used to create a negative zero value.
     *
     * @return not null.
     */
    static BaseDecimal makeDecimal(Evaluator  eval,
                                   String[]   annotations,
                                   BigDecimal value)
    {
        return makeDecimal(eval, value).annotate(eval, annotations);
    }


    /**
     * @param fusionDecimal must be a Fusion decimal.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseDecimal unsafeDecimalAnnotate(Evaluator eval,
                                             Object fusionDecimal,
                                             String[] annotations)
    {
        return ((BaseDecimal) fusionDecimal).annotate(eval, annotations);
    }


    //========================================================================
    // Float Constructors


    private static final BaseFloat NULL_FLOAT = new NullFloat();


    /**
     * @return not null.
     */
    static BaseFloat makeFloat(Evaluator eval, double value)
    {
        return new ActualFloat(value);
    }


    /**
     * @param value may be null to make {@code null.float}.
     *
     * @return not null.
     */
    static BaseFloat makeFloat(Evaluator eval, Double value)
    {
        return (value == null ? NULL_FLOAT : new ActualFloat(value));
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseFloat makeFloat(Evaluator eval,
                               String[]  annotations,
                               double    value)
    {
        return makeFloat(eval, value).annotate(eval, annotations);
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     * @param value may be null to make {@code null.float}.
     *
     * @return not null.
     */
    static BaseFloat makeFloat(Evaluator eval,
                               String[]  annotations,
                               Double    value)
    {
        return makeFloat(eval, value).annotate(eval, annotations);
    }


    /**
     * @param fusionFloat must be a Fusion float.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseFloat unsafeFloatAnnotate(Evaluator eval,
                                         Object    fusionFloat,
                                         String[]  annotations)
    {
        return ((BaseFloat) fusionFloat).annotate(eval, annotations);
    }


    //========================================================================
    // Predicates


    /**
     * Determines whether a Fusion value is an int, decimal, or float.
     *
     * @param top the top-level that was the source of the value.
     * @param value the value to test.
     *
     * @return {@code true} if the value is a Fusion number,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isNumber(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseNumber);
    }

    static boolean isNumber(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseNumber);
    }


    /**
     * Determines whether a Fusion value is an int.
     *
     * @param top the top-level that was the source of the value.
     * @param value the value to test.
     *
     * @return {@code true} if the value is a Fusion integer,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isInt(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseInt);
    }

    static boolean isInt(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseInt);
    }


    /**
     * Determines whether a Fusion value is a decimal.
     *
     * @param top the top-level that was the source of the value.
     * @param value the value to test.
     *
     * @return {@code true} if the value is a Fusion decimal,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isDecimal(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseDecimal);
    }

    static boolean isDecimal(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseDecimal);
    }


    /**
     * Determines whether a Fusion value is an int or decimal.
     *
     * @param top the top-level that was the source of the value.
     * @param value the value to test.
     *
     * @return {@code true} if the value is a Fusion integer or decimal value,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isIntOrDecimal(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseInt || value instanceof BaseDecimal);
    }

    static boolean isIntOrDecimal(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseInt || value instanceof BaseDecimal);
    }


    /**
     * Determines whether a Fusion value is a float.
     *
     * @param top the top-level that was the source of the value.
     * @param value the value to test.
     *
     * @return {@code true} if the value is a Fusion float,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isFloat(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseFloat);
    }

    static boolean isFloat(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseFloat);
    }


    //========================================================================
    // Conversions


    /**
     * Converts a Fusion int to its equivalent Java {@code int} representation,
     * trucating the value if necessary.
     *
     * @param fusionInt must be a non-null int.
     */
    static int unsafeTruncateIntToJavaInt(Evaluator eval, Object fusionInt)
        throws FusionException
    {
        return ((BaseInt) fusionInt).truncateToJavaInt();
    }


    /**
     * @param fusionInt must be a non-null int.
     */
    static long unsafeTruncateIntToJavaLong(Evaluator eval, Object fusionInt)
        throws FusionException
    {
        return ((BaseInt) fusionInt).truncateToJavaLong();
    }


    /**
     * Converts a Fusion int to its equivalent {@link BigInteger} value.
     *
     * @param top the top-level that was the source of the value.
     * @param fusionInt must be a Fusion int.
     *   <b>Any other type of value will cause a crash.</b>
     * @return {@code null} when given {@code null.int}.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static BigInteger unsafeIntToJavaBigInteger(TopLevel top,
                                                       Object   fusionInt)
        throws FusionException
    {
        return ((BaseInt) fusionInt).truncateToBigInteger();
    }

    static BigInteger unsafeIntToJavaBigInteger(Evaluator eval,
                                                Object    fusionInt)
        throws FusionException
    {
        return ((BaseInt) fusionInt).truncateToBigInteger();
    }


    /**
     * Converts a Fusion float to its equivalent Java {@code double} value.
     *
     * @param top the top-level that was the source of the value.
     * @param fusionFloat must be a non-null Fusion float.
     *   <b>Any other type of value will cause a crash.</b>
     *
     * @return the Java double equivalent to the Fusion float.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static double unsafeFloatToJavaDouble(TopLevel top,
                                                 Object   fusionFloat)
        throws FusionException
    {
        return ((BaseFloat) fusionFloat).primitiveDoubleValue();
    }

    static double unsafeFloatToDouble(Evaluator eval, Object fusionFloat)
        throws FusionException
    {
        return ((BaseFloat) fusionFloat).primitiveDoubleValue();
    }


    /**
     * Converts a Fusion number to its equivalent Java {@link BigDecimal}
     * value.
     *
     * @param top the top-level that was the source of the value.
     * @param fusionNumber must be a Fusion int, decimal, or float.
     *   <b>Any other type of value will cause a crash.</b>
     *
     * @return {@code null} when given {@code null.int}, {@code null.decimal},
     *   or {@code null.float}.
     *
     * @throws NumberFormatException if {@code fusionNumber} is a special
     *   float ({@code NaN}, {@code +inf}, or {@code -inf}).
     *   <b>This is a crash of the Fusion runtime and indicates a defect in the
     *   calling code.</b>
     * @throws FusionException if any other error occurs during evaluation
     */
    public static BigDecimal unsafeNumberToJavaBigDecimal(TopLevel top,
                                                          Object   fusionNumber)
        throws FusionException
    {
        return ((BaseNumber) fusionNumber).toBigDecimal();
    }

    static BigDecimal unsafeNumberToBigDecimal(Evaluator eval,
                                               Object    fusionNumber)
        throws FusionException
    {
        return ((BaseNumber) fusionNumber).toBigDecimal();
    }


    //========================================================================
    // Int Procedure Helpers


    /**
     * Checks that an argument is a Fusion int and that its value fits safely
     * into Java's {@code int} type.
     */
    static int checkIntArgToJavaInt(Evaluator eval,
                                    Procedure who,
                                    int       argNum,
                                    Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof LongInt)
        {
            long i = ((LongInt) arg).myContent;
            if (Integer.MIN_VALUE <= i && i <= Integer.MAX_VALUE)
            {
                return (int) i;
            }
        }

        throw who.argError(eval, "32-bit int", argNum, args);
    }


    /**
     * Checks that an argument is a Fusion int and that its value fits safely
     * into Java's {@code long} type.
     */
    static long checkIntArgToJavaLong(Evaluator eval,
                                      Procedure who,
                                      int       argNum,
                                      Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof LongInt)
        {
            return ((LongInt) arg).myContent;
        }

        throw who.argError(eval, "64-bit int", argNum, args);
    }


    /**
     * @param expectation must not be null.
     * @return may be null
     */
    static BigInteger checkIntArg(Evaluator eval,
                                  Procedure who,
                                  String    expectation,
                                  int       argNum,
                                  Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseInt)
        {
            return ((BaseInt) arg).truncateToBigInteger();
        }

        throw who.argError(eval, expectation, argNum, args);
    }


    /**
     * Checks that an argument is a Fusion int, including {@code null.int}.
     *
     * @return the integer value; null when the argument is {@code null.int}.
     */
    static BigInteger checkNullableIntArg(Evaluator eval,
                                          Procedure who,
                                          int argNum,
                                          Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable int";
        return checkIntArg(eval, who, expectation, argNum, args);
    }


    /**
     * Checks that an argument is an actual Fusion int.
     *
     * @return not null
     */
    static BigInteger checkRequiredIntArg(Evaluator eval,
                                          Procedure who,
                                          int       argNum,
                                          Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-null int";
        BigInteger result = checkIntArg(eval, who, expectation, argNum, args);
        if (result == null)
        {
            throw who.argError(eval, expectation, argNum, args);
        }
        return result;
    }


    //========================================================================
    // Decimal Procedure Helpers


    /**
     * @param expectation must not be null.
     * @return may be null
     */
    static BigDecimal checkDecimalArg(Evaluator eval,
                                      Procedure who,
                                      String    expectation,
                                      int       argNum,
                                      Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseDecimal)
        {
            return ((BaseDecimal) arg).toBigDecimal();
        }

        throw who.argError(eval, expectation, argNum, args);
    }

    static BigDecimal checkNullableDecimalArg(Evaluator eval,
                                              Procedure who,
                                              int       argNum,
                                              Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable decimal";
        BigDecimal result = checkDecimalArg(eval, who, expectation, argNum, args);
        return result;
    }


    /**
     * @return not null
     */
    static BigDecimal checkRequiredDecimalArg(Evaluator eval,
                                              Procedure who,
                                              int       argNum,
                                              Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-null decimal";
        BigDecimal result =
            checkDecimalArg(eval, who, expectation, argNum, args);
        if (result == null)
        {
            throw who.argError(eval, expectation, argNum, args);
        }
        return result;
    }


    //========================================================================
    // Float Procedure Helpers


    /**
     * @param expectation must not be null.
     * @return may be null
     */
    static BaseFloat checkFloatArg(Evaluator eval,
                                   Procedure who,
                                   String    expectation,
                                   int       argNum,
                                   Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseFloat)
        {
            return (BaseFloat) arg;
        }

        throw who.argError(eval, expectation, argNum, args);
    }


    static Double checkNullableFloatArg(Evaluator eval,
                                        Procedure who,
                                        int       argNum,
                                        Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable float";
        BaseFloat f = checkFloatArg(eval, who, expectation, argNum, args);
        return f.objectDoubleValue();
    }


    /**
     * @return not null
     */
    static double checkRequiredFloatArg(Evaluator eval,
                                        Procedure who,
                                        int       argNum,
                                        Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-null float";

        BaseFloat f = checkFloatArg(eval, who, expectation, argNum, args);
        if (f.isAnyNull())
        {
            throw who.argError(eval, expectation, argNum, args);
        }
        return f.primitiveDoubleValue();
    }


    //========================================================================
    // Number Procedure Helpers - Can be Decimal, Float or Int.


    /**
     * @param expectation must not be null.
     * @return may be null
     */
    static BigDecimal checkNumberArgToJavaBigDecimal(Evaluator eval,
                                      Procedure who,
                                      String    expectation,
                                      int       argNum,
                                      Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseNumber)
        {
            return ((BaseNumber) arg).toBigDecimal();
        }

        throw who.argError(eval, expectation, argNum, args);
    }

    static BigDecimal checkNullableNumberArgToJavaBigDecimal(Evaluator eval,
                                              Procedure who,
                                              int       argNum,
                                              Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable number";
        BigDecimal result = checkNumberArgToJavaBigDecimal(eval, who, expectation, argNum, args);
        return result;
    }


    /**
     * @return not null
     */
    static BigDecimal checkActualNumberArgToJavaBigDecimal(Evaluator eval,
                                              Procedure who,
                                              int       argNum,
                                              Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-null number";
        BigDecimal result =
           checkNumberArgToJavaBigDecimal(eval, who, expectation, argNum, args);
        if (result == null)
        {
            throw who.argError(eval, expectation, argNum, args);
        }
        return result;
    }


    //========================================================================
    // Procedures


    static final class IsIntProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isInt(eval, arg);
            return makeBool(eval, r);
        }
    }


    static final class IsDecimalProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isDecimal(eval, arg);
            return makeBool(eval, r);
        }
    }



    static final class IsFloatProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isFloat(eval, arg);
            return makeBool(eval, r);
        }
    }


    static final class SumProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            int arity = args.length;

            if (arity == 0) return ZERO_INT;

            Object arg0 = args[0];
            if (arg0 instanceof BaseInt || arg0 instanceof BaseDecimal)
            {
                if (arity == 1) return arg0;

                BaseNumber accum = (BaseNumber) arg0;

                for (int i = 1; i < arity; i++)
                {
                    Object arg = args[i];
                    if (arg instanceof BaseInt || arg instanceof BaseDecimal)
                    {
                        BaseNumber num = (BaseNumber) arg;
                        if (! num.isAnyNull())
                        {
                            accum = accum.add(num);
                            continue;
                        }
                    }

                    throw argError(eval, "non-null int or decimal", i, args);
                }

                return accum;
            }

            throw argError(eval, "non-null int or decimal", 0, args);
        }
    }


    static final class ProductProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            int arity = args.length;

            if (arity == 0) return ONE_INT;

            Object arg0 = args[0];
            if (arg0 instanceof BaseInt || arg0 instanceof BaseDecimal)
            {
                if (arity == 1) return arg0;

                BaseNumber accum = (BaseNumber) arg0;

                for (int i = 1; i < arity; i++)
                {
                    Object arg = args[i];
                    if (arg instanceof BaseInt || arg instanceof BaseDecimal)
                    {
                        BaseNumber num = (BaseNumber) arg;
                        if (! num.isAnyNull())
                        {
                            accum = accum.multiply(num);
                            continue;
                        }
                    }

                    throw argError(eval,"non-null int or decimal", i, args);
                }

                return accum;
            }

            throw argError(eval, "non-null int or decimal", 0, args);
        }
    }


    static final class DifferenceProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityAtLeast(1, args);

            Object arg0 = args[0];
            if (arg0 instanceof BaseInt || arg0 instanceof BaseDecimal)
            {
                BaseNumber accum = (BaseNumber) arg0;

                int arity = args.length;
                if (arity == 1)
                {
                    return ZERO_INT.subtract(accum);
                }
                else
                {
                    for (int i = 1; i < arity; i++)
                    {
                        Object arg = args[i];
                        if (arg instanceof BaseInt || arg instanceof BaseDecimal)
                        {
                            BaseNumber num = (BaseNumber) arg;
                            if (! num.isAnyNull())
                            {
                                accum = accum.subtract(num);
                                continue;
                            }
                        }

                        throw argError(eval, "non-null int or decimal", i, args);
                    }

                    return accum;
                }
            }

            throw argError(eval, "non-null int or decimal", 0, args);
        }
    }


    static class DivideProc
        extends Procedure
    {
        /**
         * Helper to allow subclass to tweak the divison context.
         */
        BigDecimal divide(Evaluator eval, Object[] args,
                          BigDecimal dividend, BigDecimal divisor)
            throws FusionException
        {
            try
            {
                return dividend.divide(divisor);
            }
            catch (ArithmeticException e)
            {
                String message =
                    "Result of division isn't exact.\n" +
                    "Arguments were:\n  " + safeWriteToString(eval, args[0]) +
                    "\n  " + safeWriteToString(eval, args[1]);
                throw contractFailure(message, e);
            }
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);

            BigDecimal dividend = checkRequiredDecimalArg(eval, this, 0, args);
            BigDecimal divisor  = checkRequiredDecimalArg(eval, this, 1, args);

            BigDecimal result = divide(eval, args, dividend, divisor);
            return makeDecimal(eval, result);
        }
    }

    static final class DecimalProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityRange(1, 2, args);

            final int exponent;
            if (args.length == 2)
            {
                exponent = checkRequiredIntArg(eval, this, 1, args).intValue();
            }
            else
            {
                exponent = 0;
            }

            final BigDecimal coefficient;
            try
            {
                coefficient = checkNullableNumberArgToJavaBigDecimal(eval, this, 0, args);

                if (coefficient == null)
                {
                    return makeDecimal(eval, null);
                }
            }
            catch (final NumberFormatException exception)
            {
                final Object rawCoefficient = args[0];
                assert isFloat(eval, rawCoefficient);
                throw argError(eval, "unable to convert non-number float in coefficient", 0, args);
            }

            return makeDecimal(eval, coefficient.scaleByPowerOfTen(exponent));
        }
    }

    /** EXPERIMENTAL **/
    static final class DecimalDivideProc
        extends DivideProc
    {
        @Override
        BigDecimal divide(Evaluator eval, Object[] args,
                          BigDecimal dividend, BigDecimal divisor)
            throws FusionException
        {
            return dividend.divide(divisor, MathContext.DECIMAL128);
        }
    }


    /** EXPERIMENTAL **/
    static final class DecimalDivideRescaleProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(3, args);

            BigDecimal dividend = checkRequiredDecimalArg(eval, this, 0, args);
            BigDecimal divisor  = checkRequiredDecimalArg(eval, this, 1, args);
            int        scale    = checkIntArgToJavaInt   (eval, this, 2, args);

            BigDecimal result = dividend.divide(divisor, scale, HALF_EVEN);
            return makeDecimal(eval, result);
        }
    }


    /** EXPERIMENTAL **/
    static final class DecimalRescaleProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);

            BigDecimal value = checkRequiredDecimalArg(eval, this, 0, args);
            int        scale = checkIntArgToJavaInt   (eval, this, 1, args);

            BigDecimal result = value.setScale(scale, RoundingMode.HALF_EVEN);
            return makeDecimal(eval, result);
        }
    }


    /** EXPERIMENTAL **/
    static final class DecimalScaleProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(1, args);

            BigDecimal value = checkRequiredDecimalArg(eval, this, 0, args);

            int result = value.scale();
            return makeInt(eval, result);
        }
    }


    /** EXPERIMENTAL **/
    static final class DecimalToStringProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(1, args);

            BigDecimal val = checkNullableDecimalArg(eval, this, 0, args);
            String text = null;
            if (val != null)
            {
                text = IonTextUtils.printDecimal(val);
            }
            return makeString(eval, text);
        }
    }


    /**
     * EXPERIMENTAL!
     *
     * This implementation relies on Java's {@link BigDecimal} API to parse
     * decimals. This means it will NOT correctly handle the decimal notation
     * defined by the Ion spec. It also WILL (incorrectly) accept the exponent
     * notation defined for {@code BigDecimal}.
     *
     * @see <a href="https://amazon-ion.github.io/ion-docs/docs/spec.html#real-numbers">Ion specification</a>
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html#%3Cinit%3E(java.lang.String)">
     *   BigDecimal API reference</a>
     *
     **/
    static final class StringToDecimalProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(1, args);

            String val = checkNullableStringArg(eval, this, 0, args);

            BigDecimal bigDecimal = (val != null ? parse(eval, val, args) : null);

            return makeDecimal(eval, bigDecimal);
        }

        BigDecimal parse(Evaluator eval, String text, Object[] args)
            throws FusionException
        {
            try
            {
                // Work-around behavior of BigDecimal constructor, which
                // accepts leading '+' (which we don't want).
                if (! text.startsWith("+"))
                {
                    return Decimal.valueOf(text);
                }
            }
            catch (NumberFormatException e) { }

            throw argError(eval, "valid decimal content", 0, args);
        }
    }


    static final class CeilingProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg0)
            throws FusionException
        {
            if (isAnyNull(eval, arg0).isFalse())
            {
                if (isDecimal(eval, arg0))
                {
                    BigDecimal d = unsafeNumberToBigDecimal(eval, arg0);
                    BigInteger i = d.setScale(0, CEILING).toBigInteger();
                    return makeInt(eval, i);
                }

                if (isInt(eval, arg0))
                {
                    return arg0;
                }
            }

            throw argError(eval, "non-null int or decimal", 0, arg0);
        }
    }


    static final class FloorProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg0)
            throws FusionException
        {
            if (isAnyNull(eval, arg0).isFalse())
            {
                if (isDecimal(eval, arg0))
                {
                    BigDecimal d = unsafeNumberToBigDecimal(eval, arg0);
                    BigInteger i = d.setScale(0, FLOOR).toBigInteger();
                    return makeInt(eval, i);
                }

                if (isInt(eval, arg0))
                {
                    return arg0;
                }
            }

            throw argError(eval, "non-null int or decimal", 0, arg0);
        }
    }

    static final class RandomProc extends Procedure
    {
        private final Random myRand = new Random();

        @Override
        Object doApply(Evaluator eval, Object[] args) throws FusionException
        {
            checkArityRange(0, 1, args);

            if (args.length == 0)
            {
                Double val = myRand.nextDouble();
                return makeFloat(eval, val);
            }
            else
            {
                BigInteger val = checkRequiredIntArg(eval, this, 0, args);
                return makeInt(eval, myRand.nextInt(val.intValue()));
            }
        }
    }
}
