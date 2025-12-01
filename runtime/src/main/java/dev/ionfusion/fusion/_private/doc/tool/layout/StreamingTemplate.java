// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.layout;

import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.site.Generator;
import dev.ionfusion.fusion._private.doc.site.Template;
import java.nio.file.Path;

/**
 * A template that writes to a {@link StreamWriter}, handling automatic
 * creation and closing of the stream.
 *
 * @param <Entity> the type of entity that will populate the template.
 */
public class StreamingTemplate<Entity>
    implements Template<Entity, Path>
{
    private final Template<Entity, StreamWriter> myStreamTemplate;

    public StreamingTemplate(Template<Entity, StreamWriter> template)
    {
        myStreamTemplate = template;
    }


    @Override
    public Generator<Path> populate(Artifact<Entity> artifact)
    {
        return (outFile) ->
        {
            Generator<StreamWriter> streamer = myStreamTemplate.populate(artifact);
            try (StreamWriter writer = new StreamWriter(outFile))
            {
                streamer.generate(writer);
            }
        };
    }
}
