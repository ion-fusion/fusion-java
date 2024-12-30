// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.util.FusionJarInfo;
import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonTextWriterBuilder;
import com.amazon.ion.util.JarInfo;
import java.io.IOException;
import java.io.PrintWriter;

class Version
    extends Command
{
    private static final String HELP_ONE_LINER =
        "Writes version information about this program.";

    private static final String HELP_USAGE =
        "version";

    private static final String HELP_BODY =
        "Writes this program's version and build information to standard output, in Ion\n" +
        "format.";


    //=========================================================================
    // Constructors

    Version()
    {
        super("version");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }


    //=========================================================================


    @Override
    Executor makeExecutor(GlobalOptions globals, String[] args)
    {
        return new Executor(globals);
    }


    private static class Executor
        extends StdioExecutor
    {
        private Executor(GlobalOptions globals)
        {
            super(globals);
        }


        @Override
        public int execute(PrintWriter out, PrintWriter err)
            throws IOException
        {
            IonTextWriterBuilder b = IonTextWriterBuilder.pretty();
            b.setCharset(IonTextWriterBuilder.ASCII);

            IonWriter w = b.build(out);
            w.stepIn(IonType.STRUCT);
            {
                emitFusionVersion(w);
                emitIonVersion(w);
            }
            w.stepOut();
            w.finish();
            out.println();

            return 0;
        }


        private void emitFusionVersion(IonWriter w)
            throws IOException
        {
            try
            {
                FusionJarInfo fusionInfo = new FusionJarInfo();

                w.setFieldName("fusion_version");
                w.stepIn(IonType.STRUCT);
                {
                    w.setFieldName("release_label");
                    w.writeString(fusionInfo.getReleaseLabel());
                }
                w.stepOut();
            }
            catch (FusionException e)
            {
                w.setFieldName("error");
                w.writeString(e.toString());
            }
        }

        private void emitIonVersion(IonWriter w)
            throws IOException
        {
            try
            {
                JarInfo ionInfo = new JarInfo();

                w.setFieldName("ion_version");
                w.stepIn(IonType.STRUCT);
                {
                    w.setFieldName("project_version");
                    w.writeString(ionInfo.getProjectVersion());

                    w.setFieldName("build_time");
                    w.writeTimestamp(ionInfo.getBuildTime());
                }
                w.stepOut();
            }
            catch (IonException e)
            {
                w.setFieldName("error");
                w.writeString(e.toString());
            }
        }
    }
}
