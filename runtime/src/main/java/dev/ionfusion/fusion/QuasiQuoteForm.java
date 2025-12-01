// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionList.immutableList;
import static dev.ionfusion.fusion.FusionSexp.immutableSexp;
import dev.ionfusion.fusion.FusionList.BaseList;
import dev.ionfusion.fusion.FusionSexp.BaseSexp;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;

final class QuasiQuoteForm
    extends QuasiBaseForm
{
    public QuasiQuoteForm(Object qqIdentifier,
                          Object uqIdentifier)
    {
        super(qqIdentifier, uqIdentifier);
    }


    //========================================================================


    @Override
    CompiledConstant constant(Evaluator eval, SyntaxValue quotedStx)
        throws FusionException
    {
        Object datum = quotedStx.syntaxToDatum(eval);
        return new CompiledConstant(datum);
    }


    @Override
    CompiledForm unquote(Evaluator    eval,
                         SyntaxValue  unquotedStx,
                         CompiledForm unquotedForm)
        throws FusionException
    {
        return unquotedForm;
    }


    @Override
    CompiledForm quasiSexp(Evaluator      eval,
                           SyntaxSexp     originalStx,
                           CompiledForm[] children)
        throws FusionException
    {
        BaseSexp list = (BaseSexp) originalStx.unwrap(eval);
        BaseSymbol[] annotations = list.getAnnotations();
        return new CompiledQuasiQuoteSexp(annotations, children);
    }


    @Override
    CompiledForm quasiList(Evaluator      eval,
                           SyntaxList     originalStx,
                           CompiledForm[] children)
        throws FusionException
    {
        BaseList list = (BaseList) originalStx.unwrap(eval);
        BaseSymbol[] annotations = list.getAnnotations();
        return new CompiledQuasiQuoteList(annotations, children);
    }


    //========================================================================


    private static final class CompiledQuasiQuoteSexp
        implements CompiledForm
    {
        private final BaseSymbol[]   myAnnotations;
        private final CompiledForm[] myChildForms;

        CompiledQuasiQuoteSexp(BaseSymbol[]   annotations,
                               CompiledForm[] childForms)
        {
            assert childForms.length != 0;
            myAnnotations = annotations;
            myChildForms  = childForms;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            int size = myChildForms.length;
            Object[] children = new Object[size];
            for (int i = 0; i < size; i++)
            {
                children[i] = eval.eval(store, myChildForms[i]);
            }
            return immutableSexp(eval, myAnnotations, children);
        }
    }


    private static final class CompiledQuasiQuoteList
        implements CompiledForm
    {
        private final BaseSymbol[]   myAnnotations;
        private final CompiledForm[] myChildForms;

        CompiledQuasiQuoteList(BaseSymbol[]   annotations,
                               CompiledForm[] childForms)
        {
            myAnnotations = annotations;
            myChildForms  = childForms;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            int size = myChildForms.length;
            Object[] children = new Object[size];
            for (int i = 0; i < size; i++)
            {
                children[i] = eval.eval(store, myChildForms[i]);
            }
            return immutableList(eval, myAnnotations, children);
        }
    }
}
