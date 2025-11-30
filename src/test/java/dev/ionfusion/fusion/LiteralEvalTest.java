// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.TestSetup.testDataFile;
import static java.nio.file.Files.newInputStream;

import com.amazon.ion.IonValue;
import java.io.InputStream;
import java.util.Iterator;
import org.junit.jupiter.api.Test;


public class LiteralEvalTest
    extends CoreTestCase
{
    @Test
    public void testSelfEval()
        throws Exception
    {
        InputStream data = newInputStream(testDataFile("selfeval.ion"));
        Iterator<IonValue> clauses = system().iterate(data);
        do {
            IonValue expr = clauses.next();
            assertEval(expr, expr);
        }
        while (clauses.hasNext());
    }


    @Test
    public void testNullSymbol()
        throws Exception
    {
        expectSyntaxExn("null.symbol");
    }


    @Test
    public void testNullSexp()
        throws Exception
    {
        expectSyntaxExn("null.sexp");
    }


    @Test
    public void testEmptySexp()
        throws Exception
    {
        expectSyntaxExn("()");
    }


    @Test
    public void testList()
        throws Exception
    {
        eval("(define x 13)");
        assertEval("[13]", "[x]");
        assertEval("[12,13,14]", "[12,x,14]");
        assertEval("[12,[[13]],14]", "[12,[[x]],14]");
        eval("(define y [x])");
        assertEval("[13]", "y");
        assertEval("[1,[13]]", "[ 1, y]");
        eval("(define x 23)");
        assertEval("[1,{f:[13]}]", "[1, {f:y}]");
        eval("(define z (lambda () [x]))");
        assertEval("[23]", "(z)");
        eval("(define x 33)");
        assertEval("[33]", "(z)");
    }


    @Test
    public void testStruct()
        throws Exception
    {
        eval("(define x 13)");
        assertEval("{f:13}", "{f:x}");
        assertEval("{e:12,f:13,g:14}", "{g:14,f:x,e:12}");
        assertEval("{e:12,f:[[13]],g:14}", "{e:12,f:[[x]],g:14}");
        eval("(define y {f:x})");
        assertEval("{f:13}", "y");
        assertEval("{f:1,f:{f:13}}", "{ f:1, f:y}");
        eval("(define x 23)");
        assertEval("{g:1,f:[{f:13}]}", "{ g:1, f:[y]}");
        eval("(define z (lambda () {a:{b:x}}))");
        assertEval("{a:{b:23}}", "(z)");
        eval("(define x 33)");
        assertEval("{a:{b:33}}", "(z)");
    }


    @Test
    public void testQuote()
        throws Exception
    {
        eval("(define x 13)");
        assertEval("x", "(quote x)");
        assertEval("{f:x,g:true}", "(quote {g:true, f:x})");
        assertEval("[x,99,[]]", "(quote [x, 99, [ ] ])");
    }

    @Test
    public void testQuoteNulls()
        throws Exception
    {
        assertEval("null",        "(quote null.null)");
        assertEval("null.bool",   "(quote null.bool)");
        assertEval("null.string", "(quote null.string)");
        assertEval("null.symbol", "(quote null.symbol)");
        assertEval("null.sexp",   "(quote null.sexp)");
        assertEval("null.list",   "(quote null.list)");
        assertEval("null.struct", "(quote null.struct)");
    }

    @Test
    public void testQuoteArity()
        throws Exception
    {
        expectSyntaxExn("(quote)");
        expectSyntaxExn("(quote quote quote)");
    }

    @Test
    public void testQuotedAnnotations()
        throws Exception
    {
        assertEval("ann::null", "(quote ann::null)");
        assertEval("ann::true", "(quote ann::true)");
        assertEval("ann::99", "(quote ann::99)");
        assertEval("ann::1.2", "(quote ann::1.2)");
        assertEval("ann::1e2", "(quote ann::1e2)");
        assertEval("ann::1982T", "(quote ann::1982T)");
        assertEval("ann::\"s\"", "(quote ann::\"s\")");
        assertEval("ann::s", "(quote ann::s)");
        assertEval("ann::{{}}", "(quote ann::{{}})");
        assertEval("ann::{{\"\"}}", "(quote ann::{{\"\"}})");
        assertEval("a::[a::b::true]", "(quote a::[a::b::true])");
        assertEval("a::(a::b::true)", "(quote a::(a::b::true))");
        assertEval("a::{f:a::b::99}", "(quote a::{f:a::b::99})");
    }


    @Test
    public void testIsNull()
        throws Exception
    {
        assertEval(true,  "(is_null null)");
        assertEval(true,  "(is_null null.int)");
        assertEval(false, "(is_null (void))");
        assertEval(false, "(is_null false)");
        assertEval(false, "(is_null 0)");
        assertEval(false, "(is_null \"\")");
        assertEval(false, "(is_null [])");
        assertEval(false, "(is_null (quote ()))");
        assertEval(false, "(is_null {{}})");
    }

    @Test
    public void testIsNullArity()
        throws Exception
    {
        expectArityExn("(is_null)");
        expectArityExn("(is_null null 2)");
    }

    @Test
    public void testIsNullLeniency()
        throws Exception
    {
        for (String e : allTypeExpressions())
        {
            eval("(is_null " + e + ")");
        }
    }

    @Test
    public void testApplyNonProcedure()
        throws Exception
    {
        expectFusionExn("(1)");
        expectFusionExn("(1 2)");
    }
}
