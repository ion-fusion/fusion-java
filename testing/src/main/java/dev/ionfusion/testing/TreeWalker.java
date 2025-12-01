// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.testing;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.function.ThrowingConsumer;

/**
 * Generates test cases from files found under a base directory.
 * Both directories and files can be filtered by predicates.
 */
public class TreeWalker
{
    private final Path                   myBase;
    private final Predicate<Path>        myDirPredicate;
    private final Predicate<Path>        myFilePredicate;
    private final ThrowingConsumer<Path> myFileHandler;

    public TreeWalker(Path baseDir,
                      Predicate<Path> dirPredicate,
                      Predicate<Path> filePredicate,
                      ThrowingConsumer<Path> fileHandler)
    {
        myBase = baseDir;
        myDirPredicate = dirPredicate;
        myFilePredicate = filePredicate;
        myFileHandler = fileHandler;
    }

    public static Stream<DynamicNode> walk(Path baseDir,
                                           Predicate<Path> dirPredicate,
                                           Predicate<Path> filePredicate,
                                           ThrowingConsumer<Path> fileHandler)
    {
        return new TreeWalker(baseDir, dirPredicate, filePredicate, fileHandler).walk();
    }


    public Stream<DynamicNode> walk()
    {
        return forDir(Paths.get(""));
    }

    private Stream<DynamicNode> forDir(Path dir)
    {
        if (!myDirPredicate.test(dir)) { return Stream.empty(); }

        Path     resolved  = myBase.resolve(dir);
        String[] fileNames = resolved.toFile().list();
        if (fileNames == null)
        {
            throw new IllegalArgumentException("Not a directory: " + resolved.toAbsolutePath());
        }

        // Sort the fileNames so they are listed in order.
        // This is not a functional requirement, but it helps humans scanning
        // the output looking for a specific file.
        Arrays.sort(fileNames);

        return Arrays.stream(fileNames)
                     .map(n -> forChild(dir.resolve(n)))
                     .filter(Objects::nonNull);
    }

    private DynamicNode forChild(Path file)
    {
        Path   resolved = myBase.resolve(file);
        String name     = resolved.getFileName().toString();
        URI    uri      = resolved.toUri();

        if (Files.isDirectory(resolved))
        {
            Stream<DynamicNode> nodes = forDir(file);
            return dynamicContainer(name + "/", uri, nodes);
        }

        if (myFilePredicate.test(file))
        {
            return dynamicTest(name, uri, () -> myFileHandler.accept(resolved));
        }

        return null;
    }
}
