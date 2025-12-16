---
description: "功能实现任务列表模板"
---

# 任务: JetBrains IDE 代码注释国际化显示插件

**输入**: 来自 `/specs/001-jetbrains-comment-i18n-display/` 的设计文档
**前置条件**: plan.md(必需)、spec.md(用户故事必需)、research.md、data-model.md、contracts/

**测试要求**: 
- ⚠️ **强制**: 单元测试覆盖率必须 ≥ 60% (章程原则 III, 不可协商)
- 关键业务逻辑和复杂算法建议达到 80%+ 覆盖率
- 测试必须在实施前编写(TDD 推荐)
- 集成测试和合约测试根据功能需求确定是否需要

**组织结构**: 任务按用户故事分组, 以便每个故事能够独立实施和测试.

## 格式: `[ID] [P?] [Story] 描述`
- **[P]**: 可以并行运行(不同文件, 无依赖关系)
- **[Story]**: 此任务属于哪个用户故事(例如: US1、US2、US3)
- 在描述中包含确切的文件路径

## 路径约定
- **IDE 插件**: `src/main/java`, `src/main/resources`, `src/test/java`

<!--
  ============================================================================
  任务生成依据:
  - spec.md: 用户故事 (P1: 实时显示, P1: 设置, P1: CLI 集成...)
  - plan.md: 技术架构 (IntelliJ Platform SDK, Gradle, CLI Driver)
  - contracts/cli_interface.md: CLI 接口定义
  - research.md: InlayHintsProvider, GeneralCommandLine
  - User Request: 必须包含 Makefile 和 GitHub Action CI
  ============================================================================
-->

## 阶段 1: 设置与基础设施 (Setup & Infra)

**目的**: 初始化 Gradle 项目,配置构建工具,建立 CI/CD 流程

- [x] T001 初始化 Gradle 项目结构 (build.gradle.kts, settings.gradle.kts)
- [x] T002 [P] 创建 Makefile (封装 gradle build, test, runIde 等常用命令)
- [x] T003 [P] 配置 GitHub Actions CI (.github/workflows/ci.yml) 包含构建、测试和 Plugin Verifier
- [x] T004 配置 IntelliJ Platform SDK 和 Go 插件依赖
- [x] T005 [P] 创建项目的基本包结构 (com.github.studyzy.codei18n.*)

---

## 阶段 2: 基础核心服务 (Foundation)

**目的**: 实现与 CLI 通信的核心服务和配置管理,这是所有功能的基础

**⚠️ 关键**: 在此阶段完成之前, 无法开始任何用户故事工作

- [x] T006 [Core] 定义数据模型 (TranslatedComment, CliConfiguration, etc.) 在 `src/main/java/.../models/`
- [x] T007 [Core] 实现 `CliService` 基础框架: 进程调用封装 (ProcessBuilder/GeneralCommandLine)
- [x] T008 [Core] 实现 `CliService` 的 `version` 检查和 `init` 命令调用
- [x] T009 [Core] 实现 `PluginSettings` 持久化配置 (PersistentStateComponent)
- [x] T010 [Core] 创建配置界面 UI (`PluginSettingsConfigurable`, `PluginSettingsPanel`)
- [x] T011 [Core] 实现 `JsonParser` 工具类, 解析 CLI 返回的 JSON 数据
- [x] T012 [Test] 为 `CliService` 编写单元测试 (Mock 外部进程)
- [x] T013 [Test] 为 `PluginSettings` 编写单元测试 (验证持久化)

**检查点**: 基础就绪 - CLI 可以被调用,配置可以保存,CI 流程通过

---

## 阶段 3: 用户故事 1 - 实时替换显示翻译注释 (优先级: P1) 🎯 MVP

**目标**: 打开 Go 文件时,英文注释被中文翻译替换显示,鼠标悬停显示英文原文

**独立测试**: 打开测试 Go 文件,注释显示为中文翻译,鼠标悬停显示英文原文

### 用户故事 1 的测试 (必需 - 覆盖率目标 ≥ 60%)⚠️

- [ ] T014 [P] [US1] 在 `src/test/java/.../providers/` 编写 `CommentTranslationFoldingBuilderTest` (测试折叠逻辑)
- [ ] T015 [P] [US1] 在 `src/test/java/.../services/` 编写 `TranslationServiceTest` (测试缓存和加载逻辑)
- [ ] T015a [P] [US1] 在 `src/test/java/.../providers/` 编写 `CommentDocumentationProviderTest` (测试悬停显示逻辑)

### 用户故事 1 的实施

- [x] T016 [US1] 实现 `CliService.scanFile` 方法: 支持 `--stdin` 参数和 `--with-translations`
- [x] T017 [US1] 实现 `TranslationService`: 管理翻译数据的获取和缓存 (LRU Cache)
- [ ] T018 [US1] 实现 `CommentTranslationFoldingBuilder`: 核心注释折叠逻辑,将英文注释折叠显示为中文翻译
- [ ] T018a [US1] 实现 `CommentDocumentationProvider`: 鼠标悬停时显示英文原文
- [x] T019 [US1] 实现 PSI 解析逻辑: 识别 Go 语言的注释节点 (GoLineComment, GoBlockComment)
- [x] T020 [US1] 实现性能优化: Debounce (防抖) 机制, 避免频繁调用 CLI
- [ ] T021 [US1] 注册 `lang.foldingBuilder` 和 `lang.documentationProvider` 到 `plugin.xml`
- [ ] T021a [US1] 实现折叠状态管理: 默认折叠(显示翻译),支持展开/折叠切换

**检查点**: 打开 Go 文件,CLI 被调用,注释显示为中文翻译,鼠标悬停显示英文

---

## 阶段 4: 用户故事 2 - 插件设置与语言切换 (优先级: P1)

**目标**: 用户可以在设置中切换目标语言,修改 CLI 路径,插件实时响应

**独立测试**: 修改设置中的目标语言为 "en",保存后翻译更新

### 用户故事 2 的实施

- [ ] T022 [US2] 更新 `PluginSettingsPanel`: 添加目标语言选择下拉框, CLI 路径选择器
- [ ] T023 [US2] 实现配置变更监听器 (`SettingsChangeListener` Topic)
- [ ] T024 [US2] 在 `TranslationService` 中监听配置变更, 清空缓存
- [ ] T025 [US2] 实现 `InlayHintsPassFactory.forceHintsUpdateOnNextPass` 触发 UI 刷新

---

## 阶段 5: 用户故事 3 - codei18n CLI 集成与错误处理 (优先级: P1)

**目标**: 当 CLI 不存在或报错时,优雅地提示用户

**独立测试**: 移除 CLI 可执行文件,插件显示通知并引导去设置页面

### 用户故事 3 的实施

- [ ] T026 [US3] 实现 `NotificationService`: 封装 IDE 通知显示 (Error/Warning/Info)
- [ ] T027 [US3] 在 `CliService` 中处理进程异常 (NotFound, Timeout, ExitCode != 0)
- [ ] T028 [US3] 实现 "CLI Not Found" 通知,包含跳转到设置页面的 Action
- [ ] T029 [US3] 实现 "Project Not Initialized" 检测与提示 (检查 .codei18n 目录)

---

## 阶段 6: 增强功能 (用户故事 4, 5, 6)

**目标**: 更多显示模式, 主动翻译操作, 缓存优化

### 用户故事 4 (显示模式)
- [ ] T030 [US4] 实现 Inlay Hint 显示模式 (在注释下方显示翻译)
- [ ] T030a [US4] 实现 Tooltip 显示模式 (仅悬停显示翻译,注释保持英文)
- [ ] T031 [US4] 在设置中添加显示模式切换逻辑 (替换显示/Inlay Hint/Tooltip/Gutter Icon)
- [ ] T031a [US4] 实现模式切换后的动态刷新机制

### 动作集成
- [ ] T032 [Enhance] 实现 `TranslateMissingCommentsAction`: 调用 `codei18n translate`
- [ ] T033 [Enhance] 实现 `UpdateMappingsAction`: 调用 `codei18n map update`
- [ ] T034 [Enhance] 注册 Actions 到 `plugin.xml` (Tools 菜单或右键菜单)

---

## 阶段 N: 完善与横切关注点

**目的**: 文档, 代码清理, 最终质量检查

- [ ] T035 [Doc] 完善 README.md (安装、配置、使用说明)
- [ ] T036 [Quality] 运行代码检查 (IntelliJ Inspections) 并修复警告
- [ ] T037 [Quality] 验证所有测试通过且覆盖率 ≥ 60%
- [ ] T038 [Release] 准备发布包,编写 Release Notes

---

## 依赖关系与执行顺序

### 阶段依赖关系

- **设置(阶段 1)**: 无依赖关系 - 可立即开始
- **基础(阶段 2)**: 依赖于 Gradle 项目结构就绪
- **MVP(阶段 3)**: 依赖于 `CliService` 和 `TranslationService` 基础
- **设置增强(阶段 4)**: 依赖于基础设置 UI
- **错误处理(阶段 5)**: 可与阶段 3 并行,但在发布前必须完成

### 并行机会

- T002 (Makefile) 和 T003 (GitHub Actions) 可以并行
- T014 (InlayHint Test) 和 T015 (Service Test) 可以并行
- T030 (Tooltip) 和 T032 (Actions) 可以并行

---

## 实施策略

### 仅 MVP (阶段 1-3)

1. 完成环境搭建
2. 实现核心 CLI 调用和数据解析
3. 实现 Inlay Hint 显示 (硬编码配置或默认配置)
4. **验证**: 能显示翻译即可

### 完整交付 (所有阶段)

1. 在 MVP 基础上增加配置灵活性
2. 增加错误处理的健壮性
3. 提供辅助 Action (翻译、更新映射)
4. 完善文档和 CI

---

## 注意事项

- **[P]** 任务 = 不同文件, 无依赖关系
- 确保每次提交代码都通过构建和测试
- 遵循 JetBrains 插件开发的线程模型 (ReadAction, EDT, Background Task)
