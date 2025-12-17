---
description: "Rust 语言注释翻译支持任务列表"
---

# 任务: Rust 语言注释翻译支持

**输入**: 来自 `/specs/002-add-rust-support/` 的设计文档
**关键认知**: 插件只是 codei18n CLI 的 GUI 前端，CLI 已实现 Rust 支持（tree-sitter）

**测试要求**: 
- ⚠️ **强制**: 单元测试覆盖率必须 ≥ 60%
- 集成测试验证 Rust 文件完整流程

**组织结构**: 任务极度简化，因为无需新增类，只需修改 1 个方法。

## 格式: `[ID] [P?] 描述`
- **[P]**: 可以并行运行(不同文件, 无依赖关系)
- 在描述中包含确切的文件路径

---

## 阶段 1: 环境准备与验证 (约 30 分钟)

**目的**: 验证 CLI Rust 支持和测试环境

- [ ] T001 验证 codei18n CLI 已支持 Rust
  - 运行: `codei18n version`
  - 检查 `adapters/rust/` 是否存在（在 CLI 源码中）
  - 创建测试 Rust 文件并运行扫描验证
  - 路径: 参考 `quickstart.md` 中的验证步骤

- [ ] T002 [P] 在项目中初始化 codei18n 配置
  - 运行: `codei18n init --source-lang en --local-lang zh-CN`
  - 验证: `.codei18n/config.json` 已创建
  - 测试: `codei18n scan --file test.rs --format json`

- [ ] T003 [P] 创建测试数据文件
  - 路径: `test_data/sample.rs`
  - 内容: 包含各种 Rust 注释类型（`//`, `///`, `//!`, `/* */`）
  - 参考: `quickstart.md` 中的示例

**检查点**: CLI 环境就绪，能够扫描 Rust 文件并返回 JSON

---

## 阶段 2: 核心修改 (约 1-2 小时)🎯 MVP

**目的**: 扩展文件类型支持

### 2.1 代码修改

- [ ] T004 修改 CommentTranslationFoldingBuilder 支持 .rs 文件
  - 路径: `src/main/java/com/github/studyzy/codei18n/providers/CommentTranslationFoldingBuilder.java`
  - 修改内容:
    ```java
    // 找到方法 isGoFile(PsiElement root)
    // 重命名为 isSupportedFile(PsiElement root)
    // 修改实现:
    private boolean isSupportedFile(PsiElement root) {
        String filename = root.getContainingFile().getName();
        return filename.endsWith(".go") || filename.endsWith(".rs");
    }
    
    // 更新调用点（同一文件内，buildFoldRegions 方法中）
    // 将 isGoFile(root) 改为 isSupportedFile(root)
    ```
  - 添加日志: 记录识别到 Rust 文件

### 2.2 测试编写（TDD）

- [ ] T005 [P] 编写单元测试 - 文件类型识别
  - 路径: `src/test/java/com/github/studyzy/codei18n/providers/CommentTranslationFoldingBuilderTest.java`
  - 测试场景:
    - ✅ `.rs` 文件返回 true
    - ✅ `.go` 文件返回 true（向后兼容）
    - ✅ `.java` 文件返回 false
    - ✅ 空文件名处理

- [ ] T006 [P] 编写集成测试 - Rust 文件支持
  - 路径: `src/test/java/com/github/studyzy/codei18n/integration/RustSupportIntegrationTest.java`
  - Mock CLI 返回 Rust 文件的 JSON 响应
  - 测试场景:
    - ✅ 解析 Rust 文件的 CLI JSON
    - ✅ 创建折叠描述符
    - ✅ 缓存翻译数据
    - ✅ 文档提供者显示原文

- [ ] T007 [P] 编写集成测试 - 混合项目支持
  - 路径: `src/test/java/com/github/studyzy/codei18n/integration/MixedProjectTest.java`
  - 测试场景:
    - ✅ Go + Rust 文件共存
    - ✅ 缓存隔离（不同文件类型）
    - ✅ 翻译服务处理多语言文件

**检查点**: 核心功能完成，测试覆盖 ≥ 60%

---

## 阶段 3: 验证与优化 (约 1-2 小时)

**目的**: 端到端验证和性能测试

- [ ] T008 手动测试 - 沙盒 IDE 验证
  - 运行: `./gradlew runIde`
  - 在沙盒 IDE 中打开 `test_data/sample.rs`
  - 验证:
    - ✅ 注释自动折叠显示中文翻译
    - ✅ 鼠标悬停显示英文原文
    - ✅ 点击展开/折叠功能正常
  - 参考: `quickstart.md` 中的验证步骤

- [ ] T009 手动测试 - Go 文件向后兼容性
  - 在沙盒 IDE 中打开 `.go` 文件
  - 验证 Go 翻译功能仍然正常工作
  - 确保无回归

- [ ] T010 性能测试 - 大文件处理
  - 创建包含 1000 个注释的 Rust 文件
  - 测试 CLI 扫描时间: `time codei18n scan --file large.rs --with-translations`
  - 目标: < 5 秒
  - 测试 IDE 渲染响应时间: < 100ms

- [ ] T011 [P] 更新文档
  - 路径: `README.md`
  - 添加 Rust 支持说明
  - 更新支持的语言列表: Go, Rust
  - 添加 Rust 使用示例

- [ ] T012 [P] 验证测试覆盖率
  - 运行: `./gradlew test jacocoTestReport`
  - 检查: `build/reports/jacoco/test/html/index.html`
  - 确保: 整体覆盖率 ≥ 60%
  - 补充测试（如需要）

**检查点**: 功能完整验证，性能达标，文档完善

---

## 阶段 4: 发布准备 (约 30 分钟)

**目的**: 代码审查和质量门控

- [ ] T013 代码审查检查
  - 检查代码符合 Google Java 代码规范
  - 确保所有方法有 Javadoc 注释
  - 移除调试代码和日志
  - 确保没有魔法数字

- [ ] T014 运行完整 CI 检查
  - 运行: `./gradlew check`
  - 运行: `./gradlew verifyPlugin`
  - 确保所有检查通过

- [ ] T015 更新变更日志
  - 路径: `CHANGELOG.md` 或发布说明
  - 记录: "Added Rust language support for comment translation"
  - 说明: 通过 CLI 集成，支持所有 Rust 注释类型

**检查点**: 准备发布，所有质量门控通过

---

## 依赖关系与执行顺序

### 阶段依赖

```
阶段 1 (环境准备)
    ↓
阶段 2 (核心修改) ← MVP 核心
    ↓
阶段 3 (验证优化)
    ↓
阶段 4 (发布准备)
```

### 任务依赖

**阶段 1 内部**:
- T001, T002, T003 可并行执行 [P]

**阶段 2 内部**:
- T004 必须先完成（代码修改）
- T005, T006, T007 可在 T004 完成后并行编写 [P]

**阶段 3 内部**:
- T008, T009 必须顺序执行（手动测试）
- T010, T011, T012 可并行执行 [P]

**阶段 4 内部**:
- T013, T014 可并行执行 [P]
- T015 在 T014 后执行

---

## 并行机会

**阶段 1 - 环境准备**: 3 个任务可并行
```bash
# 终端 1: 验证 CLI
任务 T001: codei18n version

# 终端 2: 初始化项目
任务 T002: codei18n init

# 终端 3: 创建测试文件
任务 T003: 编写 sample.rs
```

**阶段 2 - 测试编写**: 3 个测试可并行
```bash
# 开发人员或 AI 并行编写:
任务 T005: 单元测试 - 文件类型
任务 T006: 集成测试 - Rust 支持
任务 T007: 集成测试 - 混合项目
```

**阶段 3 - 验证**: 部分任务可并行
```bash
# 并行进行:
任务 T010: 性能测试
任务 T011: 更新文档
任务 T012: 验证覆盖率
```

---

## 实施策略

### MVP 最小交付（最快路径）

**目标**: 2-3 小时完成核心功能

```bash
# 步骤 1: 验证 CLI (15分钟)
T001 → 验证 CLI Rust 支持

# 步骤 2: 核心修改 (30分钟)
T004 → 修改 isSupportedFile() 方法

# 步骤 3: 基础测试 (1小时)
T005 → 编写单元测试
T006 → 编写集成测试

# 步骤 4: 手动验证 (30分钟)
T008 → 沙盒 IDE 验证
T009 → Go 兼容性验证

# 完成！可发布基础版本
```

### 完整交付（推荐）

**目标**: 4-6 小时完成所有任务

```bash
# 阶段 1: 准备 (30分钟)
T001 + T002 + T003 并行

# 阶段 2: 开发 (1-2小时)
T004 → T005 + T006 + T007 并行

# 阶段 3: 验证 (1-2小时)
T008 → T009 → T010 + T011 + T012 并行

# 阶段 4: 发布 (30分钟)
T013 + T014 并行 → T015

# 完成！生产就绪
```

---

## 质量门控

### 每阶段完成标准

**阶段 1 完成**:
- [x] CLI version 命令成功
- [x] `.codei18n/config.json` 存在
- [x] CLI 能扫描 `.rs` 文件并返回 JSON
- [x] 测试数据文件已创建

**阶段 2 完成**:
- [x] `isSupportedFile()` 方法已修改
- [x] 所有调用点已更新
- [x] 单元测试通过 (T005)
- [x] 集成测试通过 (T006, T007)
- [x] 测试覆盖率 ≥ 60%

**阶段 3 完成**:
- [x] Rust 文件在沙盒 IDE 中正常显示翻译
- [x] Go 文件仍然正常工作（向后兼容）
- [x] 性能测试达标（CLI < 5s, UI < 100ms）
- [x] 文档已更新

**阶段 4 完成**:
- [x] 代码审查通过
- [x] `./gradlew check` 通过
- [x] `./gradlew verifyPlugin` 通过
- [x] 变更日志已更新

---

## 总结

**总任务数**: 15 个任务（极度简化，因为只修改 1 个方法）

**任务分布**:
- 阶段 1 (环境准备): 3 个任务
- 阶段 2 (核心修改): 4 个任务 (1 修改 + 3 测试)
- 阶段 3 (验证优化): 5 个任务
- 阶段 4 (发布准备): 3 个任务

**并行机会**: 9 个任务可并行执行

**预计时间线**:
- **MVP 最小交付**: 2-3 小时
- **完整交付**: 4-6 小时
- **单人顺序执行**: 6-8 小时（包含学习时间）

**核心修改**:
- ✅ 修改 1 个方法: `isGoFile()` → `isSupportedFile()`
- ✅ 添加 1 行代码: `|| filename.endsWith(".rs")`
- ✅ 总代码变更: < 10 行

**关键成功因素**:
1. ✅ CLI 已支持 Rust（前提条件）
2. ✅ 测试覆盖率 ≥ 60%（必须）
3. ✅ Go 支持不受影响（向后兼容）
4. ✅ 性能满足要求

**交付物**:
- ✅ 修改: CommentTranslationFoldingBuilder.java（1 个方法）
- ✅ 测试: 3 个测试类
- ✅ 文档: README.md 更新
- ✅ 变更日志: CHANGELOG.md 条目

**独立测试标准**:
- 创建 `.rs` 文件，打开后注释自动显示翻译
- 鼠标悬停显示英文原文
- Go 文件支持仍然正常
- 运行 `./gradlew test` 所有测试通过
- 测试覆盖率报告 ≥ 60%

---

**文档生成信息**:
- 生成日期: 2025-12-17
- 输入文档: plan.md, spec.md, research.md, data-model.md, contracts/README.md
- 关键洞察: 插件只是 CLI 的 GUI 包装器，Rust 解析由 CLI 完成
- 功能分支: 002-add-rust-support
- 预计工作量: **极度简化，4-6 小时完成**（vs 最初估计的 4-5 天）
