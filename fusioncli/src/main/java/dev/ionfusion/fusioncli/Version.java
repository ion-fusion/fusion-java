// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonTextWriterBuilder;
import dev.ionfusion.fusioncli.framework.Command;
import dev.ionfusion.runtime.base.JarInfo;
import dev.ionfusion.runtime.embed.FusionRuntime;
import java.io.IOException;
import java.io.PrintWriter;

class Version
    extends Command<GlobalOptions>
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
    public Executor makeExecutor(GlobalOptions globals, String[] args)
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
            JarInfo fusionInfo = FusionRuntime.jarInfo();

            w.setFieldName("fusion_version");
            w.stepIn(IonType.STRUCT);
            {
                w.setFieldName("release_label");
                w.writeString(fusionInfo.getVersion());

                w.setFieldName("build_time");
                w.writeTimestamp(fusionInfo.getBuildDate());

                w.setFieldName("commit_hash");
                w.writeString(fusionInfo.getLongCommitHash());

                w.setFieldName("repo_status");
                w.writeString(fusionInfo.getRepositoryStatus());
            }
            w.stepOut();
        }

        private void emitIonVersion(IonWriter w)
            throws IOException
        {
            try
            {
                // Qualified: ion-java has a `JarInfo` of its own, which is where
                // ours was originally modeled from.
                com.amazon.ion.util.JarInfo ionInfo =
                    new com.amazon.ion.util.JarInfo();

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
