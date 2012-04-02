// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.IonSexp;
import com.amazon.ion.IonSymbol;
import com.amazon.ion.IonValue;
import com.amazon.ion.ValueFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The core, built-in bindings for Fusion.
 * This is kind-of hacky and will probably be refactored significantly.
 */
class CoreEnvironment
    implements Environment
{
    private final class DefineKeyword
        extends KeywordValue
    {
        private DefineKeyword()
        {
            super("define", "VAR VALUE",
                  "Defines a global variable VAR with the given VALUE.");
        }

        @Override
        FusionValue invoke(Evaluator eval, Environment env, IonSexp expr)
        {
            IonSymbol name = (IonSymbol) expr.get(1);
            IonValue ionValue = expr.get(2);

            FusionValue fusionValue = eval.eval(env, ionValue);
            myBindings.put(name.stringValue(), fusionValue);

            return fusionValue;
        }
    }


    private final ValueFactory myValueFactory;
    private final Map<String,FusionValue> myBindings =
        new HashMap<String,FusionValue>();


    CoreEnvironment(ValueFactory valueFactory)
    {
        myValueFactory = valueFactory;

        bind(new BeginKeyword());
        bind(new DefineKeyword());
        bind(new FuncKeyword());
        bind(new ListBindingsKeyword());
        bind(new IfKeyword());

        myBindings.put("+",    new PlusFunction());
        myBindings.put(".",    new DotFunction());
        myBindings.put("=",    new EqualFunction());
        myBindings.put("add",  new AddFunction());
        myBindings.put("doc",  new DocFunction());
        myBindings.put("exit", new ExitFunction());
        myBindings.put("size", new SizeFunction());
    }

    private void bind(KeywordValue keyword)
    {
        myBindings.put(keyword.getIntrinsicName(), keyword);
    }


    @Override
    public FusionValue lookup(String name)
    {
        return myBindings.get(name);
    }

    @Override
    public void collectNames(Collection<String> names)
    {
        names.addAll(myBindings.keySet());
    }
}
