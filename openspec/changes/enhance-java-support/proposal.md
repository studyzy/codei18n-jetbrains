# 变更：增强Java及JVM语言支持

## 为什么
目前codei18n插件仅支持Go、Rust和JavaScript/TypeScript，虽然codei18n核心已经支持Java。为了满足广大Java开发者的需求，需要扩展插件以支持Java及其衍生语言（如Kotlin、Scala），使其能够显示中文注释翻译。
此外，需要确保在 RustRover 或 GoLand 等非全功能 Java IDE 中，打开 Java 文件也能通过文本分析提供基本的注释翻译支持。

## 变更内容
- 添加 `java-support.xml` 配置文件，用于全功能 Java IDE。
- 在 `plugin.xml` 中引入Java模块依赖（可选依赖）。
- 为 Java (`JAVA`)、Kotlin (`Kotlin`)、Scala (`Scala`)、Groovy (`Groovy`) 注册 `lang.foldingBuilder` 和 `lang.documentationProvider`。
- 修改 `FileUtils.java` 以支持 Java 相关扩展名。
- 重构 `CommentTranslationFoldingBuilder`，移除 PSI 依赖，支持纯文本模式。
- 在 `plugin.xml` 中显式注册 `Java`, `java`, `Kotlin`, `kotlin`, `TEXT` 等语言 ID 的 `foldingBuilder`，以支持 TextMate Bundle 环境（如 RustRover）。

## 影响
- 受影响规范：新增 `specs/java-support`
- 受影响代码：
    - `src/main/resources/META-INF/plugin.xml`
    - 新增 `src/main/resources/META-INF/java-support.xml`
    - `src/main/java/com/github/studyzy/codei18n/utils/FileUtils.java`
    - `src/main/java/com/github/studyzy/codei18n/providers/CommentTranslationFoldingBuilder.java`
