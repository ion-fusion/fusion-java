// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionBool.trueBool;
import static dev.ionfusion.fusion.FusionList.checkActualListArg;
import static dev.ionfusion.fusion.FusionList.immutableList;
import static dev.ionfusion.fusion.FusionList.stretchyList;
import static dev.ionfusion.fusion.FusionList.unsafeListElement;
import static dev.ionfusion.fusion.FusionList.unsafeListSize;
import static dev.ionfusion.fusion.FusionNumber.isInt;
import static dev.ionfusion.fusion.FusionNumber.makeInt;
import static dev.ionfusion.fusion.FusionNumber.unsafeTruncateIntToJavaInt;
import static dev.ionfusion.fusion.FusionString.CHAR_TYPES.LOWERCASE;
import static dev.ionfusion.fusion.FusionString.CHAR_TYPES.UPPERCASE;
import static dev.ionfusion.fusion.FusionSymbol.makeSymbol;
import static dev.ionfusion.fusion.FusionText.checkRequiredTextArg;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static java.lang.Character.highSurrogate;
import static java.lang.Character.isSupplementaryCodePoint;
import static java.lang.Character.lowSurrogate;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import com.amazon.ion.util.IonTextUtils;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.FusionText.BaseText;
import dev.ionfusion.runtime.base.SourceLocation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;


/**
 * Utilities for working with Fusion {@code string} values.
 *
 * @see FusionValue
 */
public final class FusionString
{
    private FusionString() {}


    //========================================================================
    // Representation Classes


    abstract static class BaseString
        extends BaseText<BaseString>
    {
        private BaseString() {}

        @Override
        public BaseString annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return this;
            return new AnnotatedString(annotations, this);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseString)
            {
                String r = ((BaseString) right).stringValue();
                if (r != null)
                {
                    String l = this.stringValue(); // not null
                    if (l.equals(r))
                    {
                        return trueBool(eval);
                    }
                }
            }

            return falseBool(eval);
        }

        @Override
        SyntaxString makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            return SyntaxString.makeOriginal(eval, loc, this);
        }

        @Override
        SyntaxString datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
        {
            return SyntaxString.make(eval, loc, this);
        }


        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o instanceof BaseString)
            {
                BaseString that = (BaseString) o;
                if (Objects.equals(this.stringValue(), that.stringValue()))
                {
                    try
                    {
                        // TODO Unsupported null Evaluator!
                        return FusionValue.sameAnnotations(null, this, that);
                    }
                    catch (FusionException e)
                    {
                        throw new AssertionError("Should not happen", e);
                    }
                }

            }
            return false;
        }
    }


    private static class NullString
        extends BaseString
    {
        private NullString() {}

        @Override
        String stringValue()
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
            boolean b = (right instanceof BaseString
                         && ((BaseString) right).isAnyNull());
            return makeBool(eval, b);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
        {
            return factory.newNullString();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException
        {
            out.writeNull(IonType.STRING);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException
        {
            out.append("null.string");
        }
    }


    private static class ActualString
        extends BaseString
    {
        private final String myContent;

        private ActualString(String content)
        {
            assert content != null;
            myContent = content;
        }

        @Override
        String stringValue()
        {
            return myContent;
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
        {
            return factory.newString(myContent);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException
        {
            out.writeString(myContent);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException
        {
            IonTextUtils.printString(out, myContent);
        }

        @Override
        void display(Evaluator eval, Appendable out)
            throws IOException
        {
            out.append(myContent);
        }
    }


    private static class AnnotatedString
        extends BaseString
    {
        /** Not null or empty */
        final BaseSymbol[] myAnnotations;

        /** Not null, and not AnnotatedString */
        final BaseString  myValue;

        private AnnotatedString(BaseSymbol[] annotations, BaseString value)
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
        public BaseString annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return myValue.annotate(eval, annotations);
        }

        @Override
        boolean isAnyNull() { return myValue.isAnyNull(); }

        @Override
        String stringValue()
        {
            return myValue.stringValue();
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
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            IonValue iv = myValue.copyToIonValue(factory,
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


    private static final BaseString NULL_STRING = new NullString();


    /**
     * @param value may be null to make {@code null.string}.
     *
     * @return not null.
     */
    static BaseString makeString(Evaluator eval, String value)
    {
        return (value == null ? NULL_STRING : new ActualString(value));
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     * @param value may be null to make {@code null.string}.
     *
     * @return not null.
     */
    static BaseString makeString(Evaluator eval,
                                 String[]  annotations,
                                 String    value)
    {
        return makeString(eval, value).annotate(eval, annotations);
    }


    /**
     * @param fusionString must be a Fusion string.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseString unsafeStringAnnotate(Evaluator eval,
                                           Object fusionString,
                                           String[] annotations)
    {
        return ((BaseString) fusionString).annotate(eval, annotations);
    }


    //========================================================================
    // Predicates

    /**
     * Determines whether a given Fusion value is a string.
     *
     * @param top the top-level that was the source of the value.
     * @param value the value to test.
     *
     * @return {@code true} if the value is a Fusion string,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isString(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseString);
    }

    static boolean isString(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseString);
    }


    //========================================================================
    // Conversions


    /**
     * Converts a Fusion string to its equivalent {@link String} value.
     *
     * @param top the top-level that was the source of the value.
     * @param fusionString must be a Fusion string.
     *
     * @return null if given {@code null.string}.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static String unsafeStringToJavaString(TopLevel top, Object fusionString)
        throws FusionException
    {
        return ((BaseString) fusionString).stringValue();
    }

    /**
     * @param fusionString must be a Fusion string.
     *
     * @return null if given {@code null.string}.
     */
    static String unsafeStringToJavaString(Evaluator eval, Object fusionString)
        throws FusionException
    {
        return ((BaseString) fusionString).stringValue();
    }


    /**
     * Converts a Fusion string to a {@link String}.
     *
     * @return null if the value isn't a Fusion string.
     */
    static String stringToJavaString(Evaluator eval, Object value)
        throws FusionException
    {
        if (isString(eval, value))
        {
            return unsafeStringToJavaString(eval, value);
        }
        return null;
    }


    //========================================================================
    // Procedure Helpers


    /**
     * @param expectation must not be null.
     * @return may be null
     */
    static String checkStringArg(Evaluator eval,
                                 Procedure who,
                                 String    expectation,
                                 int       argNum,
                                 Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseString)
        {
            return ((BaseString) arg).stringValue();
        }

        throw who.argError(eval, expectation, argNum, args);
    }


    /**
     * @return may be null
     */
    static String checkNullableStringArg(Evaluator eval,
                                         Procedure who,
                                         int       argNum,
                                         Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable string";
        return checkStringArg(eval, who, expectation, argNum, args);
    }


    /**
     * @return not null
     */
    static String checkRequiredStringArg(Evaluator eval,
                                         Procedure who,
                                         int       argNum,
                                         Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-null string";
        String result = checkStringArg(eval, who, expectation, argNum, args);
        if (result == null)
        {
            throw who.argError(eval, expectation, argNum, args);
        }
        return result;
    }


    /**
     * @return not null or empty
     */
    static String checkNonEmptyStringArg(Evaluator eval,
                                         Procedure who,
                                         int       argNum,
                                         Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-empty string";
        String result = checkStringArg(eval, who, expectation, argNum, args);
        if (result == null || result.isEmpty())
        {
            throw who.argError(eval, expectation, argNum, args);
        }
        return result;
    }


    //========================================================================
    // Java String utilities


    static int numberOfCodePoints(final String string)
    {
        return string.codePointCount(0, string.length());
    }

    enum CHAR_TYPES {
        LOWERCASE
        {
            @Override
            boolean isType(int codePoint) {
                return Character.isLowerCase(codePoint);
            }
        },
        UPPERCASE
        {
            @Override
            boolean isType(int codePoint) {
                return Character.isUpperCase(codePoint);
            }
        };

        abstract boolean isType(int codePoint);
    }

    static boolean everyCodePointIsType(final String string, CHAR_TYPES charType)
    {
        if (isEmptyJavaString(string))
        {
            return false;
        }
        for (int i = 0; i < string.length();)
        {
            int codePoint = string.codePointAt(i);
            if (!charType.isType(codePoint))
            {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }

    static boolean isEmptyJavaString(String string)
    {
        return string == null || string.length() == 0;
    }


    //========================================================================
    // Procedures


    static final class IsStringProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isString(eval, arg);
            return makeBool(eval, r);
        }
    }


    static final class SizeCodePointsProc
            extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object stringArg)
                throws FusionException
        {
            String string = checkRequiredStringArg(eval, this, 0, stringArg);
            return makeInt(eval, numberOfCodePoints(string));
        }
    }


    static final class SizeUtf8Proc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            // TODO size_codepoint requires its string, but this does not.
            String s = checkNullableStringArg(eval, this, 0, arg);

            if (s == null || s.isEmpty()) return FusionNumber.ZERO_INT;

            CharsetEncoder encoder = UTF_8.newEncoder();
            try
            {
                ByteBuffer buffer = encoder.encode(CharBuffer.wrap(s));
                int size = buffer.limit();
                return makeInt(eval, size);
            }
            catch (CharacterCodingException e)
            {
                throw argError(eval, "valid Unicode string", 0, arg);
            }
        }
    }


    static final class AppendProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            StringBuilder resultBuilder = new StringBuilder();

            for (int i = 0; i < args.length; i++)
            {
                String v = checkRequiredTextArg(eval, this, i, args);
                resultBuilder.append(v);
            }

            return makeString(eval, resultBuilder.toString());
        }
    }



    static final class ToLowerProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            String input = checkRequiredStringArg(eval, this, 0, arg);
            return makeString(eval, input.toLowerCase());
        }
    }



    static final class ToUpperProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            String input = checkRequiredStringArg(eval, this, 0, arg);
            return makeString(eval, input.toUpperCase());
        }
    }



    static final class ToSymbolProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            String input = checkNullableStringArg(eval, this, 0, arg);
            return makeSymbol(eval, input);
        }
    }


    /**
     * EXPERIMENTAL!
     *
     * One thing to consider before finalizing is whether the result should be
     * a stretchy list or immutable or mutable.  Stretchy allows the user to
     * edit the data before imploding it back into a string.
     */
    static final class ExplodeProc
        extends Procedure1
    {
       @Override
       Object doApply(Evaluator eval, Object arg)
           throws FusionException
       {
          // TODO what about annotations?
          String string = checkRequiredStringArg(eval, this, 0, arg);

          int charSize   = string.length();
          int scalarSize = string.codePointCount(0, charSize);

          Object[] exploded = new Object[scalarSize];

          int charPos = 0;
          for (int scalarPos = 0; scalarPos < scalarSize; scalarPos++)
          {
             int codePoint = string.codePointAt(charPos);

             exploded[scalarPos] = makeInt(eval, codePoint);

             charPos += Character.charCount(codePoint);
          }
          assert charPos == charSize;

          return stretchyList(eval, exploded);
       }
    }


    static final class ImplodeProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            // TODO what about annotations?
            Object list = checkActualListArg(eval, this, 0, arg);

            final int scalarSize = unsafeListSize(eval, list);
            int charSize = scalarSize;

            char[] chars = new char[charSize];
            int charPos = 0;

            for (int scalarPos = 0; scalarPos < scalarSize; scalarPos++)
            {
                Object scalarObj = unsafeListElement(eval, list, scalarPos);

                if (isInt(eval, scalarObj)
                    && isAnyNull(eval, scalarObj).isFalse())
                {
                    // TODO range check on the scalar
                    int scalar =
                        unsafeTruncateIntToJavaInt(eval, scalarObj);

                    if (isSupplementaryCodePoint(scalar))
                    {
                        if (charSize == scalarSize)
                        {
                            // So far we've only hit BMP code points, but now
                            // we have a supplemental code point, and there
                            // won't have enough room in the buffer.  Allocate
                            // enough extra room for all remaining data to be
                            // supplemental.  We only need to do this once.
                            charSize = charPos + 2 * (charSize - charPos);
                            chars = Arrays.copyOf(chars, charSize);
                        }

                        chars[charPos++] = highSurrogate(scalar);
                        chars[charPos++] = lowSurrogate(scalar);
                    }
                    else
                    {
                        chars[charPos++] = (char) scalar;
                    }
                }
            }

            String string = new String(chars, 0, charPos);
            return makeString(eval, string);
        }
    }

    static final class ContainsProc
            extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
                throws FusionException
        {
            checkArityExact(2, args);
            String string = checkRequiredStringArg(eval, this, 0, args);
            String values = checkRequiredStringArg(eval, this, 1, args);
            return makeBool(eval, string.contains(values));
        }
    }


    static final class EndsWithProc
            extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
                throws FusionException
        {
            checkArityExact(2, args);
            String string = checkRequiredStringArg(eval, this, 0, args);
            String suffix = checkRequiredStringArg(eval, this, 1, args);
            return makeBool(eval, string.endsWith(suffix));
        }
    }


    static final class IndexCodePointsProc
            extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
                throws FusionException
        {
            checkArityExact(2, args);
            String string    = checkRequiredStringArg(eval, this, 0, args);
            String substring = checkRequiredStringArg(eval, this, 1, args);
            int indexOf = string.indexOf(substring);
            if (indexOf == -1)
            {
                return voidValue(eval);
            }
            else
            {
                return makeInt(eval, string.codePointCount(0, indexOf));
            }
        }
    }


    static final class IsLowerCaseProc
            extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object stringArg)
                throws FusionException
        {
            String string = checkRequiredStringArg(eval, this, 0, stringArg);
            return makeBool(eval, everyCodePointIsType(string, LOWERCASE));
        }
    }


    static final class IsUpperCaseProc
            extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object stringArg)
                throws FusionException
        {
            String string = checkRequiredStringArg(eval, this, 0, stringArg);
            return makeBool(eval, everyCodePointIsType(string, UPPERCASE));
        }
    }


    static final class JoinProc
            extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
                throws FusionException
        {
            checkArityAtLeast(1, args);
            String separator = checkRequiredTextArg(eval, this, 0, args);

            StringBuilder resultBuilder = new StringBuilder();

            for (int i = 1; i < args.length; i++)
            {
                String v = checkRequiredTextArg(eval, this, i, args);
                resultBuilder.append(v);
                if (i + 1 < args.length)
                {
                    resultBuilder.append(separator);
                }
            }

            return makeString(eval, resultBuilder.toString());
        }
    }


    static final class ReplaceProc
            extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
                throws FusionException
        {
            checkArityExact(3, args);

            String string = checkNullableStringArg(eval, this, 0, args);
            String from = checkRequiredStringArg(eval, this, 1, args);
            String to = checkRequiredStringArg(eval, this, 2, args);

            String result = null;

            if (string != null)
            {
                result = string.replace(from, to);
            }

            return makeString(eval, result);
        }
    }


    static class SplitProc
            extends Procedure
    {
        String prepareSeparator(String separator)
        {
            return separator;  // TODO This should always be escaped
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
                throws FusionException
        {
            checkArityExact(2, args);
            String string    = checkRequiredStringArg(eval, this, 0, args);
            String separator = checkRequiredStringArg(eval, this, 1, args);
            String[] splitResult = string.split(prepareSeparator(separator));
            List<Object> fusionStrings = new ArrayList<>();
            for (int i = 0; i < splitResult.length; i++)
            {
                if (!(i == 0 && "".equals(splitResult[i])))
                {
                    fusionStrings.add(makeString(eval, splitResult[i]));
                }
            }
            return immutableList(eval, fusionStrings);
        }
    }

    /**
     * TODO This should be merged into SplitProc, removing regex features.
     */
    static final class SplitNoRegexpProc
        extends SplitProc
    {
        @Override
        String prepareSeparator(String separator)
        {
            return Pattern.quote(separator);
        }
    }


    static final class StartsWithProc
            extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
                throws FusionException
        {
            checkArityExact(2, args);
            String string = checkRequiredStringArg(eval, this, 0, args);
            String prefix = checkRequiredStringArg(eval, this, 1, args);
            return makeBool(eval, string.startsWith(prefix));
        }
    }

}
