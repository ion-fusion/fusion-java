// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.cover;

import dev.ionfusion.fusion._private.HtmlWriter;
import java.io.IOException;

class EstimatedCoverageInfoPair
    extends CoverageInfoPair
{
    @Override
    void renderTotal(HtmlWriter html)
        throws IOException
    {
        html.append("???");
    }
}
