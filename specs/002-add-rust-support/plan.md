# 实施计划: Rust 语言注释翻译支持

**分支**: `002-add-rust-support` | **日期**: 2025-12-17 | **规范**: [spec.md](./spec.md)
**输入**: 来自 `/specs/002-add-rust-support/spec.md` 的功能规范

**注意**: 此模板由 `/speckit.plan` 命令填充.

## 摘要

本计划旨在为 codei18n-jetbrains 插件添加 Rust 语言文件的支持。
核心需求是能够在 IDE 中展示 Rust 源代码注释的中英文翻译，所有注释解析、ID生成、翻译均由 codei18n CLI 工具完成。
技术方法是复用现有的 `CommentTranslationFoldingBuilder` 和 `CommentDocumentationProvider`，仅需添加对 `.rs` 文件的识别逻辑。
插件职责：调用 CLI scan 命令、解析 JSON 响应、在 IDE 中渲染翻译结果。

## 技术背景

**语言/版本**: Java 17 (项目当前设置), Rust (目标语言支持)
**主要依赖**:
- `org.jetbrains.plugins.go` (现有)
- `codei18n` CLI (核心，负责 Rust 注释解析和翻译)
**存储**: IDE 内存 (PSI 树), 翻译缓存 (LRU in-memory)
**测试**: JUnit 5, Mockito, IntelliJ Platform Test Framework
**目标平台**: GoLand, IntelliJ IDEA, RustRover (2023.3+)
**项目类型**: IntelliJ Platform Plugin
**性能目标**: CLI 调用 < 5s, UI 响应 < 100ms, 带缓存机制
**约束条件**: 依赖 codei18n CLI 工具，CLI 中已实现 Rust adapter (基于 tree-sitter)

## 章程检查

*门控: 必须在阶段 0 研究前通过. 阶段 1 设计后重新检查. *

**核心原则验证**:
- [x] JetBrains 插件开发规范: 复用现有 `FoldingBuilder` 和 `DocumentationProvider`, 无需新增扩展点.
- [x] Java 业界规范: 将遵循现有代码风格, 修改最小化.
- [x] 单元测试覆盖率: 将编写集成测试验证 Rust 文件支持, 目标 > 60%.
- [x] codei18n CLI 封装: 复用已验证的 `CliService`, CLI 已支持 Rust (adapters/rust/).
- [x] 用户体验: 保持与 Go 版本一致的交互体验 (Hover 显示原文, 折叠显示译文).
- [x] 向后兼容性: `build.gradle.kts` 中定义了版本范围 `233` 到 `243.*`.
- [x] 可观测性: 使用 `com.intellij.openapi.diagnostic.Logger`.

**技术约束验证**:
- [x] 技术栈: Java 17, Gradle Kotlin DSL.
- [x] 性能: 解析使用 `FoldingBuilder` (后台高亮传递), 翻译服务带缓存和去抖动.
- [x] 安全: 无新的安全风险, 仅读取文件内容传递给 CLI.

## 项目结构

### 文档(此功能)

```
specs/002-add-rust-support/
├── plan.md              # 此文件
├── research.md          # 研究决策
├── data-model.md        # 类设计与数据流
├── quickstart.md        # 开发与测试指南
├── contracts/           # 接口定义
└── tasks.md             # 任务列表
```

### 源代码(仓库根目录)

```
src/main/java/com/github/studyzy/codei18n/
├── providers/           # (现有, 语言无关)
│   ├── CommentTranslationFoldingBuilder.java  # 复用, 添加 .rs 文件支持
│   └── CommentDocumentationProvider.java      # 复用, 无需修改
├── services/            # (现有)
│   ├── TranslationService.java  # 复用, 无需修改
│   └── CliService.java          # 复用, 无需修改
└── ...
```

**结构决策**: 
- **无需新增类**，现有 `CommentTranslationFoldingBuilder` 已经是语言无关的设计
- 仅需修改 `isGoFile()` 方法为 `isSupportedFile()`，支持 `.go` 和 `.rs` 文件
- CLI 负责识别文件类型并调用对应的 Rust adapter (已在 CLI 中实现)

## 复杂度跟踪

暂无违规项。
