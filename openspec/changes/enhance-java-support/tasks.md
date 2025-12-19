## 1. 实施
- [x] 1.1 创建 `src/main/resources/META-INF/java-support.xml` 文件，定义 Java、Kotlin、Scala、Groovy 的扩展点。
- [x] 1.2 修改 `src/main/resources/META-INF/plugin.xml`，添加对 `com.intellij.java` 的可选依赖，并引入 `java-support.xml`。
- [x] 1.3 验证 XML 配置格式正确。
- [x] 1.4 修改 `src/main/java/com/github/studyzy/codei18n/utils/FileUtils.java`，添加 Java 及 JVM 语言的文件扩展名支持。
- [x] 1.5 修改 `src/main/java/com/github/studyzy/codei18n/providers/CommentTranslationFoldingBuilder.java`，移除对 `PsiRecursiveElementVisitor` 的依赖，直接使用 TranslationService 的结果，并注册 `TEXT` 语言支持。
- [x] 1.6 在 `plugin.xml` 中为 Java、Kotlin、Scala、Groovy 显式注册多种语言 ID 变体（大小写）以支持不同 IDE 环境。
- [x] 1.7 添加详细的调试日志以诊断折叠功能问题。
- [x] 1.8 创建 DEBUG_STEPS.md 文档指导用户调试。

## 2. 验证
- [ ] 2.1 在 RustRover 中打开 Java 文件，验证注释是否显示为中文
- [ ] 2.2 检查 IDE 日志，确认 FoldingBuilder 被正确调用
- [ ] 2.3 验证插件设置是否正确（Enabled + FOLDING 模式）
