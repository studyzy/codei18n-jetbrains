package com.github.studyzy.codei18n.models;

public record TranslatedComment(
    String commentId,
    String sourceText,
    String translation,
    int startOffset,
    int endOffset,
    int lineNumber,
    CommentType commentType,
    String symbolPath
) {
    public enum CommentType {
        LINE, BLOCK, DOC
    }
}
