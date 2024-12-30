// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWriteToString;
import static dev.ionfusion.fusion.FusionList.immutableList;
import dev.ionfusion.fusion.FusionList.BaseList;
import dev.ionfusion.fusion.FusionSexp.BaseSexp;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;

final class QuasiSyntaxForm
    extends QuasiBaseForm
{
    public QuasiSyntaxForm(Object qsIdentifier,
                           Object usIdentifier)
    {
        super(qsIdentifier, usIdentifier);
    }


    //========================================================================


    @Override
    CompiledConstant constant(Evaluator eval, SyntaxValue quotedStx)
        throws FusionException
    {
        return new CompiledConstant(quotedStx);
    }


    @Override
    CompiledForm unquote(Evaluator    eval,
                         SyntaxValue  unquotedStx,
                         CompiledForm unquotedForm)
        throws FusionException
    {
        SourceLocation location = unquotedStx.getLocation();
        String expression = safeWriteToString(eval, unquotedStx);
        return new CompiledUnsyntax(unquotedForm, location, expression);
    }


    @Override
    CompiledForm quasiSexp(Evaluator      eval,
                           SyntaxSexp     originalStx,
                           CompiledForm[] children)
        throws FusionException
    {
        SourceLocation location    = originalStx.getLocation();
        BaseSexp sexp = (BaseSexp) originalStx.unwrap(eval);
        BaseSymbol[] annotations = sexp.getAnnotations();
        return new CompiledQuasiSyntaxSexp(location, annotations, children);
    }


    @Override
    CompiledForm quasiList(Evaluator      eval,
                           SyntaxList     originalStx,
                           CompiledForm[] children)
        throws FusionException
    {
        SourceLocation location    = originalStx.getLocation();
        BaseList list = (BaseList) originalStx.unwrap(eval);
        BaseSymbol[] annotations = list.getAnnotations();
        return new CompiledQuasiSyntaxList(location, annotations, children);
    }


    //========================================================================


    private static final class CompiledQuasiSyntaxSexp
        implements CompiledForm
    {
        private final SourceLocation myLocation;
        private final BaseSymbol[]   myAnnotations;
        private final CompiledForm[] myChildForms;

        CompiledQuasiSyntaxSexp(SourceLocation location,
                                BaseSymbol[]   annotations,
                                CompiledForm[] childForms)
        {
            assert childForms.length != 0;
            myLocation    = location;
            myAnnotations = annotations;
            myChildForms  = childForms;
        }

        @Override
        public SyntaxValue doEval(Evaluator eval, Store store)
            throws FusionException
        {
            int size = myChildForms.length;
            SyntaxValue[] children = new SyntaxValue[size];
            for (int i = 0; i < size; i++)
            {
                Object child = eval.eval(store, myChildForms[i]);

                // This cast is safe because children are either quote-syntax
                // or unsyntax, which always return syntax.
                children[i] = (SyntaxValue) child;
            }

            // We don't use copyReplacingChildren because we don't want the
            // properties to come over.
            return SyntaxSexp.make(eval, myLocation, myAnnotations, children);
        }
    }


    private static final class CompiledQuasiSyntaxList
        implements CompiledForm
    {
        private final SourceLocation myLocation;
        private final BaseSymbol[]   myAnnotations;
        private final CompiledForm[] myChildForms;

        CompiledQuasiSyntaxList(SourceLocation location,
                                BaseSymbol[]   annotations,
                                CompiledForm[] childForms)
        {
            myLocation    = location;
            myAnnotations = annotations;
            myChildForms  = childForms;
        }

        @Override
        public SyntaxValue doEval(Evaluator eval, Store store)
            throws FusionException
        {
            int size = myChildForms.length;
            Object[] children = new Object[size];
            for (int i = 0; i < size; i++)
            {
                children[i] = eval.eval(store, myChildForms[i]);
            }

            // We don't use copyReplacingChildren because we don't want the
            // properties to come over.
            Object list = immutableList(eval, myAnnotations, children);
            return SyntaxList.make(eval, myLocation, list);
        }
    }


    private static final class CompiledUnsyntax
        implements CompiledForm
    {
        private final CompiledForm myUnquotedForm;
        private final SourceLocation myLocation;
        private final String         myExpression;

        CompiledUnsyntax(CompiledForm unquotedForm,
                         SourceLocation location,
                         String expression)
        {
            myUnquotedForm   = unquotedForm;
            myLocation       = location;
            myExpression     = expression;
        }

        @Override
        public SyntaxValue doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object unquoted = eval.eval(store, myUnquotedForm);
            try
            {
                return (SyntaxValue) unquoted;
            }
            catch (ClassCastException e) {}

            String message =
                "Result of (unsyntax " + myExpression +
                ") isn't a syntax value: " +
                safeWriteToString(eval, unquoted);
            throw new ContractException(message, myLocation);
        }
    }
}
