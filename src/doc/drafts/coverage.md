FusionJava includes a built-in tool for measuring coverage of Fusion code. It consists of two parts:

* Metrics collection while Fusion code is running, usually during unit tests.
* Report generation based off the collected metrics.

## **Build-time Configuration**

By default, coverage metrics will be collected for all repository-loaded modules *except* those
within the `/fusion` hierarchy, and the HTML report will be a build artifact under `docs/fcoverage`.

### **Configuring Instrumented Code**

Fusion code that can be instrumented for code-coverage metrics falls into two buckets:

* Module code that resides in a (file-based) Fusion repository.
* Fusion "script" files evaluated via `load` (or equivalent Java APIs).

The two buckets are configured via separate include/exclude declarations, and code is instrumented
if it passes either one.

* The `fusion.coverage.included.modules` and `fusion.coverage.excluded.modules` Ant properties
  select repository-based modules for instrumentation. They each hold a sequence of absolute module
  paths, separated by `":"`. A module is instrumented if it is selected by the includes and *not*
  selected by the excludes. If the included-module property is not defined, all modules are
  included; if it is empty then no modules are included. If the excluded-module property is not
  defined or is empty, then all included modules are instrumented.
* The `fusion.coverage.included.sources` Ant property selects loaded scripts for instrumentation. It
  holds a sequence of directory paths, separated by the platform path separator. All code loaded
  from files under a listed directory are instrumented. If this list is not defined or is empty,
  then no scripts are instrumented.

In both cases, "selection" is hierarchical: a module (or script directory) is selected if any parent
module (or directory) is declared.

Note that the default value of `fusion.coverage.excluded.modules` is `"/fusion"`, so if you want to
exclude others then you'll need to declare `"/fusion"` as well or you'll get reports on the standard
libraries. Add something like this to your `build.xml`:

```
<property name="fusion.coverage.excluded.modules"
    value="/fusion:/amazon/mypackage/examples" />
```

### **Configuring Report Output**

The coverage report takes the form of a directory containing a static HTML website, including an
`index.html` file. The entire directory will be deleted and recreated by the reporting tool.

* `fusion.coverage.report.dir` is a path to the directory into which the coverage report is written.
  The default value of this property is `build/reports/fcoverage`.

## **Programmatic Configuration**

The build-time features described above leverage programmatic features that can be used directly.

From an API perspective, coverage instrumentation is configured at the `FusionRuntime` level. You
use `FusionRuntimeBuilder.setCoverageDataDirectory()` to declare a data directory, put a config file
there, and then build one or more `FusionRuntime`s. The runtime will instrument the desired code and
then write the results into the directory. Results aggregate within a directory, and you can create
multiple runtimes in the same JVM instance, or in multiple sequential instances (though not two JVMs
simultaneously).

More precisely: the data directory is mapped to a unique "collector" object that can be used from
multiple runtimes simultaneously or sequentially. The collector reads its state from the directory
when its constructed, collects metrics as Fusion code is run by associated runtimes, and then writes
its state to the directory when no more runtimes need it.


> Note: To guarantee that the metrics are persisted without manual intervention, the library will
> start a cleanup thread (to write collected metrics when the collector is garbage-collected) and
> install a shutdown hook (to write metrics before the JVM terminates).


The includes/excludes features described above must be configured via a `config.properties` file
inside the data directory. The following properties are observed and have the same semantics as the
corresponding Ant properties.

* `IncludedModules`
* `ExcludedModules`
* `IncludedSources`

To generate an HTML report, use the `fusion` CLI command `report_coverage`, passing the data
directory and the report directory. There are currently no APIs for generating the HTML report, nor
is the content of the data directory supported for custom reporting.
