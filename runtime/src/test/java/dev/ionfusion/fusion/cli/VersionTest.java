// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import dev.ionfusion.fusion.util.FusionJarInfo;
import com.amazon.ion.IonString;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonTimestamp;
import com.amazon.ion.IonValue;
import com.amazon.ion.Timestamp;
import com.amazon.ion.system.IonSystemBuilder;
import com.amazon.ion.util.JarInfo;
import org.junit.jupiter.api.Test;

public class VersionTest
    extends CliTestCase
{
    @Test
    public void testFusionVersion()
        throws Exception
    {
        IonStruct version = getField(loadVersion(), "fusion_version");

        FusionJarInfo info = new FusionJarInfo();

        assertEquals(info.getReleaseLabel(),
                     getString(version, "release_label"));
    }


    @Test
    public void testIonVersion()
        throws Exception
    {
        IonStruct version = getField(loadVersion(), "ion_version");

        JarInfo info = new JarInfo();

        assertEquals(info.getProjectVersion(),
                     getString(version, "project_version"));
        assertEquals(info.getBuildTime(),
                     getTimestamp(version, "build_time"));
    }


    private IonStruct loadVersion()
        throws Exception
    {
        run("version");

        IonSystem ionSystem = IonSystemBuilder.standard().build();

        // This ensures that the output is pure Ion.
        IonStruct version = (IonStruct) ionSystem.singleValue(stdoutText);
        assertNotNull(version, "No version on stdout");
        return version;
    }


    @SuppressWarnings("unchecked")
    private <T extends IonValue> T getField(IonStruct struct, String fieldName)
    {
        IonValue value = struct.get(fieldName);
        assertNotNull(value, "Missing field " + fieldName + " in " + struct);
        return (T) value;
    }

    private String getString(IonStruct struct, String fieldName)
    {
        IonString value = getField(struct, fieldName);
        return value.stringValue();
    }

    private Timestamp getTimestamp(IonStruct struct, String fieldName)
    {
        IonTimestamp value = getField(struct, fieldName);
        return value.timestampValue();
    }
}
