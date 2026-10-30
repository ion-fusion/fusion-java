### Benchmarking

These JMH benchmarks do not run as part of the regular build.

To run a benchmark, use the `run-benchmarks` script.
For example, to run one benchmark with a short warmup and measurement:

```shell
./run-benchmarks '.*StructBenchmarks.structZip' -p structSize=1 -wi 1 -i 1 -f 1
```

To see Gradle options, run `./run-benchmarks -H`.

The script is a wrapper around the Gradle target `:benchmarks:jmh`, which configures
common options and accepts run-specific options using `-PjmhArgs=...`.
