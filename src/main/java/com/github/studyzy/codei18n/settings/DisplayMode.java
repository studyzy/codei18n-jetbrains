package com.github.studyzy.codei18n.settings;

public enum DisplayMode {
    FOLDING,       // Collapsed display (English comments are collapsed and displayed as Chinese translation, click to expand to view the original)
    INLAY_HINT,    // Inline hint (display Chinese translation next to the comment)
    TOOLTIP,       // Tooltip (displays translation on mouse hover)
    GUTTER_ICON    // Line number side icon (click to show translation)
}
