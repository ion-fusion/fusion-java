// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.LocalEnvironment.LexicalBinding;

/**
 * The {@code lambda} syntactic form, which evaluates to a {@link Closure}.
 */
final class LambdaKeyword
    extends KeywordValue
{
    LambdaKeyword()
    {
        //    "                                                                               |
        super("(PARAM ...) DOC? BODY",
              "Returns a new procedure. When invoked, the caller's arguments are bound to the\n" +
              "PARAMs and the BODY is evaluated and returned.\n" +
              "DOC is an optional documentation string.\n" +
              "BODY may be one or more forms; the result of the last form is the result of the\n" +
              "procedure invocation.");
    }


    @Override
    SyntaxValue expand(Evaluator eval, Environment env, SyntaxSexp source)
        throws SyntaxFailure
    {
        SyntaxChecker check = check(source);
        int arity = check.arityAtLeast(3);

        SyntaxValue[] children = source.extract();

        int bodyStart;
        SyntaxValue maybeDoc = children[2];
        if (maybeDoc.getType() == SyntaxValue.Type.STRING && arity > 3)
        {
            bodyStart = 3;
        }
        else
        {
            bodyStart = 2;
        }

        SyntaxChecker checkFormals = check.subformSexp("formal parameters", 1);
        SyntaxSymbol[] params = determineParams(checkFormals);

        // We create a wrap even if there's no params, because there may be
        // local definitions that will be added to the wrap.
        Environment bodyEnv = new LocalEnvironment(env, params);
        SyntaxWrap localWrap = new EnvironmentRenameWrap(bodyEnv);

        // Prepare the bound names so they resolve to their own binding.
        for (int i = 0; i < params.length; i++)
        {
            SyntaxSymbol param = params[i];
            param = param.addWrap(localWrap);
            param.resolve();           // Caches the binding in the identifier
            params[i] = param;
        }

        children[1] = SyntaxSexp.make(children[1].getLocation(), params);

        for (int i = bodyStart; i < children.length; i++)
        {
            SyntaxValue bodyForm = children[i];
            bodyForm = bodyForm.addWrap(localWrap);
            bodyForm = bodyForm.expand(eval, bodyEnv);
            children[i] = bodyForm;
        }

        source = SyntaxSexp.make(source.getLocation(), children);
        return source;
    }


    private static SyntaxSymbol[] determineParams(SyntaxChecker checkParams)
        throws SyntaxFailure
    {
        SyntaxSexp paramsExpr = (SyntaxSexp) checkParams.form();
        int size = paramsExpr.size();
        if (size == 0) return SyntaxSymbol.EMPTY_ARRAY;

        SyntaxSymbol[] params = new SyntaxSymbol[size];
        for (int i = 0; i < size; i++)
        {
            params[i] = checkParams.requiredSymbol("formal parameter name", i);
        }
        return params;
    }

    //========================================================================


    private static LexicalBinding[] determineBindings(SyntaxSexp paramsExpr)
    {
        int size = paramsExpr.size();
        if (size == 0) return LexicalBinding.EMPTY_ARRAY;

        LexicalBinding[] params = new LexicalBinding[size];
        for (int i = 0; i < size; i++)
        {
            SyntaxSymbol identifier = (SyntaxSymbol) paramsExpr.get(i);
            params[i] = (LexicalBinding) identifier.resolve();
        }
        return params;
    }


    @Override
    CompiledForm compile(Evaluator eval, Environment env, SyntaxSexp source)
        throws FusionException
    {
        String doc;
        int bodyStart;

        SyntaxValue maybeDoc = source.get(2);
        if (maybeDoc.getType() == SyntaxValue.Type.STRING
            && source.size() > 3)
        {
            doc = ((SyntaxString) maybeDoc).stringValue();
            if (doc != null) doc = doc.trim();
            bodyStart = 3;
        }
        else
        {
            doc = null;
            bodyStart = 2;
        }

        LexicalBinding[] params =
            determineBindings((SyntaxSexp) source.get(1));

        CompiledForm body = BeginKeyword.compile(eval, env, source, bodyStart);

        return new CompiledLambda(doc, params, body);
    }


    //========================================================================


    private static final class CompiledLambda
        implements CompiledForm
    {
        private final String           myDoc;
        private final LexicalBinding[] myParams;
        private final CompiledForm     myBody;

        CompiledLambda(String doc, LexicalBinding[] params, CompiledForm body)
        {
            myDoc    = doc;
            myParams = params;
            myBody   = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            return new Closure(store, myDoc, myParams, myBody);
        }
    }
}
