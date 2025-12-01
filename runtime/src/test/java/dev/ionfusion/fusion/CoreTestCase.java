// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printString;
import static dev.ionfusion.fusion.FusionNumber.isDecimal;
import static dev.ionfusion.fusion.FusionNumber.isFloat;
import static dev.ionfusion.fusion.FusionNumber.isInt;
import static dev.ionfusion.fusion.FusionNumber.unsafeIntToJavaBigInteger;
import static dev.ionfusion.fusion.FusionNumber.unsafeNumberToJavaBigDecimal;
import static dev.ionfusion.fusion.FusionString.isString;
import static dev.ionfusion.fusion.FusionString.unsafeStringToJavaString;
import static dev.ionfusion.fusion.FusionValue.isAnyNull;
import static dev.ionfusion.fusion.FusionVoid.isVoid;
import static dev.ionfusion.fusion.TestSetup.testRepositoryDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.amazon.ion.IonContainer;
import com.amazon.ion.IonInt;
import com.amazon.ion.IonList;
import com.amazon.ion.IonSequence;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonText;
import com.amazon.ion.IonValue;
import com.amazon.ion.system.IonSystemBuilder;
import dev.ionfusion.testing.StdioTestCase;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoreTestCase
    extends StdioTestCase
{
    static final String NONTERMINATING_EXPRESSION =
        "((lambda (x) (x x)) (lambda (x) (x x)))";

    /** Exprs that evaluate to Ion types. */
    private static final String[] ION_EXPRESSIONS =
    {
        "null",
        "null.bool", "true", "false",
        "null.int", "0", "1", "12345",
        "null.decimal", "0.", "0.0", "123.45",
        "null.float", "0e0", "123e45",
        "null.timestamp", "2012-04-20T16:20-07:00",
        "null.string", "\"\"", "\"text\"",
        "(quote null.symbol)", "(quote sym)",
        "null.blob", "{{}}",
        "null.clob", "{{\"\"}}",
        "null.list", "[]",
        "(quote null.sexp)", "(quote ())",
        "null.struct", "{}",
    };

    /** Exprs that evaluate to non-Ion types. */
    private static final String[] NON_ION_EXPRESSIONS =
    {
        "(void)",
        "(letrec [(x y), (y 2)] x)", // undef
        "(lambda () 1)",
    };


    //========================================================================


    private final IonSystem            mySystem = IonSystemBuilder.standard().build();
    private       FusionRuntimeBuilder myRuntimeBuilder;
    private       FusionRuntime        myRuntime;
    private       TopLevel             myTopLevel;


    protected IonSystem system()
    {
        return mySystem;
    }

    protected FusionRuntimeBuilder runtimeBuilder()
        throws FusionException
    {
        if (myRuntimeBuilder == null)
        {
            myRuntimeBuilder = TestSetup.makeRuntimeBuilder()
                                        .withInitialCurrentOutputPort(stdout());
        }
        return myRuntimeBuilder;
    }

    protected void useTstRepo()
        throws FusionException
    {
        runtimeBuilder().addRepositoryDirectory(testRepositoryDirectory().toFile());
    }

    protected synchronized FusionRuntime runtime()
        throws FusionException
    {
        if (myRuntime == null)
        {
            myRuntime = runtimeBuilder().build();
        }
        return myRuntime;
    }

    /**
     * Gets the default TopLevel from the {@link #runtime()}.
     */
    protected synchronized TopLevel topLevel()
        throws FusionException
    {
        if (myTopLevel == null)
        {
            myTopLevel = runtime().getDefaultTopLevel();
        }
        return myTopLevel;
    }

    /**
     * For use only in testing internal APIs.
     */
    protected Evaluator evaluator()
        throws FusionException
    {
        StandardTopLevel top = (StandardTopLevel) topLevel();
        return top.getEvaluator();
    }


    //========================================================================
    // Basic evaluation


    protected Object eval(TopLevel top, String expressionIon)
        throws FusionException
    {
        return top.eval(expressionIon);
    }

    protected Object eval(String expressionIon)
        throws FusionException
    {
        TopLevel top = topLevel();
        return eval(top, expressionIon);
    }


    protected Object loadFile(Path path)
        throws FusionException
    {
        TopLevel top = topLevel();
        return top.load(path.toFile());
    }

    protected Object loadFile(String path)
        throws FusionException
    {
        return loadFile(Paths.get(path));
    }


    protected IonValue evalToIon(TopLevel top, String source)
        throws FusionException
    {
        Object fv = eval(top, source);
        IonValue iv = runtime().ionizeMaybe(fv, system());
        if (iv == null)
        {
            fail("Result isn't ion: " + fv + "\nSource: " + source);
        }
        return iv;
    }

    protected IonValue evalToIon(String source)
        throws FusionException
    {
        TopLevel top = topLevel();
        return evalToIon(top, source);
    }


    protected Procedure evalToProcedure(String expressionIon)
        throws FusionException
    {
        Object result = eval(expressionIon);
        return (Procedure) result;
    }


    //========================================================================

    List<String> allTypeExpressions()
    {
        ArrayList<String> exprs = new ArrayList<>();
        Collections.addAll(exprs, ION_EXPRESSIONS);
        Collections.addAll(exprs, NON_ION_EXPRESSIONS);
        assert exprs.size() != 0;
        return exprs;
    }


    List<String> allIonExpressions()
        throws FusionException
    {
        ArrayList<String> exprs = new ArrayList<>();
        Collections.addAll(exprs, ION_EXPRESSIONS);
        assert exprs.size() != 0;
        return exprs;
    }


    List<String> nonIonExpressions()
        throws FusionException
    {
        ArrayList<String> exprs = new ArrayList<>();
        Collections.addAll(exprs, NON_ION_EXPRESSIONS);
        assert exprs.size() != 0;
        return exprs;
    }


    <T extends IonValue> List<String> nonIonExpressions(Class<T> klass)
        throws FusionException
    {
        ArrayList<String> exprs = new ArrayList<>();
        for (String expr : allTypeExpressions())
        {
            Object v = eval(expr);
            IonValue dom = runtime().ionizeMaybe(v, system());
            if (dom == null || ! klass.isInstance(dom))
            {
                exprs.add(expr);
            }
        }
        assert exprs.size() != 0;
        return exprs;
    }


    List<String> nonIntExpressions()
        throws FusionException
    {
        return nonIonExpressions(IonInt.class);
    }

    List<String> nonTextExpressions()
        throws FusionException
    {
        return nonIonExpressions(IonText.class);
    }

    List<String> nonContainerExpressions()
        throws FusionException
    {
        return nonIonExpressions(IonContainer.class);
    }

    List<String> nonSequenceExpressions()
        throws FusionException
    {
        return nonIonExpressions(IonSequence.class);
    }

    List<String> nonListExpressions()
        throws FusionException
    {
        return nonIonExpressions(IonList.class);
    }

    List<String> nonStructExpressions()
        throws FusionException
    {
        return nonIonExpressions(IonStruct.class);
    }


    //========================================================================


    void checkString(String expected, Object actual)
        throws FusionException
    {
        TopLevel top = topLevel();
        if (isString(top, actual))
        {
            String actualString = unsafeStringToJavaString(top, actual);
            assertEquals(expected, actualString);
        }
        else
        {
            fail("Expected " + printString(expected) + " but got " + actual);
        }
    }


    void checkInt(BigInteger expected, Object actual)
        throws FusionException
    {
        TopLevel top = topLevel();
        if (isInt(top, actual))
        {
            BigInteger actualInt = unsafeIntToJavaBigInteger(top, actual);
            if (actualInt != null)
            {
                assertEquals(expected, actualInt);
                return;
            }
        }

        fail("Expected " + expected + " but got " + actual);
    }

    // TODO rename
    void checkLong(long expected, Object actual)
        throws FusionException
    {
        checkInt(BigInteger.valueOf(expected), actual);
    }


    void checkDecimal(BigDecimal expected, Object actual)
        throws FusionException
    {
        TopLevel top = topLevel();
        if (isDecimal(top, actual))
        {
            BigDecimal actualDec = unsafeNumberToJavaBigDecimal(top, actual);
            if (actualDec != null)
            {
                assertEquals(expected, actualDec);
                return;
            }
        }

        fail("Expected " + expected + " but got " + actual);
    }

    void checkFloat(double expected, Object actual)
        throws FusionException
    {
        TopLevel top = topLevel();
        if (isFloat(top, actual) && ! isAnyNull(top, actual))
        {
            double d = FusionNumber.unsafeFloatToJavaDouble(top, actual);
            assertEquals(expected, d, 0);
            return;
        }

        fail("Expected " + expected + " but got " + actual);
    }


    void checkIon(IonValue expected, Object actual)
        throws FusionException
    {
        IonValue iv = runtime().ionizeMaybe(actual, system());
        if (iv == null)
        {
            fail("Result isn't ion: " + actual);
        }
        assertEquals(expected, iv);
    }


    //========================================================================


    protected void assertEval(TopLevel top, IonValue expected, String source)
        throws FusionException
    {
        IonValue iv = evalToIon(top, source);
        assertEquals(expected, iv, source);
    }

    protected void assertEval(IonValue expected, String source)
        throws FusionException
    {
        TopLevel top = topLevel();
        assertEval(top, expected, source);
    }

    protected void assertEval(IonValue expected, IonValue source)
        throws FusionException
    {
        String sourceText = source.toString();
        assertEval(expected, sourceText);
    }

    protected void assertEval(String expectedIon, String sourceIon)
        throws FusionException
    {
        IonValue expected = mySystem.singleValue(expectedIon);
        assertEval(expected, sourceIon);
    }

    protected void assertVoid(String expressionIon)
        throws FusionException
    {
        Object fv = eval(expressionIon);
        if (! isVoid(topLevel(), fv))
        {
            fail("Result isn't void: " + fv + "\nSource: " + expressionIon);
        }
    }

    protected void assertEval(boolean expectedBool, String expressionIon)
        throws FusionException
    {
        IonValue expected = mySystem.newBool(expectedBool);
        assertEval(expected, expressionIon);
    }


    protected void assertEval(TopLevel top,
                              long expectedInt, String expressionIon)
        throws FusionException
    {
        Object fv = top.eval(expressionIon);
        checkLong(expectedInt, fv);
    }

    protected void assertEval(long expectedInt, String expressionIon)
        throws FusionException
    {
        TopLevel top = topLevel();
        assertEval(top, expectedInt, expressionIon);
    }


    protected void assertEval(BigInteger expectedInt, String expressionIon)
        throws FusionException
    {
        Object fv = eval(expressionIon);
        checkInt(expectedInt, fv);
    }

    protected void assertEval(BigDecimal expected, String expressionIon)
        throws FusionException
    {
        Object fv = eval(expressionIon);
        checkDecimal(expected, fv);
    }

    // TODO remove, it's redundant.
    protected void assertBigInt(int expectedInt, String expressionIon)
        throws FusionException
    {
        BigInteger bExpInt = BigInteger.valueOf(expectedInt);
        assertEval(bExpInt, expressionIon);
    }

    protected void assertEval(double expected, String expressionIon)
        throws FusionException
    {
        Object fv = eval(expressionIon);
        checkFloat(expected, fv);
    }

    protected void assertString(String expectedString, String expressionIon)
        throws FusionException
    {
        Object fv = eval(expressionIon);
        checkString(expectedString, fv);
    }

    protected void assertSelfEval(String expressionIon)
        throws FusionException
    {
        assertEval(expressionIon, expressionIon);
    }


    //========================================================================

    <T extends Throwable> T assertEvalThrows(Class<T> klass, String expr)
    {
        return assertThrows(klass, () -> eval(expr));
    }

    void expectFusionExn(String expr)
        throws Exception
    {
        assertEvalThrows(FusionException.class, expr);
    }

    void expectSyntaxExn(String expr)
        throws Exception
    {
        assertEvalThrows(SyntaxException.class, expr);
    }


    void expectUnboundIdentifierExn(String expr)
        throws Exception
    {
        assertEvalThrows(UnboundIdentifierException.class, expr);
    }


    void expectContractExn(String expr)
        throws Exception
    {
        assertEvalThrows(ContractException.class, expr);
    }


    void expectArityExn(String expr)
        throws Exception
    {
        assertEvalThrows(ArityFailure.class, expr);
    }


    void expectArgumentExn(String expr, int badArgNum)
        throws Exception
    {
        ArgumentException e = assertEvalThrows(ArgumentException.class, expr);
        assertEquals(badArgNum, e.getBadPos(), "argument #");
    }
}
