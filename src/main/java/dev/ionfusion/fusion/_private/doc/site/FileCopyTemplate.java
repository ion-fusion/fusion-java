// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileCopyTemplate
    implements Template<Path, Path>
{
    @Override
    public Generator<Path> populate(Artifact<Path> artifact)
    {
        return dest -> {
            Files.copy(artifact.getEntity(), dest, REPLACE_EXISTING);
        };
    }
}
