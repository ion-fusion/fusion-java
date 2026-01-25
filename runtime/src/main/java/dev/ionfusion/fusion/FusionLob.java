// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionString.checkRequiredStringArg;
import static dev.ionfusion.fusion.FusionString.makeString;
import static dev.ionfusion.fusion.SimpleSyntaxValue.makeSyntax;

import dev.ionfusion.fusion.FusionBool.BaseBool;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


/**
 * Utilities for working with Fusion {@code blob} and {@code clob} values.
 *
 * @see FusionValue
 */
public final class FusionLob
{
    private FusionLob() {}


    //========================================================================
    // Representation Classes


    abstract static class BaseLob<Self extends BaseLob<Self>>
        extends BaseValue
        implements AnnotatableValue<Self>
    {
        BaseLob() {}

        byte[] bytesNoCopy()
        {
            return null;
        }

        byte[] bytesCopy()
        {
            return null;
        }

        static BaseBool actualLobEquals(Evaluator eval,
                                        byte[]    leftBytes,
                                        Object    rightLob)
        {
            byte[] rightBytes = ((BaseLob) rightLob).bytesNoCopy();
            boolean b = Arrays.equals(leftBytes, rightBytes);
            return makeBool(eval, b);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseLob)
            {
                byte[] leftBytes = bytesNoCopy();
                return actualLobEquals(eval, leftBytes, right);
            }
            return falseBool(eval);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
        {
            return makeSyntax(eval, loc, this);
        }
    }


    //========================================================================
    // Constructors


    //========================================================================
    // Predicates


    /**
     * Determines whether a Fusion value has type {@code blob} or {@code clob}.
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value the value to test
     *
     * @return {@code true} if the value is a Fusion blob or clob,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isLob(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseLob);
    }


    static boolean isLob(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseLob);
    }


    //========================================================================
    // Conversions


    /** NOT FOR PUBLIC CONSUMPTION */
    static byte[] unsafeLobBytesNoCopy(Evaluator eval, Object lob)
    {
        return ((BaseLob) lob).bytesNoCopy();
    }


    /**
     * Returns the contents of a lob as a byte array, without making extra
     * copies. <b>The caller must take great care not to modify the content of
     * the array!</b> Any changes will violate the lob's immutability contract,
     * which may cause terrible problems.
     *
     * @param top the {@link TopLevel} to use for evaluation
     * @param lob must be a Fusion blob or clob.
     *
     * @return null if {@code lob} is {@code null.blob} or {@code null.clob},
     * otherwise a byte array of the data within the lob.
     */
    public static byte[] unsafeLobBytesNoCopy(TopLevel top, Object lob)
    {
        return ((BaseLob) lob).bytesNoCopy();
    }


    /**
     * Extracts the contents of a lob into a byte array. The bytes are copied
     * from the lob to prevent modification to the value.
     *
     * @param top the {@link TopLevel} to use for evaluation
     * @param lob must be a Fusion blob or clob.
     *
     * @return null if {@code lob} is {@code null.blob} or {@code null.clob},
     * otherwise a fresh copy of the data within the lob.
     */
    public static byte[] unsafeLobBytesCopy(TopLevel top, Object lob)
    {
        return ((BaseLob) lob).bytesCopy();
    }


    //========================================================================
    // Procedure Helpers

    /**
     * @param expectation must not be null.
     * @return may be null
     */
    static Object checkLobArg(Evaluator eval,
                              Procedure who,
                              String    expectation,
                              int       argNum,
                              Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseLob)
        {
            return arg;
        }

        throw who.argError(eval, expectation, argNum, args);
    }


    /**
     * @return may be null
     */
    static Object checkNullableLobArg(Evaluator eval,
                                      Procedure who,
                                      int       argNum,
                                      Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable lob";
        return checkLobArg(eval, who, expectation, argNum, args);
    }


    /**
     * @return not null
     */
    static Object checkRequiredLobArg(Evaluator eval,
                                      Procedure who,
                                      int       argNum,
                                      Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-null lob";
        BaseLob result = (BaseLob) checkLobArg(eval, who, expectation, argNum, args);
        if (result.isAnyNull())
        {
            throw who.argError(eval, expectation, argNum, args);
        }
        return result;
    }


    //========================================================================
    // Procedures

    static final class LobDigestProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object arg0, Object arg1)
            throws FusionException
        {
            Object lob = checkRequiredLobArg(eval, this, 0, arg0, arg1);
            String algorithm = checkRequiredStringArg(eval, this, 1, arg0, arg1);

            if (algorithm.equals("SHA-256") || algorithm.equals("SHA-512"))
            {
                byte[] bytes = unsafeLobBytesNoCopy(eval, lob);

                try
                {
                    MessageDigest md     = MessageDigest.getInstance(algorithm);
                    byte[]        digest = md.digest(bytes);
                    return FusionBlob.forBytesNoCopy(eval, digest);
                }
                catch (NoSuchAlgorithmException e)
                {
                    // Fall through and throw.
                }
            }

            throw argError(eval, "algorithm name; \"SHA-256\" or \"SHA-512\"", 1, arg0, arg1);
        }
    }


    static class UnsafeLobToHexProc
        extends Procedure
    {
        private static final char[] DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        };

        private static final int[] DIGIT_MASKS = { 0xF0, 0x0F };

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            byte[] in = unsafeLobBytesNoCopy(eval, args[0]);
            StringBuilder out = new StringBuilder(in.length * 2);
            for (byte b : in)
            {
                out.append(DIGITS[(DIGIT_MASKS[0] & b) >>> 4]);
                out.append(DIGITS[(DIGIT_MASKS[1] & b)]);
            }

            return makeString(eval, out.toString());
        }
    }
}
