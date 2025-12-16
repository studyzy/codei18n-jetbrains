package com.github.studyzy.codei18n.settings;

public enum DisplayMode {
    FOLDING,       // 折叠显示 (英文注释折叠显示为中文翻译，点击展开查看原文)
    INLAY_HINT,    // 内联提示 (注释旁边显示中文翻译)
    TOOLTIP,       // 工具提示 (鼠标悬停显示翻译)
    GUTTER_ICON    // 行号旁图标 (点击显示翻译)
}
