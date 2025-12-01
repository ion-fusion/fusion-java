# Ion Fusion

Ion Fusion is a programming language designed to make it easy to work with Amazon Ion data. Among
its interesting characteristics:

* Fusion source code is Ion data, and the Fusion type system is a superset of the Ion data model.
  This reduces impedance mismatch and glue code.
* A highly-expressive core language facilitates general-purpose use.
* Syntactic abstractions enable developers to create custom syntax forms and Domain-Specific
  Languages, even those that are not as general as the core.

Linguistically, Fusion is a dialect of Scheme, adapted to the Ion notation. (A more "C-like" syntax
may be implemented in the future.) Here's a quick sample:

```
([for](/fusion/for.html#for) [(value ([in_port](/fusion/series.html#in_port)))]                 // Iteratively read each Ion value from stdin into local var `value`
  ([writeln](/fusion/io.html#writeln) ([.](/fusion/collection.html#.) value "marketplace_id")))  //   and write its marketplace_id field to stdout
```

See [Fusion Basics](/basics.html) for a more thorough introduction to the language. At present,
Fusion is implemented in **Java SE 8**. It has an interactive command console (for experimentation)
and a "batch" mode (for running programs and pipelining data). It can also be embedded into Java
applications by using the `fusion-java` package.

## Documentation

* The [Fusion documentation site](/index.html) is updated with every release:
    * [Fusion Basics](/basics.html) is a good place to start learning the language.
    * The [Fusion library reference](/fusion.html) documents the available features.
    * The [Java APIs](/javadoc/index.html) are for embedding Fusion in your application.
* If you want to see what good-quality Fusion code looks like, you
  can [browse the standard library code](/fusion/src/fusion).
* Fusion FAQ

## Support

First and foremost, ****[Read The Fine Manual](/index.html)****. When asking a question, please
indicate where the documentation is incomplete or unhelpful, so it can be improved.
Next, check the [Fusion FAQ](FAQ.md).
