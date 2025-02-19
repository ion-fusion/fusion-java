// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

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
public class FoldBenchmarks
{

    @State(Scope.Benchmark)
    public static class FoldLeftArgs {
        private Object foldLeft;

        @Param({"0", "1", "2", "3", "4", "10", "20", "40", "80"})
        private int length;
        @Param({"0", "1", "2", "3", "4", "8"})
        private int seqCount;

        private Object listProc;
        private Object[] args;

        @Setup(Level.Trial)
        public void setup(FusionState fusion) throws Exception {
            Random rand = new Random(123456789L + seqCount + length * 9L);
            listProc = fusion.top.lookup("list");
            foldLeft = fusion.top.lookup("fold_left");
            args = new Object[2 + seqCount];
            args[0] = fusion.top.lookup("+");
            args[1] = 0;
            for(int i=0;i<seqCount;i++) {
                Object[] numbers = new Object[length];
                for(int j=0;j<length;j++) {
                    numbers[j] = rand.nextInt(16);
                }
                args[2 + i] = fusion.top.call(listProc, numbers);
            }
        }
    }

    @Benchmark
    public Object callFoldLeft(FusionState fusion, FoldLeftArgs args) throws Exception {
        return fusion.top.call(args.foldLeft, args.args);
    }
}
