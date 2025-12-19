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
        String type,  // Comment type: line, block, doc
        String localizedText  // The field name returned by CLI is localizedText
    ) {
        // For compatibility, keep the getTranslation method
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
