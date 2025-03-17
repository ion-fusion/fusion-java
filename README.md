<!-- Copyright Ion Fusion contributors. All rights reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

**Ion Fusion** is a programmable programming language for working with JSON and Amazon Ion data.
Its goal is to simplify data processing by eliminating impedance mismatch and enabling
domain-specific custom syntax. Among its interesting characteristics:

* Fusion source code is Ion data, and the Fusion type system is a superset of the Ion data model.
  This reduces glue code such as object binding, and eliminates lossy type conversions.
* While Fusion facilitates general-purpose use, you can use syntactic abstractions to create
custom syntax forms and Domain-Specific Languages, even those that are not as general as the core.

Linguistically, Fusion is a dialect of Scheme, adapted to the Ion notation. Here's a quick sample:

```
(for [(value (in_port))]                 // Read each Ion value from stdin into local var `value`
  (writeln (. value "marketplace_id")))  //   and write its marketplace_id field to stdout
```

Fusion has an interactive command console for experimentation, and a "batch" mode for running
scripts and pipelining data. It can also be embedded into Java applications and services, with
some support for sandboxing untrusted code.

Ion Fusion started inside Amazon in 2012, and has been used in production for over a
decade, driving numerous data processing, workflow management, and analytics systems.
It is now an independent Apache-licensed project led by current and former Amazonians.

> [!IMPORTANT]
> This repository is under active development, preparing for public consumption. The language
> itself is largely stable, but expect significant changes to Java APIs and packaging as we
> renovate everything for public development and use.


# Support

This product is not supported by Amazon or AWS, but by a small group of volunteers.
At this time we cannot promise any particular SLA for responding to issues.

Please submit bug reports and feature requests via [GitHub issues][issues].
If you have questions, feel free to reach out on our [Slack workspace][slack].

[issues]: https://github.com/ion-fusion/fusion-java/issues
[slack]:  https://join.slack.com/t/ion-fusion/shared_invite/zt-2y0jr8vh2-bZLa66hdyZ3ykHcgOcYkcA
