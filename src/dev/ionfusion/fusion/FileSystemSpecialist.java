// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * NOT FOR APPLICATION USE!
 *
 * Centralizes access to security-checked file system operations.
 * These exist 1:1 with {@link FusionRuntime}s.
 */
final class FileSystemSpecialist
{
    private final DynamicParameter myCurrentSecurityGuard;
    private final DynamicParameter myCurrentDirectory;

    public FileSystemSpecialist(DynamicParameter currentSecurityGuard,
                                DynamicParameter currentDirectory)
    {
        myCurrentSecurityGuard = currentSecurityGuard;
        myCurrentDirectory     = currentDirectory;
    }


    /**
     * Resolve a relative path against the {@code current_directory} parameter.
     * If file is absolute it is returned as-is.
     *
     * @param who identifies the invoking procedure for error messaging.
     */
    File resolvePath(Evaluator eval, String who, File file)
        throws FusionException
    {
        SecurityGuard guard = myCurrentSecurityGuard.currentValue(eval);
        if (! guard.isFileSystemEnabled())
        {
            throw new FusionErrorException(who + ": Access denied to " + file);
        }

        if (file.isAbsolute()) return file;

        String cdPath = myCurrentDirectory.asString(eval);
        File   cdFile = new File(cdPath);
        return new File(cdFile, file.getPath());
    }


    /**
     * Resolve a relative path against the {@code current_directory} param.
     * If the path is absolute it is returned as-is.
     *
     * @param who identifies the invoking procedure for error messaging.
     */
    File resolvePath(Evaluator eval, String who, String path)
        throws FusionException
    {
        return resolvePath(eval, who, new File(path));
    }


    /**
     * Open a file for reading, resolving relative paths against the
     * {@code current_directory} parameter.
     *
     * @param who identifies the invoking procedure for error messaging.
     */
    InputStream openInputFile(Evaluator eval, String who, File file)
        throws FusionException
    {
        file = resolvePath(eval, who, file);  // Checks the security guard
        try
        {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException e)
        {
            // TODO: Use a more specific exception
            String message = who + ": File not found: " + e.getMessage();
            throw new FusionException(message, e);
        }
    }
}
