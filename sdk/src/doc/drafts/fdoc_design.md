# FusionDoc Design Notes

It’s been noted a few times that Fusion’s documentation features are not themselves documented and
thus not contractual for customers. This is unfortunate and it would be worthwhile to think through
the concerns and see how to make progress toward a supported and stable documentation system.

There’s three aspects at play:

* **Discovery**: The mechanism by which documentation fragments are located in source code.
* **Markup**: The format used by those fragments.
* **Rendering**: How the fragments are assembled into HTML pages or otherwise presented to users.

I’ll document the current state and then discuss my design considerations and what I have seen as
blocking factors to broader use.

## Current State

### **Discovery**

Documentation fragments exist at the *module* level and at the *binding* level. They are discovered
by core syntax forms during compilation (after macro expansion) and by some higher-level forms (ie,
macros) during expansion.

* Code for core-syntax
  `module` [here](/runtime/src/com/amazon/fusion/ModuleForm.java#L386)
* Code for core-syntax
  `define_values` [here](/runtime/src/com/amazon/fusion/Compiler.java#L351)
* Code for core-syntax
  `define_syntax` [here](/runtime/src/com/amazon/fusion/Compiler.java#L407)
* Code for macro
  `define` [here](/fusion/src/fusion/private/define.fusion#L25)
  and [here](/fusion/src/fusion/private/define.fusion#L81)

The relevant grammars are as follows:

`(`**`module`**  *name-id dialect doc-string*? *expr*+ `)`
`(`**`define_values`**  `(` *name-id** `)`  `[` *doc-string** `]`? *expr* `)`
`(`**`define_syntax`**  *name-id* *doc-string*? *expr* `)`
`(`**`define`**  *name-id* *doc-string*? *expr*+ `)`
`(`**`define`**  `(` *name-id* *arg-id** `)` *doc-string*? *expr*+ `)`

In all cases, the documentation takes the form of a literal data, and is the first clause within the
feature being documented, followed by the expression(s) defining the feature. This means that they
can be treated as executable code that have no effect on the application (since they have no side
effects and are discarded in favor of the next expression in the body).

The core forms collect doc fragments only when a specific private continuation mark is present. This
avoids wasting time when running applications.

### Markup

The format of a doc-string is basically just Markdown, as implemented by (a rather old version of)
MarkdownJ. They are used as-is with one exception: the variant of `define` with an implicit lambda —
`(**define** (*fn* *arg* ...) *doc* *body*...)` —
will [prepend](/fusion/src/fusion/private/define.fusion#L98)
a copy of the lambda’s signature to the start of the doc string whenever the given docs don’t start
with a code snippet (specifically, when the string doesn’t start with four spaces, or a newline and
four spaces).

Since the strings are Markdown, it’s possible to include hyperlinks, though linking to specific
Fusion features is verbose and annoying and coupled to the rendering layer.

### Rendering

Generating docs uses an undocumented `document` command in the `fusion` CLI. It walks a given
repository directory and produces a corresponding tree of HTML files. To gather fragments, it loads
every module in the repository *except* those named `private` or submodules therein. It does this
using a `FusionRuntime` that’s used a protected configuration flag to add the private continuation
mark that enables documentation collection.

The HTML generated is quite simplistic; the majority of formatting is what’s done by MarkdownJ based
on the doc fragment markup. There’s some extremely basic CSS at play, but not much because I don’t
really know how to do it. One notable thing is that each binding’s fragment is wrapped in a `div`
with an `id` matching the binding’s symbolic name. These are linked-to by the two index pages, and
can be used to write Markdown cross-reference links as noted above.

In addition to the `*.html` files generated for each `*.fusion` in the repository, any `*.md` files
in the repository are pumped through MarkdownJ and rendered as additional `*.html` files with little
in any decoration.

## Design Considerations

* Fusion’s heavy reliance on hierarchical modules is in part intended to support organization of
  documentation, not just code. In fact, some modules provide no features, just documentation.
* Discovery shouldn’t be built-in to the syntax forms, where it is hard to evolve, extend, or
  override.
* I’d prefer if docs were part of a more general and extensible metadata-annotation mechanism for
  modules and bindings.
* Links to modules and bindings should be easy, and not coupled to the rendering layer.
