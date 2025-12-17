package com.github.studyzy.codei18n.models;

import java.util.List;

public record CliResponse(
    String version,
    List<CommentData> comments,
    List<String> errors
) {
    public record CommentData(
        String id,
        String file,
        String language,
        String symbol,
        TextRange range,
        String sourceText,
        String type,  // 注释类型: line, block, doc
        String localizedText  // CLI 返回的字段名是 localizedText
    ) {
        // 为了兼容性,保留 getTranslation 方法
        public String getTranslation() {
            return localizedText;
        }
    }

    public record TextRange(
        int startLine,
        int startCol,
        int endLine,
        int endCol
    ) {}
}
