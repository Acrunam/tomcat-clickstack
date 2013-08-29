/*
 * Copyright 2010-2013, the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudbees.genapp;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class Files2 {
    final static Set<PosixFilePermission> PERMISSION_R = Collections.unmodifiableSet(PosixFilePermissions.fromString("rw-r-----")); // grant 'w' to owner
    final static Set<PosixFilePermission> PERMISSION_RX = Collections.unmodifiableSet(PosixFilePermissions.fromString("rwxr-x---")); // grant 'w' to owner
    final static Set<PosixFilePermission> PERMISSION_RW = Collections.unmodifiableSet(PosixFilePermissions.fromString("rw-rw----"));
    final static Set<PosixFilePermission> PERMISSION_RWX = Collections.unmodifiableSet(PosixFilePermissions.fromString("rwxrwx---"));
    final static Set<PosixFilePermission> PERMISSION_750 = Collections.unmodifiableSet(PosixFilePermissions.fromString("rwxr-x---"));
    final static Set<PosixFilePermission> PERMISSION_770 = Collections.unmodifiableSet(PosixFilePermissions.fromString("rwxrwx---"));
    final static Set<PosixFilePermission> PERMISSION_640 = Collections.unmodifiableSet(PosixFilePermissions.fromString("rw-r-----"));
    private static final Logger logger = LoggerFactory.getLogger(Files2.class);

    public static void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {

                logger.trace("Delete file: {} ...", file);
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                                                      IOException exc) throws IOException {

                if (exc == null) {
                    logger.trace("Delete dir: {} ...", dir);
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }

        });
    }

    public static void chmodReadOnly(Path path) throws IOException {

        SimpleFileVisitor<Path> setReadOnlyFileVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isDirectory(file)) {
                    throw new IllegalStateException("no dir expected here");
                } else {
                    Files.setPosixFilePermissions(file, PERMISSION_R);
                }
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.setPosixFilePermissions(dir, PERMISSION_RX);
                return super.preVisitDirectory(dir, attrs);
            }
        };
        Files.walkFileTree(path, setReadOnlyFileVisitor);
    }

    public static void chmodReadExecute(Path path) throws IOException {

        SimpleFileVisitor<Path> setReadOnlyFileVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isDirectory(file)) {
                    throw new IllegalStateException("no dir expected here");
                } else {
                    Files.setPosixFilePermissions(file, PERMISSION_RX);
                }
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.setPosixFilePermissions(dir, PERMISSION_RX);
                return super.preVisitDirectory(dir, attrs);
            }
        };
        Files.walkFileTree(path, setReadOnlyFileVisitor);
    }

    public static void chmodReadWrite(Path path) throws IOException {
        SimpleFileVisitor<Path> setReadWriteFileVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isDirectory(file)) {
                    throw new IllegalStateException("no dir expected here");
                } else {
                    Files.setPosixFilePermissions(file, PERMISSION_RW);
                }
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.setPosixFilePermissions(dir, PERMISSION_RWX);
                return super.preVisitDirectory(dir, attrs);
            }
        };
        Files.walkFileTree(path, setReadWriteFileVisitor);
    }

    /**
     * Returns a zip file system
     *
     * @param zipFile to construct the file system from
     * @param create  true if the zip file should be created
     * @return a zip file system
     * @throws java.io.IOException
     */
    private static FileSystem createZipFileSystem(Path zipFile,
                                                  boolean create)
            throws IOException {
        // convert the filename to a URI
        final URI uri = URI.create("jar:file:" + zipFile.toUri().getPath());

        final Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
        }
        return FileSystems.newFileSystem(uri, env);
    }

    /**
     * Unzips the specified zip file to the specified destination directory.
     * Replaces any files in the destination, if they already exist.
     *
     * @param zipFilename the name of the zip file to extract
     * @param destDirname the directory to unzip to
     * @throws java.io.IOException
     */
    public static void unzip(String zipFilename, String destDirname)
            throws IOException {

        Path zipFile = Paths.get(zipFilename);
        Path destDir = Paths.get(destDirname);
        unzip(zipFile, destDir);

    }

    public static void unzip(Path zipFile, final Path destDir) throws IOException {
        //if the destination doesn't exist, create it
        if (Files.notExists(destDir)) {
            logger.trace("Create dir: {}", destDir);
            Files.createDirectories(destDir);
        }

        try (FileSystem zipFileSystem = createZipFileSystem(zipFile, false)) {
            final Path root = zipFileSystem.getPath("/");

            //walk the zip file tree and copy files to the destination
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs) throws IOException {
                    final Path destFile = Paths.get(destDir.toString(), file.toString());
                    logger.trace("Extract file {} to {}", file, destDir);
                    Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs) throws IOException {
                    final Path dirToCreate = Paths.get(destDir.toString(), dir.toString());

                    if (Files.notExists(dirToCreate)) {
                        logger.trace("Create dir {}", dirToCreate);
                        Files.createDirectory(dirToCreate);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    public static void dump(Path path) throws IOException {
        System.out.println("## DUMP FOLDER TREE ##");
        dump(path, 0);
    }

    private static void dump(Path path, int depth) throws IOException {
        depth++;
        String icon = Files.isDirectory(path) ? " + " : " |- ";
        System.out.println(Strings.repeat(" ", depth) + icon + path.getFileName() + "\t" + PosixFilePermissions.toString(Files.getPosixFilePermissions(path)));

        if (Files.isDirectory(path)) {
            DirectoryStream<Path> children = Files.newDirectoryStream(path);
            for (Path child : children) {
                dump(child, depth);
            }
        }
    }

    public static void copyDirectoryContent(final Path fromPath, final Path toPath) throws IOException {
        logger.trace("Copy from {} to {}", fromPath, toPath);

        FileVisitor<Path> copyDirVisitor = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetPath = toPath.resolve(fromPath.relativize(dir));
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, toPath.resolve(fromPath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        };

        Files.walkFileTree(fromPath, copyDirVisitor);
    }

    @Nonnull
    public static Path copyToDirectory(@Nonnull Path source, @Nonnull Path dest) throws IOException {
        Preconditions.checkArgument(Files.isDirectory(dest), "Dest %s is not a directory");
        return Files.copy(source, dest.resolve(source.getFileName()));
    }

    @Nonnull
    public static Path copyArtifactToDirectory(@Nonnull Path sourceDir, @Nonnull String artifactId, @Nonnull Path dest) throws IOException {
        Path source = findArtifact(sourceDir, artifactId);
        return Files.copy(source, dest.resolve(source.getFileName()));
    }

    @Nonnull
    public static Path findArtifact(@Nonnull Path source, @Nonnull final String artifactId) throws IOException {
        return findArtifact(source, artifactId, "jar");
    }

    @Nonnull
    public static Path findArtifact(@Nonnull Path source, @Nonnull final String artifactId, @Nonnull final String type) throws IOException {
        Preconditions.checkArgument(Files.isDirectory(source), "Dest %s is not a directory", source.toAbsolutePath());

        DirectoryStream<Path> paths = Files.newDirectoryStream(source, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                String fileName = entry.getFileName().toString();
                if (fileName.startsWith(artifactId) && fileName.endsWith("." + type)) {
                    return true;
                } else {
                    return false;
                }
            }
        });
        try {
            return Iterables.getOnlyElement(paths);
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("Artifact '" + artifactId + ":" + type + "' not found in path: " + source + ", absolutePath: " + source.toAbsolutePath());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("More than 1 version of artifact '" + artifactId + ":" + type + "' found in path: " + source + ", absolutePath: " + source.toAbsolutePath() + " -> " + paths);
        }
    }

}
