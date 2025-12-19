package com.github.studyzy.codei18n.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * File type utility class
 * Used for unified management of supported file type determination
 */
public class FileUtils {
    
    // Supported file extension collection
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        ".go",      // Go
        ".rs",      // Rust
        ".js",      // JavaScript
        ".mjs",     // JavaScript Module
        ".cjs",     // CommonJS
        ".ts",      // TypeScript
        ".jsx",     // React JavaScript
        ".tsx",     // React TypeScript
        ".java",    // Java
        ".kt",      // Kotlin
        ".scala",   // Scala
        ".groovy"   // Groovy
    );
    
    /**
     * Determines if the file is supported
     * @param filename The filename
     * @return Whether it is supported
     */
    public static boolean isSupportedFile(@Nullable String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        String lowerName = filename.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        
        return false;
    }
}
