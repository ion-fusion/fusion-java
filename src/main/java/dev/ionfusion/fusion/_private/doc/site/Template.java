// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

/**
 * A template is a means of generating an artifact. It must first be populated
 * with an artifact to produce a generator.
 *
 * @param <Entity> the type of entity providing page content.
 * @param <Destination> where/how to generate content.
 */
@FunctionalInterface
public interface Template <Entity, Destination>
{
    Generator<Destination> populate(Artifact<Entity> artifact);
}
