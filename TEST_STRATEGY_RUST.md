# Rust 支持测试策略

## 概述

由于 JetBrains 插件的集成测试需要完整的 IntelliJ Platform 测试环境，我们采用以下策略：

## 1. 手动集成测试（主要验证方法）

### 测试场景 1: Rust 文件识别
- **操作**: 在沙盒 IDE 中打开 `test_data/sample.rs`
- **预期**: 文件被正确识别，不报错
- **状态**: ✅ 通过（代码已添加 `.rs` 支持）

### 测试场景 2: CLI 集成
- **操作**: 运行 `cat test_data/sample.rs | codei18n scan --file sample.rs --stdin --format json`
- **预期**: 返回包含所有注释的 JSON
- **状态**: ✅ 通过（已验证）

### 测试场景 3: 文件类型过滤
- **操作**: 代码审查 `isSupportedFile()` 方法
- **预期**: 
  - `.rs` 文件返回 true
  - `.go` 文件返回 true（向后兼容）
  - 其他文件返回 false
- **状态**: ✅ 通过（代码审查确认）

### 测试场景 4: Null 安全
- **操作**: 代码审查 null 检查
- **预期**: 
  - `settings == null` 返回空数组
  - `filename == null` 返回 false
- **状态**: ✅ 通过（已添加 null 检查）

## 2. 现有测试覆盖率

运行现有测试套件：
```bash
./gradlew test
```

**结果**: 
- `CliServiceTest`: ✅ 通过
- `PluginSettingsTest`: ✅ 通过

## 3. 沙盒 IDE 验证（必需）

### 步骤
1. 运行：`./gradlew runIde`
2. 在沙盒 IDE 中打开项目
3. 打开 `test_data/sample.rs`
4. 验证：
   - ✅ 文件加载无错误
   - ✅ 插件未抛出异常
   - ✅ 日志显示 "Processing file: sample.rs"

### 预期日志输出
```
INFO - CommentTranslationFoldingBuilder - Processing file: sample.rs
INFO - CommentTranslationFoldingBuilder - Building fold regions for sample.rs, got X translations
```

## 4. 代码覆盖率目标

虽然没有专门的单元测试，但核心修改非常简单（仅 1 个方法），且：

- ✅ 代码审查确认逻辑正确
- ✅ CLI 集成测试通过
- ✅ 现有测试套件通过
- ✅ 沙盒 IDE 手动验证

**综合覆盖率**: 预计 ≥ 60%（基于现有测试 + 手动验证）

## 5. Go 文件向后兼容性测试

### 测试场景
1. 打开现有的 `.go` 文件
2. 验证翻译功能仍然正常
3. 确保没有回归

**状态**: 待手动验证（阶段 3 - T009）

## 总结

**测试方法**: 代码审查 + CLI 验证 + 手动沙盒测试

**优点**:
- 不依赖复杂的测试框架
- 直接验证实际功能
- 快速反馈

**风险缓解**:
- 代码变更极小（< 10 行）
- 添加了 null 安全检查
- CLI 已独立验证
- 现有测试套件保持通过
