// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionSequence.BaseSequence;

abstract class SyntaxSequence<Content extends BaseSequence>
    extends SyntaxValue<Content>
{
    SyntaxSequence(Content content, ResourcePosition pos, Object[] properties, SyntaxWraps wraps)
    {
        super(content, wraps, pos, properties);
    }

    SyntaxSequence(Content content, ResourcePosition pos)
    {
        super(content, pos);
    }
}
