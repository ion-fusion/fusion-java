# Documentation Ideas

Miscellaneous links, ideas, and inspiration.

# Tools and Best Practices

[**Diátaxis**](https://diataxis.fr/): The Diátaxis framework solves a problem of quality in
technical documentation, describing an information architecture that makes it easier to create,
maintain and use.

**[Quarkdown](https://github.com/iamgio/quarkdown)**: Markdown with scripting, in Kotlin

## Hosting

* https://www.gitbook.com/solutions/open-source
* [**Read the Docs**](https://about.readthedocs.com/): Build, host, and share documentation, all
  with a single platform.

# Inspiring Examples

* [Two open source projects with great documentation • John Jago...](https://johnjago.com/great-docs/)
* [Why is language documentation still so terrible?](https://walnut356.github.io/posts/language-documentation/)
* [Lax Language Tutorials](https://web.archive.org/web/20110811041854/http://drdobbs.com/article/print?articleId=229700183&siteSectionName=):
  Learning a new language too often means working around the tutorials
    * “[A]uthors forget what readers most want to do when learning a new language; namely, to write
      small working programs to familiarize themselves with the syntax. Frequently, tutorials
      present endless tiny snippets of code that illustrate a feature — without showing a single
      useful program.”
* https://tiddlywiki.com/

# What Docs We Need

## First-contact docs

See [GraalVM docs](https://www.graalvm.org/latest/docs/introduction/) for a very nice "first
contact", esp the "what to read next". Though the actual documentation beyond that is poorly
organized and hard to follow for this newbie.

### README

* [README Maturity Model](https://github.com/LappleApple/feedmereadmes/blob/master/README-maturity-model.md)

### CONTRIBUTORS

* Contributor license agreement / copyright assignment?
* [Recognize all contributors](https://allcontributors.org/)
    * Includes GitHub bot

### Code of Conduct

* [Example: Alpine Linux](https://alpinelinux.org/community/code-of-conduct.html)
    * Note the "attribution" section for more good links
* Example: https://www.writethedocs.org/slack/

### CHANGELOG

* [Effective Changelogs](https://xavd.id/blog/post/effective-changelogs)
* [Suggested keywords and icons](https://allcontributors.org/docs/en/emoji-key)

### Org-level README

* [See PayPal for example](https://github.com/paypal)

## Developer docs

“Developer” == writing Fusion code and/or using the embedding APIs

### Learn

* Glossary / conceptual index
* Vision and philosophy
    * Design philosophy
        * Integrity by default
        * sustainability by design
            * Always consider Change over time
            * We won’t always get it right the first time, so make change easy
    * Language as engineering tool
* Style Guide
* Best Practices

### Develop

#### Fusion Language Reference

I really want to experiment with a [TiddlyWiki](https://tiddlywiki.com/)-style dynamic reference
manual. I think this would be effective for Fusion due to the way it provides the same binding from
multiple modules. I have long loved the way TiddlyWiki makes it easy to traverse snippets of
information without getting lost. As a bonus, you can link to *sets of snippets*, really useful when
explaining things to others.

#### Java Embedding Reference

* Document what's a "Fusion object"
    * No promises on toString() etc

### Test

### Deploy

### Evolve

## Contributor docs

“Contributor” == working in the Fusion project itself

* Glossary
* Coding standards, api design guidance, etc
    * Enable EditorConfig support in your IDE
* Design patterns
    * Builders
    * Explain how `check` framework creates an artificial stack trace.
