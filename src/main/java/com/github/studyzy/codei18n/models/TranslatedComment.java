package com.github.studyzy.codei18n.models;

public class TranslatedComment {
    private String commentId;
    private String sourceText;
    private String translation;
    private int startOffset;
    private int endOffset;
    private int lineNumber;
    private CommentType commentType;
    private String symbolPath;

    public enum CommentType {
        LINE, BLOCK, DOC
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }

    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }

    public int getStartOffset() { return startOffset; }
    public void setStartOffset(int startOffset) { this.startOffset = startOffset; }

    public int getEndOffset() { return endOffset; }
    public void setEndOffset(int endOffset) { this.endOffset = endOffset; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public CommentType getCommentType() { return commentType; }
    public void setCommentType(CommentType commentType) { this.commentType = commentType; }

    public String getSymbolPath() { return symbolPath; }
    public void setSymbolPath(String symbolPath) { this.symbolPath = symbolPath; }
}
