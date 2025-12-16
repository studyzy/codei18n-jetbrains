package com.github.studyzy.codei18n.models;

import java.util.List;

public class CliResponse {
    private String version;
    private List<CommentData> comments;
    private List<String> errors;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<CommentData> getComments() { return comments; }
    public void setComments(List<CommentData> comments) { this.comments = comments; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public static class CommentData {
        private String id;
        private String file;
        private String language;
        private String symbol;
        private TextRange range;
        private String sourceText;
        private String localizedText;  // CLI 返回的字段名是 localizedText

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public TextRange getRange() { return range; }
        public void setRange(TextRange range) { this.range = range; }

        public String getSourceText() { return sourceText; }
        public void setSourceText(String sourceText) { this.sourceText = sourceText; }

        public String getLocalizedText() { return localizedText; }
        public void setLocalizedText(String localizedText) { this.localizedText = localizedText; }
        
        // 为了兼容性，保留 getTranslation 方法
        public String getTranslation() { return localizedText; }
    }

    public static class TextRange {
        private int startLine;
        private int startCol;
        private int endLine;
        private int endCol;

        public int getStartLine() { return startLine; }
        public void setStartLine(int startLine) { this.startLine = startLine; }

        public int getStartCol() { return startCol; }
        public void setStartCol(int startCol) { this.startCol = startCol; }

        public int getEndLine() { return endLine; }
        public void setEndLine(int endLine) { this.endLine = endLine; }

        public int getEndCol() { return endCol; }
        public void setEndCol(int endCol) { this.endCol = endCol; }
    }
}
