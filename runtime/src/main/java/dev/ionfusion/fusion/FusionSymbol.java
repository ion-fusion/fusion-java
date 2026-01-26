// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionBool.trueBool;
import static dev.ionfusion.fusion.FusionString.makeString;
import static dev.ionfusion.fusion._private.FusionUtils.EMPTY_STRING_ARRAY;

import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import com.amazon.ion.util.IonTextUtils;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion._private.InternMap;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.embed.TopLevel;
import java.io.IOException;
import java.util.Arrays;


final class FusionSymbol
{
    private FusionSymbol() {}


    abstract static class BaseSymbol
        // This qualified use is necessary in some build contexts.
        // See https://github.com/ion-fusion/fusion-java/pull/383 for example.
        extends FusionText.BaseText<BaseSymbol>
    {
        static final BaseSymbol[] EMPTY_ARRAY = new BaseSymbol[0];

        private BaseSymbol() {}


        /**
         * NOT FOR APPLICATION USE!
         *
         * @param value may be null to make {@code null.symbol}.
         */
        static BaseSymbol internSymbol(String value)
        {
            if (value == null) return NULL_SYMBOL;

            return ourActualSymbols.intern(value);
        }

        /**
         * NOT FOR APPLICATION USE!
         */
        static BaseSymbol[] internSymbols(String[] names)
        {
            int len = names.length;
            if (len == 0) return EMPTY_ARRAY;

            BaseSymbol[] syms = new BaseSymbol[len];
            for (int i = 0; i < len; i++)
            {
                syms[i] = internSymbol(names[i]);
            }
            return syms;
        }


        /**
         * Returns an equivalent symbol, stripped of any annotations.
         */
        BaseSymbol strip()
        {
            return this;
        }

        static void stripSymbolsInPlace(BaseSymbol[] symbols)
        {
            for (int i = 0; i < symbols.length; i++)
            {
                symbols[i] = symbols[i].strip();
            }
        }


        static String[] unsafeSymbolsToJavaStrings(Object[] fusionSymbols)
        {
            int len = fusionSymbols.length;
            if (len == 0) return EMPTY_STRING_ARRAY;

            String[] strs = new String[len];
            for (int i = 0; i < len; i++)
            {
                strs[i] = ((BaseSymbol) fusionSymbols[i]).stringValue();
            }
            return strs;
        }


        @Override
        public BaseSymbol annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return FusionSymbol.annotate(this, annotations);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof BaseSymbol)
            {
                String r = ((BaseSymbol) right).stringValue();
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

        /**
         * Checks whether this symbol's text is not null and not empty.
         *
         * @return true IFF this is neither {@code null.symbol} nor {@code ''}.
         */
        boolean isNonEmpty()
        {
            String value = stringValue();
            return (value != null && !value.isEmpty());
        }

        private boolean isKeyword()
        {
            String value = stringValue();
            return (value != null
                    && value.startsWith("_")
                    && value.endsWith("_"));
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            if (isKeyword())
            {
                return SyntaxKeyword.makeOriginal(eval, loc, this);
            }
            return SyntaxSymbol.makeOriginal(loc, this);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
            throws FusionException
        {
            if (isKeyword())
            {
                return SyntaxKeyword.make(eval, loc, this);
            }
            return SyntaxSymbol.make(loc, this);
        }
    }


    private static class NullSymbol
        extends BaseSymbol
    {
        private NullSymbol() {}

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
            boolean b = (right instanceof BaseSymbol
                         && ((BaseSymbol) right).isAnyNull());
            return makeBool(eval, b);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            // No need to check for keywords.
            return SyntaxSymbol.makeOriginal(loc, this);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator eval, SourceLocation loc)
            throws FusionException
        {
            // No need to check for keywords.
            return SyntaxSymbol.make(loc, this);
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
        {
            return factory.newNullSymbol();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException
        {
            out.writeNull(IonType.SYMBOL);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException
        {
            out.append("null.symbol");
        }
    }


    /**
     * An interned, unannotated, non-null symbol.
     */
    private static final class ActualSymbol
        extends BaseSymbol
    {
        private final String myContent;

        private ActualSymbol(String content)
        {
            assert content != null;
            myContent = content;
        }

        @Override
        public boolean equals(Object other)
        {
            // We can't optimize this to be a trivial == comparison, since
            // equal instances will exist during symbol creation while we
            // search the intern table for an extant instance.

            if (this == other) return true;
            if (other instanceof ActualSymbol)
            {
                ActualSymbol that = (ActualSymbol) other;
                return myContent.equals(that.myContent);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            // Similarly to equals(), this can't be identityHashCode() due to
            // the use of an intern table.

            return myContent.hashCode();
        }

        @Override
        String stringValue()
        {
            return myContent;
        }

        @Override
        IonValue copyToIonValue(Evaluator eval, ValueFactory factory,
                                boolean throwOnConversionFailure)
        {
            return factory.newSymbol(myContent);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException
        {
            out.writeSymbol(myContent);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException
        {
            IonTextUtils.printSymbol(out, myContent);
        }

        @Override
        void display(Evaluator eval, Appendable out)
            throws IOException
        {
            out.append(myContent);
        }
    }


    private static final class AnnotatedSymbol
        extends BaseSymbol
    {
        /** Not null or empty */
        final BaseSymbol[] myAnnotations;

        /** Not null, and not AnnotatedSymbol */
        final BaseSymbol  myValue;

        private AnnotatedSymbol(BaseSymbol[] annotations, BaseSymbol value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (other instanceof AnnotatedSymbol)
            {
                AnnotatedSymbol that = (AnnotatedSymbol) other;
                if (this.myValue == that.myValue) // Since they are interned
                {
                    // TODO optimize to use identity equality on the symbols
                    return Arrays.equals(myAnnotations, that.myAnnotations);
                }
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            int result = 1;
            result = 31 * result + myValue.hashCode();
            result = 31 * result + Arrays.hashCode(myAnnotations);
            return result;
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
        public BaseSymbol annotate(Evaluator eval, BaseSymbol[] annotations)
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


    private static final BaseSymbol NULL_SYMBOL  = new NullSymbol();

    /**
     * Interning table for unannotated, non-null symbols.
     */
    private static final InternMap<String, ActualSymbol>
        ourActualSymbols = new InternMap<>(ActualSymbol::new, 256);

    /**
     * Interning table for annotated symbols.
     */
    private static final InternMap<AnnotatedSymbol, AnnotatedSymbol>
        ourAnnotatedSymbols = new InternMap<>((s) -> s, 256);

    // TODO Perhaps add expungeStaleEntries() to force GC of intern tables.
    // Because WeakHashMap only purges entries on access, we could end up with
    // a bunch of garbage in there after code compilation is done, and unless
    // new symbols are instantiated there won't be any access to the map and no
    // garbage released. Perhaps it's worth expunging the map (via size())
    // after significant processing events like compiling code.


    /**
     * @param value must not be empty but may be null to make
     * {@code null.symbol}.
     *
     * @return not null.
     */
    static BaseSymbol makeSymbol(Evaluator eval, String value)
    {
        return BaseSymbol.internSymbol(value);
    }


    private static BaseSymbol annotate(BaseSymbol unannotated,
                                       BaseSymbol[] annotations)
    {
        assert ! (unannotated instanceof AnnotatedSymbol);

        if (annotations.length == 0) return unannotated;

        // No way to avoid allocating a key aggregating the value+annotations.
        AnnotatedSymbol sym = new AnnotatedSymbol(annotations, unannotated);

        return ourAnnotatedSymbols.intern(sym);
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     * @param value may be null to make {@code null.symbol}.
     *
     * @return not null.
     */
    static BaseSymbol makeSymbol(Evaluator eval,
                                 String[]  annotations,
                                 String    value)
    {
        BaseSymbol base = makeSymbol(eval, value);
        return annotate(base, BaseSymbol.internSymbols(annotations));
    }


    /**
     * @param fusionSymbol must be a Fusion symbol.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseSymbol unsafeSymbolAnnotate(Evaluator eval,
                                           Object fusionSymbol,
                                           String[] annotations)
    {
        return ((BaseSymbol) fusionSymbol).annotate(eval, annotations);
    }


    //========================================================================
    // Predicates


    public static boolean isSymbol(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseSymbol);
    }

    static boolean isSymbol(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseSymbol);
    }


    //========================================================================
    // Conversions


    /**
     * @param fusionSymbol must be a Fusion symbol.
     *
     * @return null if given {@code null.symbol}.
     */
    static String unsafeSymbolToJavaString(Evaluator eval, Object fusionSymbol)
        throws FusionException
    {
        return ((BaseSymbol) fusionSymbol).stringValue();
    }


    /**
     * Converts a Fusion symbol to a {@link String}.
     *
     * @return null if the value isn't a Fusion symbol.
     */
    static String symbolToJavaString(Evaluator eval, Object value)
        throws FusionException
    {
        if (isSymbol(eval, value))
        {
            return unsafeSymbolToJavaString(eval, value);
        }
        return null;
    }


    static String[] unsafeSymbolsToJavaStrings(Evaluator eval,
                                               Object[]  fusionSymbols)
        throws FusionException

    {
        return BaseSymbol.unsafeSymbolsToJavaStrings(fusionSymbols);
    }


    //========================================================================
    // Procedure Helpers

    /**
     * @param expectation must not be null.
     * @return may be null
     */
    static String checkSymbolArg(Evaluator eval,
                                 Procedure who,
                                 String    expectation,
                                 int       argNum,
                                 Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseSymbol)
        {
            return ((BaseSymbol) arg).stringValue();
        }

        throw who.argError(eval, expectation, argNum, args);
    }


    /**
     * @return may be null
     */
    static String checkNullableSymbolArg(Evaluator eval,
                                         Procedure who,
                                         int       argNum,
                                         Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable symbol";
        return checkSymbolArg(eval, who, expectation, argNum, args);
    }


    /**
     * @return not null
     */
    static String checkRequiredSymbolArg(Evaluator eval,
                                         Procedure who,
                                         int       argNum,
                                         Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "non-null symbol";
        String result = checkSymbolArg(eval, who, expectation, argNum, args);
        if (result == null)
        {
            throw who.argError(eval, expectation, argNum, args);
        }
        return result;
    }


    /**
     * @deprecated Use
     * {@link #checkNullableSymbolArg(Evaluator, Procedure, int, Object...)}.
     */
    @Deprecated
    static String checkNullableArg(Procedure who, int argNum, Object... args)
        throws FusionException, ArgumentException
    {
        return checkNullableSymbolArg(null, who, argNum, args);
    }

    /**
     * @deprecated Use
     * {@link #checkRequiredSymbolArg(Evaluator, Procedure, int, Object...)}.
     */
    @Deprecated
    static String checkRequiredArg(Procedure who, int argNum, Object... args)
        throws FusionException, ArgumentException
    {
        return checkRequiredSymbolArg(null, who, argNum, args);
    }



    //========================================================================
    // Procedures


    static final class IsSymbolProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isSymbol(eval, arg);
            return makeBool(eval, r);
        }
    }


    static final class ToStringProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            String input = checkNullableSymbolArg(eval, this, 0, arg);
            return makeString(eval, input);
        }
    }
}
