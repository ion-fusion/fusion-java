// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printString;
import dev.ionfusion.fusion.FusionNumber.SumProc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class JavaFfiTest
    extends CoreTestCase
{
    @BeforeEach
    public void requires()
        throws Exception
    {
        topLevel().requireModule("/fusion/ffi/java");
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class NonStatic extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            return null;
        }
    }

    public abstract static class Abstract extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            return null;
        }
    }

    public static class Uninstantiable extends Procedure
    {
        public Uninstantiable()
            throws Exception
        {
            throw new Exception("boom");
        }

        public Uninstantiable(Object arg)
            throws Exception
        {
            throw new Exception("boom");
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            return null;
        }
    }


    private String name(Class<?> c)
    {
        String className = c.getName();
        return printString(className);
    }


    @Test
    public void testJavaNew()
        throws Exception
    {
        eval("(define plus (java_new " + name(SumProc.class) + "))");
        assertEval(2, "(plus 1 1)");

        eval("(define param (java_new " + name(DynamicParameter.class) + " 1))");
        assertEval(1, "(param)");
    }

    @Test
    public void testJavaNewBadArgs()
        throws Exception
    {
        expectContractExn("(define foo (java_new '''no such class'''))");
        expectContractExn("(define foo (java_new " + name(NonStatic.class) + "))");
        expectContractExn("(define foo (java_new " + name(Abstract.class) + "))");
        expectContractExn("(define foo (java_new " + name(Uninstantiable.class) + "))");
        expectContractExn("(define foo (java_new " + name(Uninstantiable.class) + " null))");
   }
}
