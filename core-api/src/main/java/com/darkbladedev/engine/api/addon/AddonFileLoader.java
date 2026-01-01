package com.darkbladedev.engine.api.addon;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface AddonFileLoader<T> {

    Collection<T> load(Path rootFolder);

    static List<Path> findFilesRecursively(Path rootFolder, Predicate<Path> include, BiConsumer<Path, IOException> onError) {
        List<Path> files = new ArrayList<>();

        Predicate<Path> includeSafe = include != null ? include : p -> true;

        if (rootFolder == null) {
            if (onError != null) {
                onError.accept(null, new IOException("rootFolder is null"));
            }
            return files;
        }

        if (!Files.exists(rootFolder) || !Files.isDirectory(rootFolder)) {
            return files;
        }

        try {
            Files.walkFileTree(rootFolder, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (Files.isSymbolicLink(dir)) {
                        if (onError != null) {
                            onError.accept(dir, new IOException("Symlink directory skipped"));
                        }
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        if (Files.isSymbolicLink(file)) {
                            if (onError != null) {
                                onError.accept(file, new IOException("Symlink skipped"));
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        if (includeSafe.test(file)) {
                            files.add(file);
                        }
                    } catch (Exception e) {
                        if (onError != null) {
                            onError.accept(file, e instanceof IOException io ? io : new IOException(e));
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    if (onError != null) {
                        onError.accept(file, exc);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            if (onError != null) {
                onError.accept(rootFolder, e);
            }
            return files;
        }

        files.sort(Comparator.comparing(p -> toDeterministicRelativeKey(rootFolder, p)));
        return files;
    }

    static String toDeterministicRelativeKey(Path rootFolder, Path file) {
        try {
            return rootFolder.relativize(file).toString().replace('\\', '/');
        } catch (Exception ignored) {
            return file.toString().replace('\\', '/');
        }
    }
}
