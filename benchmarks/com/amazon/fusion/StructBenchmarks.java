// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static java.util.UUID.randomUUID;
import com.amazon.ion.util.IonTextUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class StructBenchmarks
{
    @State(Scope.Benchmark)
    public static class ArgState
    {
        private Object structProc;

        @Param({"0", "1", "2", "4", "8", "16", "32", "64", "128", "256"})
        private int      structSize;
        private Object[] args;

        @Setup(Level.Trial)
        public void setup(FusionState fusion)
            throws Exception
        {
            structProc = fusion.top.lookup("struct");

            // Avoid injecting arguments while running the benchmark.
            args = new Object[structSize * 2];
            for (int i = 0; i < args.length; i++)
            {
                String name = randomUUID().toString();
                args[i] = fusion.symbol(name);
            }
        }

        @Setup(Level.Iteration)
        public void shuffleArgs()
        {
            Collections.shuffle(Arrays.asList(args));
        }
    }

    @Benchmark
    public Object callStructProc(FusionState fusion, ArgState args)
        throws Exception
    {
        return fusion.top.call(args.structProc, args.args);
    }


    @State(Scope.Benchmark)
    public static class QuasiLiteralInvocation
    {
        private Object thunkProc;

        @Param({"0", "1", "2", "4", "8", "16", "32", "64", "128", "256"})
        private int structSize;

        @Setup(Level.Trial)
        public void setup(FusionState fusion)
            throws Exception
        {
            // Generate code of the form: (thunk {"K":(identity "K"), ... })
            // The use of `identity` prevents the compiler from constant-folding
            // the entire {} expression into a single constant.

            StringBuilder thunkCode = new StringBuilder("(thunk {");
            for (int i = 0; i < structSize; i++)
            {
                String k = IonTextUtils.printString(randomUUID().toString());
                thunkCode.append(k).append(":(identity ").append(k).append("),");
            }
            thunkCode.append("})");

            thunkProc = fusion.top.eval(thunkCode.toString());
        }
    }

    @Benchmark
    public Object structQuasiLiteral(FusionState fusion, QuasiLiteralInvocation invocation)
        throws Exception
    {
        return fusion.top.call(invocation.thunkProc);
    }



    @State(Scope.Benchmark)
    public static class StructZipInvocation
    {
        private Object proc;

        @Param({"0", "1", "2", "4", "8", "16", "32", "64", "128", "256"})
        private int structSize;
        private Object list;

        @Setup(Level.Trial)
        public void setup(FusionState fusion)
            throws Exception
        {
            proc = fusion.top.lookup("struct_zip");

            Object[] values = new Object[structSize];
            for (int i = 0; i < values.length; i++)
            {
                String name = randomUUID().toString();
                values[i] = fusion.symbol(name);
            }

            list = fusion.top.call("list", values);
        }
    }

    @Benchmark
    public Object structZip(FusionState fusion, StructZipInvocation args)
        throws Exception
    {
        return fusion.top.call(args.proc, args.list, args.list);
    }


    @State(Scope.Benchmark)
    public static class MergeInvocation
    {
        @Param({"8", "32", "128"})
        private int    lSize;
        @Param({"smaller", "larger"})
        private String rSize;
        @Param({"0", "50", "75"})
        private int    lMultiPct;
        @Param({"0", "50", "75"})
        private int    rMultiPct;

        Object proc;
        Object leftStruct;
        Object rightStruct;

        private Object makeStruct(FusionState fusion, int size, int multiPercent)
            throws Exception
        {
            Random rand = new Random();

            Object[] symbols = new Object[size];
            for (int i = 0; i < size; i++)
            {
                if (i != 0 && rand.nextInt(100) < multiPercent)
                {
                    symbols[i] = symbols[rand.nextInt(i)];
                }
                else
                {
                    symbols[i] = fusion.gensym();
                }
            }

            Object list = fusion.top.call("list", symbols);
            return fusion.top.call("struct_zip", list, list);
        }

        public void setup(FusionState fusion, String procName)
            throws Exception
        {
            proc = fusion.top.lookup(procName);

            leftStruct = makeStruct(fusion, lSize, lMultiPct);

            int rightSize = ("smaller".equals(rSize) ? lSize / 2 : lSize * 2);

            rightStruct = makeStruct(fusion, rightSize, rMultiPct);
        }
    }

    @State(Scope.Benchmark)
    public static class Merge1Invocation
        extends MergeInvocation
    {
        @Setup(Level.Trial)
        public void setup(FusionState fusion)
            throws Exception
        {
            super.setup(fusion,"struct_merge1");
        }
    }

    @Benchmark
    public Object merge1(FusionState fusion, Merge1Invocation args)
        throws Exception
    {
        return fusion.top.call(args.proc, args.leftStruct, args.rightStruct);
    }


    @State(Scope.Benchmark)
    public static class MergeMultiInvocation
        extends MergeInvocation
    {
        @Setup(Level.Trial)
        public void setup(FusionState fusion)
            throws Exception
        {
            super.setup(fusion,"struct_merge");
        }
    }

    @Benchmark
    public Object mergeMulti(FusionState fusion, MergeMultiInvocation args)
        throws Exception
    {
        return fusion.top.call(args.proc, args.leftStruct, args.rightStruct);
    }

}
