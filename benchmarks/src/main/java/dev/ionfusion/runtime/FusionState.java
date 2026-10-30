// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime;

import static java.util.UUID.randomUUID;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.embed.FusionRuntime;
import dev.ionfusion.runtime.embed.FusionRuntimeBuilder;
import dev.ionfusion.runtime.embed.TopLevel;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class FusionState
{
    public FusionRuntime runtime;
    public TopLevel      top;
    public Object        stringToSymbolProc;

    @Setup(Level.Trial)
    public void setup()
        throws Exception
    {
        runtime = FusionRuntimeBuilder.standard().build();
        top     = runtime.getDefaultTopLevel();
        top.requireModule("/fusion/experimental/struct");

        stringToSymbolProc = top.lookup("string_to_symbol");
    }

    public Object symbol(String content)
        throws FusionException
    {
        return top.call(stringToSymbolProc, content);
    }

    public Object gensym()
        throws FusionException
    {
        return symbol(randomUUID().toString());
    }
}
