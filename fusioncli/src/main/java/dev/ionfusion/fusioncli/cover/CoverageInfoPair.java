// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.cover;

import static java.math.RoundingMode.HALF_EVEN;

import java.io.IOException;
import java.math.BigDecimal;

class CoverageInfoPair
{
    public long coveredExpressions;
    public long uncoveredExpressions;

    CoverageInfoPair()
    {
        coveredExpressions = 0;
        uncoveredExpressions = 0;
    }

    void foundExpression(boolean covered)
    {
        if (covered)
        {
            coveredExpressions++;
        }
        else
        {
            uncoveredExpressions++;
        }
    }

    public long total()
    {
        return coveredExpressions + uncoveredExpressions;
    }

    BigDecimal percentCovered()
    {
        final long total = total();

        if (total == 0) { return BigDecimal.ZERO; }

        BigDecimal numerator = new BigDecimal(coveredExpressions * 100);

        return numerator.divide(new BigDecimal(total), 2, HALF_EVEN);
    }

    void renderCoveragePercentage(HtmlWriter htmlWriter)
        throws IOException
    {
        htmlWriter.append(percentCovered().toString());
        htmlWriter.append("% expression coverage of ");
        htmlWriter.append(Long.toString(total()));
        htmlWriter.append(" expressions observed");
    }

    void renderTotal(HtmlWriter html)
        throws IOException
    {
        html.append(Long.toString(total()));
    }

    void renderPercentageGraph(HtmlWriter html)
        throws IOException
    {
        final BigDecimal percent       = percentCovered();
        final int        percentIntVal = percent.intValue();

        html.append("<table class='percentgraph'>" + "<tr class='percentgraph'>" +
                    "<td class='percentgraphright'>");
        html.append(Integer.toString(percentIntVal));
        html.append("%</td>");
        html.append("<td class='percentgraph'>" + "<div class='percentgraph'>" +
                    "<div class='greenbar' style='width:");
        html.append(Integer.toString(percentIntVal));
        html.append("px'><span class='text'>");
        html.append(Long.toString(coveredExpressions));
        html.append("/");
        renderTotal(html);
        html.append("</span></div></div></td></tr></table>");
    }
}
