// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionBool.trueBool;
import static dev.ionfusion.fusion.FusionNumber.isFloat;
import static dev.ionfusion.fusion.FusionNumber.isNumber;
import static dev.ionfusion.fusion.FusionNumber.unsafeFloatToDouble;
import static dev.ionfusion.fusion.FusionNumber.unsafeNumberToBigDecimal;
import static dev.ionfusion.fusion.FusionString.isString;
import static dev.ionfusion.fusion.FusionString.unsafeStringToJavaString;
import static dev.ionfusion.fusion.FusionSymbol.isSymbol;
import static dev.ionfusion.fusion.FusionSymbol.unsafeSymbolToJavaString;
import static dev.ionfusion.fusion.FusionTimestamp.isTimestamp;
import static dev.ionfusion.fusion.FusionTimestamp.unsafeTimestampToJavaTimestamp;

import com.amazon.ion.Timestamp;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionNumber.BaseNumber;
import java.math.BigDecimal;

/**
 *
 */
final class FusionCompare
{
    private FusionCompare() {}



    static BaseBool isSame(Evaluator eval, Object left, Object right)
        throws FusionException
    {
        if (left == right) return trueBool(eval);

        if (left instanceof BaseNumber)
        {
            return ((BaseNumber) left).isSame(eval, right);
        }

        return falseBool(eval);
    }


    //========================================================================
    // Procedures

    enum EqualityTier
    {
        STRICT_EQUAL
        {
            @Override
            BaseBool eval(Evaluator eval, Object left, Object right)
                throws FusionException
            {
                return BaseValue.strictEquals(eval, left, right);
            }
        },
        TIGHT_EQUAL
        {
            @Override
            BaseBool eval(Evaluator eval, Object left, Object right)
                throws FusionException
            {
                return BaseValue.tightEquals(eval, left, right);
            }
        },
        LOOSE_EQUAL
        {
            @Override
            BaseBool eval(Evaluator eval, Object left, Object right)
                throws FusionException
            {
                return BaseValue.looseEquals(eval, left, right);
            }
        };

        abstract BaseBool eval(Evaluator eval, Object left, Object right)
            throws FusionException;
    }


    private static abstract class BaseCompareProc
        extends Procedure
    {
        FusionException failure(Evaluator eval, Object[] args)
        {
            return argError(eval, "comparable types", -1, args);
        }


        abstract <T> boolean compare(Comparable<T> left,
                                     T             right,
                                     Object[]      args)
            throws FusionException;

        abstract boolean compare(double left, double right)
            throws FusionException;


        boolean compareStrings(Evaluator eval, String left, String right, Object[] args)
            throws FusionException
        {
            throw failure(eval, args);
        }


        @Override
        final Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);

            Object arg0 = args[0];
            Object arg1 = args[1];

            // Try to avoid conversion to BigDecimal.
            if (isFloat(eval, arg0) && isFloat(eval, arg1))
            {
                if (isAnyNull(eval, arg0).isFalse() &&
                    isAnyNull(eval, arg1).isFalse())
                {
                    double left  = unsafeFloatToDouble(eval, arg0);
                    double right = unsafeFloatToDouble(eval, arg1);

                    boolean r = compare(left, right);
                    return makeBool(eval, r);
                }
            }

            if (isNumber(eval, arg0) && isNumber(eval, arg1))
            {
                BigDecimal left  = unsafeNumberToBigDecimal(eval, arg0);
                BigDecimal right = unsafeNumberToBigDecimal(eval, arg1);

                if (left != null && right != null)
                {
                    boolean r = compare(left, right, args);
                    return makeBool(eval, r);
                }
            }

            if (isString(eval, arg0) && isString(eval, arg1))
            {
                String left  = unsafeStringToJavaString(eval, arg0);
                String right = unsafeStringToJavaString(eval, arg1);

                if (left != null && right != null)
                {
                    boolean r = compareStrings(eval, left, right, args);
                    return makeBool(eval, r);
                }
            }

            if (isSymbol(eval, arg0) && isSymbol(eval, arg1))
            {
                String left  = unsafeSymbolToJavaString(eval, arg0);
                String right = unsafeSymbolToJavaString(eval, arg1);

                if (left != null && right != null)
                {
                    boolean r = compareStrings(eval, left, right, args);
                    return makeBool(eval, r);
                }
            }

            if (isTimestamp(eval, arg0) && isTimestamp(eval, arg1))
            {
                Timestamp left  = unsafeTimestampToJavaTimestamp(eval, arg0);
                Timestamp right = unsafeTimestampToJavaTimestamp(eval, arg1);

                if (left != null && right != null)
                {
                    boolean r = compare(left, right, args);
                    return makeBool(eval, r);
                }
            }

            throw failure(eval, args);
        }
    }


    static final class LessThanProc
        extends BaseCompareProc
    {
        @Override
        <T> boolean compare(Comparable<T> left, T right, Object[] args)
            throws FusionException
        {
            int r = left.compareTo(right);
            return (r < 0);
        }

        @Override
        boolean compare(double left, double right)
            throws FusionException
        {
            return (left < right);
        }
    }


    static final class LessThanOrEqualToProc
        extends BaseCompareProc
    {
        @Override
        <T> boolean compare(Comparable<T> left, T right, Object[] args)
            throws FusionException
        {
            int r = left.compareTo(right);
            return (r <= 0);
        }

        @Override
        boolean compare(double left, double right)
            throws FusionException
        {
            return (left <= right);
        }
    }


    static final class GreaterThanProc
        extends BaseCompareProc
    {
        @Override
        <T> boolean compare(Comparable<T> left, T right, Object[] args)
            throws FusionException
        {
            int r = left.compareTo(right);
            return (r > 0);
        }

        @Override
        boolean compare(double left, double right)
            throws FusionException
        {
            return (left > right);
        }
    }


    static final class GreaterThanOrEqualToProc
        extends BaseCompareProc
    {
        @Override
        <T> boolean compare(Comparable<T> left, T right, Object[] args)
            throws FusionException
        {
            int r = left.compareTo(right);
            return (r >= 0);
        }

        @Override
        boolean compare(double left, double right)
            throws FusionException
        {
            return (left >= right);
        }
    }


    static final class IsIdenticalProc
        extends Procedure
    {
        @Override
        final BaseBool doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);

            Object arg0 = args[0];
            Object arg1 = args[1];

            return makeBool(eval, arg0 == arg1);
        }
    }


    static final class IsSameProc
        extends Procedure
    {
        @Override
        final BaseBool doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);

            Object arg0 = args[0];
            Object arg1 = args[1];

            return isSame(eval, arg0, arg1);
        }
    }


    static final class LooseEqualProc
        extends Procedure
    {
        @Override
        final BaseBool doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);

            Object arg0 = args[0];
            Object arg1 = args[1];

            return BaseValue.looseEquals(eval, arg0, arg1);
        }
    }


    static final class TightEqualProc
        extends Procedure
    {
        @Override
        final BaseBool doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);

            Object arg0 = args[0];
            Object arg1 = args[1];

            return BaseValue.tightEquals(eval, arg0, arg1);
        }
    }


    static final class StrictEqualProc
        extends Procedure
    {
        @Override
        final BaseBool doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);

            Object arg0 = args[0];
            Object arg1 = args[1];

            return BaseValue.strictEquals(eval, arg0, arg1);
        }
    }
}
