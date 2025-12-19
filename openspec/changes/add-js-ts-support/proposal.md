# 提案：增加 JavaScript 和 TypeScript 支持

## 变更ID
add-js-ts-support

## 目的
扩展 Code I18n JetBrains 插件，使其支持 JavaScript 和 TypeScript 源代码文件（.js, .ts, .jsx, .tsx 等）的注释翻译显示。

## 背景
Code I18n CLI 工具已经更新以支持 JS/TS。为了在 IDE 中提供一致的体验，插件侧也需要适配，以便在打开这些类型的文件时能够提取并显示中文注释。

## 范围
- 更新文件监听器以识别 JS/TS 文件。
- 更新注解器（Annotator）以在 JS/TS 注释上显示翻译。
- 更新折叠构建器（FoldingBuilder）以支持 JS/TS 注释折叠。
- 更新内嵌提示（InlayHints）以支持 JS/TS。
- 更新插件配置以注册必要的文件类型支持（如果需要）。

## 关键变更
1.  **代码层面的文件类型检查**：现有的逻辑通过硬编码检查 `.go` 和 `.rs` 后缀。需要将其扩展为支持 JS/TS 相关后缀。
2.  **注释格式处理**：JS/TS 的注释格式（`//`, `/* */`, `/** */`）与现有的处理逻辑兼容，但需要验证 JSDoc 的显示效果。

## 验证计划
- 手动测试：
    - 打开 .js 文件，验证单行和多行注释。
    - 打开 .ts 文件，验证单行和多行注释。
    - 验证 .jsx/.tsx 文件（如果环境支持）。
    - 验证 JSDoc 注释的显示。
