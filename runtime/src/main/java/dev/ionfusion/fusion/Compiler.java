// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWrite;
import static dev.ionfusion.fusion.FusionList.immutableList;
import static dev.ionfusion.fusion.FusionList.unsafeListElement;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionString.stringToJavaString;
import static dev.ionfusion.fusion.FusionStruct.emptyStruct;
import static dev.ionfusion.fusion.FusionStruct.immutableStruct;
import static dev.ionfusion.fusion.FusionStruct.nullStruct;
import static dev.ionfusion.fusion.FusionSymbol.BaseSymbol.internSymbol;
import static dev.ionfusion.fusion.FusionValue.isAnnotated;
import static dev.ionfusion.fusion.FusionValue.isAnyNull;
import static dev.ionfusion.fusion.FusionVoid.isVoid;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion.LetValuesForm.compilePlainLet;

import dev.ionfusion.fusion.FusionStruct.StructFieldVisitor;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.LambdaForm.CompiledLambdaBase;
import dev.ionfusion.fusion.LambdaForm.CompiledLambdaExact;
import dev.ionfusion.fusion.LocalEnvironment.CompiledImmediateVariableReference;
import dev.ionfusion.fusion.LocalEnvironment.CompiledImmediateVariableSet;
import dev.ionfusion.fusion.LocalEnvironment.CompiledLocalVariableReference;
import dev.ionfusion.fusion.LocalEnvironment.CompiledLocalVariableSet;
import dev.ionfusion.fusion.LocalEnvironment.LocalBinding;
import dev.ionfusion.fusion.ModuleNamespace.CompiledImportedVariableReference;
import dev.ionfusion.fusion.ModuleNamespace.CompiledModuleVariableReference;
import dev.ionfusion.fusion.ModuleNamespace.ModuleDefinedBinding;
import dev.ionfusion.fusion.ModuleNamespace.ProvidedBinding;
import dev.ionfusion.fusion.Namespace.CompiledNsDefine;
import dev.ionfusion.fusion.Namespace.CompiledNsDefineSyntax;
import dev.ionfusion.fusion.Namespace.CompiledNsDefineValues;
import dev.ionfusion.fusion.Namespace.NsDefinedBinding;
import dev.ionfusion.fusion.Namespace.RequiredBinding;
import dev.ionfusion.fusion.TopLevelNamespace.CompiledFreeVariableReference;
import dev.ionfusion.fusion.TopLevelNamespace.CompiledTopDefine;
import dev.ionfusion.fusion.TopLevelNamespace.CompiledTopDefineValues;
import dev.ionfusion.fusion.TopLevelNamespace.CompiledTopLevelVariableReference;
import dev.ionfusion.fusion.TopLevelNamespace.TopLevelDefinedBinding;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.fusion._private.doc.model.BindingDoc.Kind;

/**
 * "Registers" used during compilation.
 */
class Compiler
{
    private static final Object STX_PROPERTY_RETAIN_ARG_LOCS =
        internSymbol("#%plain_app retain arg locations");

    private final Evaluator myEval;
    private final boolean   myDocCollectingEnabled;

    Compiler(Evaluator eval)
    {
        myEval = eval;

        DynamicParameter param = eval.getGlobalState().myCollectDocsParam;
        myDocCollectingEnabled = param.currentValue(eval);
    }


    final Evaluator getEvaluator()
    {
        return myEval;
    }

    final GlobalState getGlobalState()
    {
        return myEval.getGlobalState();
    }

    final ModuleInstance getKernel()
    {
        return myEval.findKernel();
    }

    boolean isDocCollectingEnabled()
    {
        return myDocCollectingEnabled;
    }


    /**
     * @see FusionEval#evalCompileTimePartOfTopLevel
     */
    void evalCompileTimePart(final TopLevelNamespace topNs,
                             final SyntaxValue       stx)
        throws FusionException
    {
        SyntaxValue.Visitor v = new SyntaxValue.Visitor()
        {
            @Override
            Object accept(SyntaxValue stx) throws FusionException
            {
                return null;
            }

            @Override
            Object accept(SyntaxSexp stx) throws FusionException
            {
                SyntaxValue first = stx.get(myEval, 0);
                if (first instanceof SyntaxSymbol)
                {
                    SyntacticForm form =
                        ((SyntaxSymbol) first).resolveSyntaxMaybe(topNs);
                    if (form != null)
                    {
                        // TODO Eliminate this tail-call.
                        //  https://github.com/ion-fusion/fusion-java/issues/71
                        form.evalCompileTimePart(Compiler.this, topNs, stx);
                    }
                }
                return null;
            }
        };

        stx.visit(v);
    }


    /**
     * Compiles a single fully-expanded core syntax form.
     */
    CompiledForm compileExpression(final Environment env, SyntaxValue stx)
        throws FusionException
    {
        SyntaxValue.Visitor v = new SyntaxValue.Visitor()
        {
            @Override
            Object accept(SimpleSyntaxValue stx) throws FusionException
            {
                return new CompiledConstant(stx.unwrap(myEval));
            }

            @Override
            Object accept(SyntaxSymbol stx) throws FusionException
            {
                assert stx.getBinding() != null : "No binding for " + stx;
                return compileReference(env, stx);
            }

            @Override
            Object accept(SyntaxKeyword stx) throws FusionException
            {
                throw new IllegalStateException("Should not get here");
            }

            @Override
            Object accept(SyntaxList stx) throws FusionException
            {
                return compileListSemiliteral(env, stx);
            }

            @Override
            Object accept(SyntaxSexp stx) throws FusionException
            {
                return compileExpression(env, stx);
            }

            @Override
            Object accept(SyntaxStruct stx) throws FusionException
            {
                return compileStructSemiliteral(env, stx);
            }
        };

        return (CompiledForm) stx.visit(v);
    }


    CompiledForm compileExpression(Environment env, SyntaxSexp stx)
        throws FusionException
    {
        SyntacticForm form = stx.syntaxForm(myEval, env);
        if (form != null)
        {
            // We found a static top-level syntax binding!

            // NOTE: Unexpected failure to get here indicates use of a built-in
            // syntactic form that's defined (probably via java_new) in the
            // same module. That's not supported! Such modules need to be
            // broken apart to meet this requirement.  This won't affect
            // users unless we open the whole compiler APIs so that they can add
            // new "built-in" syntax.

            return form.compile(this, env, stx);
        }

        return compileProcedureApplication(env, stx);
    }


    /**
     * Compiles a sequence of individual expressions.
     *
     * @return not null, but perhaps {@link CompiledForm#EMPTY_ARRAY}.
     */
    CompiledForm[] compileExpressions(Environment env, SyntaxSequence source,
                                      int from, int to)
        throws FusionException
    {
        int size = to - from;

        if (size == 0) return CompiledForm.EMPTY_ARRAY;

        CompiledForm[] forms = new CompiledForm[size];
        for (int i = from; i < to; i++)
        {
            SyntaxValue form = source.get(myEval, i);
            forms[i - from] = compileExpression(env, form);
        }

        return forms;
    }


    /**
     * Compiles a sequence of individual expressions.
     *
     * @return not null, but perhaps {@link CompiledForm#EMPTY_ARRAY}.
     */
    CompiledForm[] compileExpressions(Environment env, SyntaxSequence source,
                                      int from)
        throws FusionException
    {
        return compileExpressions(env, source, from, source.size());
    }


    /**
     * Compiles a sequence of expressions as if in a {@code begin} expression.
     */
    final CompiledForm compileBegin(Environment env, SyntaxSexp stx,
                                    int from, int to)
        throws FusionException
    {
        int size = to - from;

        if (size == 0) return new CompiledConstant(voidValue(myEval));

        if (size == 1) return compileExpression(env, stx.get(myEval, from));

        CompiledForm[] subforms = compileExpressions(env, stx, from, to);
        // TODO Perhaps eliminate forms that are constants?
        return new CompiledBegin(subforms);
    }

    CompiledForm compileBegin(Environment env, SyntaxSexp stx, int from)
        throws FusionException
    {
        return compileBegin(env, stx, from, stx.size());
    }


    private CompiledForm compileProcedureApplication(Environment env,
                                                     SyntaxSexp  stx)
        throws FusionException
    {
        SyntaxValue procExpr = stx.get(myEval, 0);
        CompiledForm procForm = compileExpression(env, procExpr);
        CompiledForm[] argForms = compileExpressions(env, stx, 1);

        if (procForm instanceof CompiledLambdaExact)
        {
            CompiledLambdaBase lambda = (CompiledLambdaBase) procForm;
            if (lambda.myArgNames.length != argForms.length)
            {
                String message =
                    "procedure expects " + lambda.myArgNames.length +
                    " arguments but application has " + argForms.length +
                    " expressions";
                 throw new SyntaxException("procedure application", message,
                                           stx);
            }

            SourceLocation[] argLocs = extractArgLocations(stx,
                                                           argForms.length);
            return compilePlainLet(argForms, argLocs, lambda.myBody);
        }

        // Look for syntax property forcing argument location tracking.
        if (isVoid(myEval, stx.findProperty(myEval, STX_PROPERTY_RETAIN_ARG_LOCS)))
        {
            return new CompiledPlainApp(stx.getLocation(), procForm, argForms);
        }

        SourceLocation[] argLocs = extractArgLocations(stx, argForms.length);

        return new CompiledPlainAppWithLocations(stx.getLocation(), procForm,
                                                 argForms, argLocs);
    }

    private SourceLocation[] extractArgLocations(SyntaxSexp stx, int argCount)
        throws FusionException
    {
        SourceLocation[] argLocs = new SourceLocation[argCount];
        Object argSexp = stx.unwrap(myEval);
        for (int i = 0; i < argCount; i++)
        {
            argSexp = unsafePairTail(myEval, argSexp);
            SyntaxValue argExpr = (SyntaxValue)
                unsafePairHead(myEval, argSexp);

            argLocs[i] = argExpr.getLocation();
        }
        return argLocs;
    }


    CompiledForm compileDefineValues(final Environment env, SyntaxSexp stx)
        throws FusionException
    {
        int arity = stx.size();
        SyntaxValue valueSource = stx.get(myEval, arity - 1);
        final CompiledForm valuesForm = compileExpression(env, valueSource);

        SyntaxSexp idSexp = (SyntaxSexp) stx.get(myEval, 1);
        SyntaxSymbol[] ids = idSexp.extract(myEval, SyntaxSymbol.class);
        int idCount = ids.length;

        Namespace namespace = env.namespace();
        if (namespace instanceof TopLevelNamespace)
        {
            // Regardless of any current binding for this id, treat it the same.
            if (idCount == 1)
            {
                return new CompiledTopDefine(ids[0], valuesForm);
            }
            else
            {
                return new CompiledTopDefineValues(ids, valuesForm);
            }
        }

        String[] names  = new String[idCount];
        int[] addresses = new int   [idCount];

        for (int i = 0; i < idCount; i++)
        {
            SyntaxSymbol identifier = ids[i];
            Binding b = identifier.getBinding();

            names[i] = b.getName().stringValue();
            addresses[i] = ((ModuleDefinedBinding) b).myAddress;
        }

        CompiledForm compiled;
        if (idCount == 1)
        {
            compiled = new CompiledNsDefine(names[0], addresses[0], valuesForm);
        }
        else
        {
            compiled = new CompiledNsDefineValues(names, addresses, valuesForm);
        }

        // Collect documentation when necessary.
        if (arity != 3 && isDocCollectingEnabled())
        {
            SyntaxSequence docSeq = (SyntaxSequence) stx.get(myEval, 2);

            for (int i = 0; i < idCount; i++)
            {
                Object docString = docSeq.get(myEval, i).unwrap(myEval);
                BindingDoc doc = new BindingDoc(null, // kind
                                                null, // usage
                                                stringToJavaString(myEval, docString));
                namespace.setDoc(addresses[i], doc);
            }
        }

        return compiled;
    }


    CompiledForm compileDefineSyntax(final Environment env, SyntaxSexp stx)
        throws FusionException
    {
        int arity = stx.size();
        SyntaxValue valueSource = stx.get(myEval, arity-1);
        final CompiledForm valueForm = compileExpression(env, valueSource);

        final SyntaxSymbol identifier = (SyntaxSymbol) stx.get(myEval, 1);
        Binding binding = identifier.getBinding();

        Binding.Visitor v = new Binding.Visitor()
        {
            @Override
            Object visit(Binding b) throws FusionException
            {
                String msg = "Unexpected binding type for `define_syntax`.";
                throw new IllegalStateException(msg);
            }

            @Override
            public Object visit(NsDefinedBinding b) throws FusionException
            {
                String name = b.getName().stringValue();
                return new CompiledNsDefineSyntax(name, b.myAddress, valueForm);
            }

            @Override
            public Object visit(TopLevelDefinedBinding b) throws FusionException
            {
                // TODO This should bind after evaluation, as 'define'.
                //  https://github.com/ion-fusion/fusion-java/issues/75
                return visit((NsDefinedBinding) b);
            }
        };

        CompiledForm compiled = (CompiledForm) binding.visit(v);

        if (arity != 3 && isDocCollectingEnabled())
        {
            // We have documentation. Sort of.
            Object docString = stx.get(myEval, 2).unwrap(myEval);
            BindingDoc doc = new BindingDoc(Kind.SYNTAX,
                                            null, // usage
                                            stringToJavaString(myEval, docString));
            int address = ((NsDefinedBinding) binding).myAddress;
            env.namespace().setDoc(address, doc);
        }

        return compiled;
    }


    private CompiledForm compileReference(final Environment  env,
                                          final SyntaxSymbol identifier)
        throws FusionException
    {
        Binding.Visitor v = new Binding.Visitor()
        {
            @Override
            Object visit(Binding b) throws FusionException
            {
                String msg = "Unexpected binding type for variable reference.";
                throw new IllegalStateException(msg);
            }

            @Override
            Object visit(FreeBinding b) throws FusionException
            {
                throw new UnboundIdentifierException(identifier);
            }

            @Override
            Object visit(LocalBinding b) throws FusionException
            {
                int rib = env.getDepth() - b.myDepth;
                if (rib == 0)
                {
                    return new CompiledImmediateVariableReference(b.myAddress);
                }
                return new CompiledLocalVariableReference(rib, b.myAddress);
            }

            @Override
            Object visit(TopLevelDefinedBinding b) throws FusionException
            {
                assert b.isOwnedBy(env.namespace());
                return new CompiledTopLevelVariableReference(b.myAddress);
            }

            @Override
            Object visit(ModuleDefinedBinding b) throws FusionException
            {
                Namespace localNamespace = env.namespace();
                if (localNamespace.getModuleId() != b.myModuleId)
                {
                    // We have a reference to a binding from another module!
                    // Compiled form must include address of the module since it
                    // won't be the top of the runtime environment chain.

                    int moduleAddress =
                        localNamespace.requiredModuleAddress(b.myModuleId);

                    return new CompiledImportedVariableReference(moduleAddress,
                                                                 b.myAddress);
                }

                SourceLocation locn = identifier.getLocation();
                return new CompiledModuleVariableReference(b.myAddress, locn);
            }

            @Override
            Object visit(ProvidedBinding b) throws FusionException
            {
                return b.target().visit(this);
            }

            @Override
            Object visit(RequiredBinding b) throws FusionException
            {
                return b.getProvided().visit(this);
            }
        };

        Binding binding = identifier.getBinding();
        return (CompiledForm) binding.visit(v);
    }


    /**
     * Compile a free variable reference.  These are allowed at top-level but
     * not within a module.
     */
    CompiledForm compileTopReference(final Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final SyntaxSymbol id = (SyntaxSymbol) stx.get(myEval, 1);

        Binding.Visitor v = new Binding.Visitor()
        {
            @Override
            Object visit(Binding b) throws FusionException
            {
                String msg = "Unexpected binding type " + getClass() + " for #%top reference.";
                throw new IllegalStateException(msg);
            }

            @Override
            Object visit(FreeBinding b) throws FusionException
            {
                Namespace.Visitor nv = new Namespace.Visitor()
                {
                    @Override
                    Object accept(TopLevelNamespace ns) throws FusionException
                    {
                        return new CompiledFreeVariableReference(id);
                    }

                    @Override
                    Object accept(ModuleNamespace ns) throws FusionException
                    {
                        String msg = "Unexpected #%top in module: " + id;
                        throw new IllegalStateException(msg);
                    }
                };

                return env.namespace().visit(nv);
            }

            @Override
            Object visit(LocalBinding b) throws FusionException
            {
                String message = "#%top not implemented for local binding.";
                throw new SyntaxException("#%top", message, id);
            }

            @Override
            Object visit(TopLevelDefinedBinding b) throws FusionException
            {
                return new CompiledTopLevelVariableReference(b.myAddress);
            }

            @Override
            Object visit(ModuleDefinedBinding b) throws FusionException
            {
                String message = "#%top not implemented for module binding.";
                throw new SyntaxException("#%top", message, id);
            }

            @Override
            Object visit(RequiredBinding b) throws FusionException
            {
                String message = "#%top not implemented for imported binding.";
                throw new SyntaxException("#%top", message, id);
            }
        };

        Binding binding = id.getBinding();
        return (CompiledForm) binding.visit(v);
    }


    CompiledForm compileSet(final Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final CompiledForm valueForm =
            compileExpression(env, stx.get(myEval, 2));

        Binding.Visitor v = new Binding.Visitor()
        {
            @Override
            Object visit(Binding b) throws FusionException
            {
                String msg =
                    "Unexpected binding type " + b.getClass() + " for `set`.";
                throw new IllegalStateException(msg);
            }

            @Override
            Object visit(LocalBinding b) throws FusionException
            {
                int rib = env.getDepth() - b.myDepth;
                if (rib == 0)
                {
                    return new CompiledImmediateVariableSet(b.myAddress,
                                                            valueForm);
                }
                return new CompiledLocalVariableSet(rib, b.myAddress,
                                                    valueForm);
            }
        };

        SyntaxSymbol id = (SyntaxSymbol) stx.get(myEval, 1);
        Binding binding = id.getBinding();
        return (CompiledForm) binding.visit(v);
    }


    private CompiledForm compileListSemiliteral(Environment env,
                                                SyntaxList  stx)
        throws FusionException
    {
        Object list = stx.unwrap(myEval);

        // Annotations on this form are not handled here.
        assert ! isAnnotated(myEval, list);

        if (isAnyNull(myEval, list))
        {
            return new CompiledConstant(list);
        }

        boolean allConstant = true;

        int len = stx.size();
        CompiledForm[] children = new CompiledForm[len];
        for (int i = 0; i < len; i++)
        {
            SyntaxValue elementExpr = (SyntaxValue)
                unsafeListElement(myEval, list, i);
            CompiledForm child = compileExpression(env, elementExpr);
            children[i] = child;

            allConstant &= (child instanceof CompiledConstant);
        }

        if (allConstant)
        {
            Object[] constChildren = new Object[len];
            for (int i = 0; i < len; i++)
            {
                constChildren[i] = ((CompiledConstant) children[i]).getValue();
            }

            return new CompiledConstant(immutableList(myEval, constChildren));
        }
        else
        {
            return new CompiledList(children);
        }
    }


    private CompiledForm compileStructSemiliteral(final Environment  env,
                                                  final SyntaxStruct stx)
        throws FusionException
    {
        Object struct = stx.unwrap(myEval);

        // Annotations on this form are not handled here.
        assert ! FusionValue.isAnnotated(myEval, struct);

        if (isAnyNull(myEval, struct))
        {
            return new CompiledConstant(nullStruct(myEval));
        }

        int size = FusionStruct.unsafeStructSize(myEval, struct);
        if (size == 0)
        {
            return new CompiledConstant(emptyStruct(myEval));
        }

        final String[]       fieldNames = new String[size];
        final CompiledForm[] fieldForms = new CompiledForm[size];

        final Object[]       constFields = new Object[size];
        final Object    notConstSentinel = new Object();

        StructFieldVisitor visitor = new StructFieldVisitor()
        {
            int i = 0;

            @Override
            public Object visit(String name, Object value)
                throws FusionException
            {
                SyntaxValue child = (SyntaxValue) value;
                CompiledForm form = compileExpression(env, child);

                fieldNames[i] = name;
                fieldForms[i] = form;

                constFields[i] = (form instanceof CompiledConstant
                                    ? ((CompiledConstant) form).getValue()
                                    : notConstSentinel);
                i++;
                return null;
            }
        };

        FusionStruct.unsafeStructFieldVisit(myEval, struct, visitor);

        boolean allConstant = true;
        for (int i = 0; i < size; i++)
        {
            allConstant &= (constFields[i] != notConstSentinel);
        }

        if (allConstant)
        {
            return new CompiledConstant(immutableStruct(fieldNames,
                                                        constFields,
                                                        BaseSymbol.EMPTY_ARRAY));
        }

        if (size == 1) return new CompiledStruct1(fieldNames[0], fieldForms[0]);

        return new CompiledStruct(fieldNames, fieldForms);
    }


    //========================================================================


    private static final class CompiledBegin
        implements CompiledForm
    {
        final CompiledForm[] myBody;

        CompiledBegin(CompiledForm[] body)
        {
            myBody = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            final int last = myBody.length - 1;
            for (int i = 0; i < last; i++)
            {
                CompiledForm form = myBody[i];
                eval.eval(store, form);
            }

            CompiledForm form = myBody[last];
            return eval.bounceTailForm(store, form);
        }
    }


    private static class CompiledPlainApp
        implements CompiledForm
    {
        private final SourceLocation myLocation;
        private final CompiledForm   myProcForm;
        private final CompiledForm[] myArgForms;

        CompiledPlainApp(SourceLocation location,
                         CompiledForm   procForm,
                         CompiledForm[] argForms)
        {
            myLocation = location;
            myProcForm = procForm;
            myArgForms = argForms;
        }

        Object evalArg(Evaluator eval, Store store, int i, CompiledForm arg)
            throws FusionException
        {
            return eval.eval(store, arg, myLocation);
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object proc = eval.eval(store, myProcForm, myLocation);

            int argCount = myArgForms.length;

            Object[] args;
            if (argCount == 0)
            {
                args = FusionUtils.EMPTY_OBJECT_ARRAY;
            }
            else
            {
                args = new Object[argCount];
                for (int i = 0; i < argCount; i++)
                {
                    args[i] = evalArg(eval, store, i, myArgForms[i]);
                }
            }

            Procedure p;
            try
            {
                p = (Procedure) proc;
            }
            catch (ClassCastException e)
            {
                StringBuilder b = new StringBuilder();
                b.append("Application expected procedure, given: ");
                safeWrite(eval, b, proc);
                if (args.length == 0)
                {
                    b.append("\nNo arguments were provided.");
                }
                else
                {
                    b.append("\nArguments were: ");
                    for (Object arg : args)
                    {
                        b.append("\n  ");
                        safeWrite(eval, b, arg);
                    }
                }

                FusionException fe = new FusionException(b.toString());
                fe.addContext(myLocation);
                throw fe;
            }

            return eval.bounceTailCall(myLocation, p, args);
        }
    }


    private static final class CompiledPlainAppWithLocations
        extends CompiledPlainApp
    {
        private final SourceLocation[] myArgLocs;

        CompiledPlainAppWithLocations(SourceLocation   location,
                                      CompiledForm     procForm,
                                      CompiledForm[]   argForms,
                                      SourceLocation[] argLocs)
        {
            super(location, procForm, argForms);
            myArgLocs = argLocs;
        }

        @Override
        Object evalArg(Evaluator eval, Store store, int i, CompiledForm arg)
            throws FusionException
        {
            return eval.eval(store, arg, myArgLocs[i]);
        }
    }


    //========================================================================


    private static final class CompiledList
        implements CompiledForm
    {
        private final CompiledForm[] myChildForms;

        CompiledList(CompiledForm[] childForms)
        {
            myChildForms = childForms;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            int len = myChildForms.length;
            Object[] children = new Object[len];
            for (int i = 0; i < len; i++)
            {
                children[i] = eval.eval(store, myChildForms[i]);
            }

            return immutableList(eval, children);
        }
    }


    private static final class CompiledStruct
        implements CompiledForm
    {
        private final String[]       myFieldNames;
        private final CompiledForm[] myFieldForms;

        CompiledStruct(String[] fieldNames, CompiledForm[] fieldForms)
        {
            myFieldNames = fieldNames;
            myFieldForms = fieldForms;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            FusionStruct.Builder builder = FusionStruct.builder(eval);

            for (int i = 0; i < myFieldNames.length; i++)
            {
                Object value = eval.eval(store, myFieldForms[i]);
                builder.add(myFieldNames[i], value);
            }

            return builder.buildImmutable();
        }
    }


    /**
     * Special-case for single-field {@code {key:expr}}, which can skip a lot of
     * bookkeeping.
     */
    private static final class CompiledStruct1
        implements CompiledForm
    {
        private final String       myFieldName;
        private final CompiledForm myFieldForm;

        CompiledStruct1(String fieldName, CompiledForm fieldForm)
        {
            myFieldName = fieldName;
            myFieldForm = fieldForm;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object value = eval.eval(store, myFieldForm);
            return immutableStruct(eval, myFieldName, value);
        }
    }
}
