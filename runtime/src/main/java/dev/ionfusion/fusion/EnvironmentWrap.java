// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import java.util.Iterator;
import java.util.Set;

/**
 * Syntax wrap that adds all bindings from a specific environment.
 */
class EnvironmentWrap
    extends SyntaxWrap
{
    private final Environment myEnvironment;

    EnvironmentWrap(Environment environment)
    {
        myEnvironment = environment;
    }

    Environment getEnvironment()
    {
        return myEnvironment;
    }

    @Override
    Binding resolveMaybe(BaseSymbol name,
                         Iterator<SyntaxWrap> moreWraps,
                         Set<MarkWrap> returnMarks)
    {
        Binding b;
        if (moreWraps.hasNext())
        {
            SyntaxWrap nextWrap = moreWraps.next();
            b = nextWrap.resolveMaybe(name, moreWraps, returnMarks);
            if (b != null)
            {
                return myEnvironment.substitute(b, returnMarks);
            }
        }

        // The identifier doesn't have a binding outside of this environment,
        // so look for one here.
        Binding subst = myEnvironment.substituteFree(name, returnMarks);
        return subst;
    }

    @Override
    public String toString()
    {
        return "{{{Environment renames for " + myEnvironment + "}}}";
    }
}
