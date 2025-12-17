# 实施计划: JetBrains IDE 代码注释国际化显示插件

**分支**: `001-jetbrains-comment-i18n-display` | **日期**: 2025-12-16 | **规范**: [spec.md](./spec.md)
**输入**: 来自 `/specs/001-jetbrains-comment-i18n-display/spec.md` 的功能规范

**注意**: 此模板由 `/speckit.plan` 命令填充. 执行工作流程请参见 `.specify/templates/commands/plan.md`.

## 摘要

本项目为 JetBrains IDE (优先支持 GoLand) 开发一个插件,实现代码注释的实时国际化显示功能。插件使用 Java 语言开发,基于 IntelliJ Platform SDK,通过调用 codei18n CLI 工具获取注释翻译数据,并通过代码折叠(Folding)机制将英文注释替换显示为中文翻译,而不修改源文件。

**主要需求**:
- 实时将 Go 源文件中英文注释替换显示为中文翻译(默认模式)
- 鼠标悬停时显示英文原文,支持展开/折叠切换
- 提供插件设置界面,支持配置 CLI 路径、目标语言、显示模式等
- 集成 codei18n CLI,处理进程调用、超时、错误等场景
- 支持多种显示模式(替换显示、Inlay Hint、Tooltip、Gutter Icon)
- 优化性能,实现翻译缓存,不阻塞 UI 线程
- 支持 GoLand 2023.3+ 及 IntelliJ IDEA Ultimate 2023.3+

**技术方法**:
- 使用 IntelliJ Platform Plugin SDK 开发插件
- 使用 Java 21+ 和 Gradle 构建系统
- 利用 PSI (Program Structure Interface) 解析 Go 源代码
- 通过 ProcessBuilder 调用 codei18n CLI 子进程
- 使用 FoldingBuilder 实现注释的替换显示(默认折叠显示翻译)
- 使用 DocumentationProvider 实现鼠标悬停显示英文原文
- 可选支持 InlayHintsProvider 作为备选显示模式
- 采用异步任务和缓存机制优化性能
- 遵循 JetBrains 插件开发规范,确保通过 Plugin Verifier 验证

## 技术背景

**语言/版本**: Java 21 (推荐, 支持最新IntelliJ Platform特性)
**主要依赖**: 
- IntelliJ Platform SDK 2023.3+
- Gradle 8.11+ (Gradle IntelliJ Plugin)
- JUnit 5 + Mockito (测试框架)
- PSI (Program Structure Interface, 代码解析)
- codei18n CLI v0.1.0+ (外部运行时依赖)

**存储**: 
- IDE 配置持久化 (PersistentStateComponent)
- 内存缓存 (LRU Cache 管理翻译数据)
- 不需要数据库

**测试**: 
- JUnit 5 (Platform Test Framework)
- Mockito (模拟框架)
- IntelliJ Platform Test Fixtures
- Plugin Verifier (兼容性验证)

**目标平台**: 
- GoLand 2023.3+ (优先支持)
- IntelliJ IDEA Ultimate 2023.3+
- 跨平台 (Windows, macOS, Linux)

**项目类型**: IDE 插件 (IntelliJ Platform Plugin)

**性能目标**: 
- 文件打开后 1 秒内完成翻译加载 (<100 条注释)
- UI 响应时间 < 100ms
- CLI 调用超时 5 秒
- 缓存查询 < 10ms
- **输入防抖 (Debounce)**: 300ms-500ms (避免频繁调用 CLI)

**约束条件**: 
- 插件包大小 < 5MB
- 内存占用 < 50MB (缓存 100 文件)
- CPU 占用 < 5%
- 支持 IDE 版本范围: 2023.3 - 2024.3
- 仅支持 Go 语言 (MVP 阶段)

**规模/范围**: 
- 支持单个文件 >1000 条注释
- 支持项目级别的翻译映射管理
- MVP 仅支持英文→中文单向显示
- **CLI 驱动架构**: 核心逻辑 (解析/ID/翻译) 由 CLI 处理, 插件专注 UI 渲染

## 章程检查

*门控: 必须在阶段 0 研究前通过. 阶段 1 设计后重新检查. *

**核心原则验证**:
- [x] JetBrains 插件开发规范: 使用官方 IntelliJ Platform SDK API, 遵循 Plugin Development Guidelines, 将通过 Plugin Verifier 验证
- [x] Java 业界规范: 遵循 Google Java Style Guide, 使用有意义的命名和清晰的异常处理
- [x] 单元测试覆盖率: 计划达到 60% 覆盖率, 核心服务 (CliService, TranslationService) 达到 80%+ 覆盖
- [x] codei18n CLI 封装: 使用 ProcessBuilder/GeneralCommandLine 调用, 处理超时/错误/版本检测, 支持 stdin/stdout 通信 (传入当前缓冲区内容)
- [x] 用户体验: 异步执行 CLI 调用不阻塞 UI, 提供进度通知, 错误消息友好且可操作, 实现 Debounce 机制
- [x] 向后兼容性: 明确支持 GoLand/IntelliJ 2023.3-2024.3, 在 plugin.xml 声明版本范围, 将测试边界版本
- [x] 可观测性: 使用 IntelliJ Platform Logger, 设置 DEBUG/INFO/WARN/ERROR 日志级别, 记录关键操作

**技术约束验证**:
- [x] 技术栈: 使用 Java 21, Gradle 8.11+, JUnit 5 + Mockito, 满足要求
- [x] 性能: UI 操作异步执行, 目标响应 < 100ms, CLI 调用后台线程, 超时 5 秒
- [x] 安全: 验证 CLI 路径防止命令注入, 不记录敏感信息, 配置加密存储 (如需要)

**如有违规**: 无违规项。所有设计符合章程要求。

## 项目结构

### 文档(此功能)

```
specs/[###-feature]/
├── plan.md              # 此文件 (/speckit.plan 命令输出)
├── research.md          # 阶段 0 输出 (/speckit.plan 命令)
├── data-model.md        # 阶段 1 输出 (/speckit.plan 命令)
├── quickstart.md        # 阶段 1 输出 (/speckit.plan 命令)
├── contracts/           # 阶段 1 输出 (/speckit.plan 命令)
└── tasks.md             # 阶段 2 输出 (/speckit.tasks 命令 - 非 /speckit.plan 创建)
```

### 源代码(仓库根目录)

```
codei18n-jetbrains/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── github/
│       │           └── studyzy/
│       │               └── codei18n/
│       │                   ├── settings/          # 插件设置
│       │                   │   ├── PluginSettings.java
│       │                   │   └── PluginSettingsConfigurable.java
│       │                   ├── services/          # 核心服务
│       │                   │   ├── CliService.java
│       │                   │   ├── TranslationService.java
│       │                   │   └── CacheService.java
│       │                   ├── providers/         # UI 提供者
│       │                   │   ├── InlayHintProvider.java
│       │                   │   └── AnnotationProvider.java
│       │                   ├── models/            # 数据模型
│       │                   │   ├── TranslatedComment.java
│       │                   │   └── CliResponse.java
│       │                   ├── utils/             # 工具类
│       │                   │   ├── ProcessExecutor.java
│       │                   │   └── JsonParser.java
│       │                   └── actions/           # 用户操作
│       │                       ├── RefreshTranslationsAction.java
│       │                       ├── TranslateMissingCommentsAction.java
│       │                       └── UpdateMappingsAction.java
│       └── resources/
│           ├── META-INF/
│           │   └── plugin.xml           # 插件配置文件
│           ├── messages/
│           │   └── CodeI18nBundle.properties  # 国际化资源
│           └── icons/
│               └── pluginIcon.svg       # 插件图标
│
├── src/
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── github/
│       │           └── studyzy/
│       │               └── codei18n/
│       │                   ├── services/
│       │                   │   ├── CliServiceTest.java
│       │                   │   └── TranslationServiceTest.java
│       │                   └── providers/
│       │                       └── InlayHintProviderTest.java
│       └── resources/
│           └── testData/                # 测试数据
│               └── sample.go
│
├── build.gradle.kts                     # Gradle 构建脚本
├── gradle.properties                    # Gradle 属性
├── settings.gradle.kts                  # Gradle 设置
└── README.md                            # 项目文档
```

**结构决策**: 
- 采用标准的 IntelliJ Platform Plugin 项目结构
- 使用 Gradle Kotlin DSL 构建系统
- 遵循 Java 包命名约定 (com.github.studyzy.codei18n)
- 分离核心服务、UI 提供者、数据模型和工具类
- 测试代码与主代码镜像结构,便于维护
- 资源文件放置在 resources 目录,包含插件配置和国际化文件

## 复杂度跟踪

*仅在章程检查有必须证明的违规时填写*

| 违规 | 为什么需要 | 拒绝更简单替代方案的原因 |
|-----------|------------|-------------------------------------|
| [例如: 第 4 个项目] | [当前需求] | [为什么 3 个项目不够] |
| [例如: 仓储模式] | [特定问题] | [为什么直接数据库访问不够] |
