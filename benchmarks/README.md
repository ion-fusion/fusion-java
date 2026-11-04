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

Results are copied into `tmp/benchmarks` with names that include the commit id (or the
change ID, when using Jujutsu).

Unlabeled arguments are passed to JMH which assumes them to be regexps to be matched
against all available benchmark names.  As a shortcut, the `-s` argument provides a
substring to match, more or less: `-s SUB` is passed through to JMH as `.*SUB.*`.

To compare benchmarks across two commits, use the `-c` argument to specify them.
Each commit is checked out for a clean build before running the benchmarks.
