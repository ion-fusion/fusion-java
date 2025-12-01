// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Generates content to some destination.
 * <p>
 * The main point of this interface, as distinct from {@link Consumer}, is to
 * allow {@link IOException}.
 *
 * @param <Destination> where/how to generate content.
 */
@FunctionalInterface
public interface Generator <Destination>
{
    /**
     * Renders content to the destination.
     *
     * @param dest additional context needed to generate the artifact.
     *
     * @throws IOException if there's a problem generating the artifact.
     */
    void generate(Destination dest)
        throws IOException;
}
