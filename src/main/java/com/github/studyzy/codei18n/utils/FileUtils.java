package com.github.studyzy.codei18n.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 文件类型工具类
 * 用于统一管理支持的文件类型判断
 */
public class FileUtils {
    
    // 支持的文件扩展名集合
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        ".go",      // Go
        ".rs",      // Rust
        ".js",      // JavaScript
        ".mjs",     // JavaScript Module
        ".cjs",     // CommonJS
        ".ts",      // TypeScript
        ".jsx",     // React JavaScript
        ".tsx"      // React TypeScript
    );
    
    /**
     * 判断文件是否受支持
     * @param filename 文件名
     * @return 是否支持
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
