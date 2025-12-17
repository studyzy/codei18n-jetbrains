# 研究: Rust 语言支持

**状态**: 完成
**所有者**: CodeBuddy
**日期**: 2025-12-17

## 核心架构认知

### 关键发现
通过分析 codei18n CLI 源码和 jetbrains 插件现有实现，明确了职责边界：

1. **codei18n CLI (Go)**: 
   - 负责所有 AST 解析（包括 Rust，使用 tree-sitter）
   - 负责注释 ID 生成（语义绑定）
   - 负责翻译管理和映射存储
   - 已实现 Rust adapter (`adapters/rust/`)

2. **JetBrains Plugin (Java)**:
   - 仅负责 UI 渲染和用户交互
   - 调用 CLI scan 命令获取结构化数据（JSON）
   - 不做任何 AST 解析或翻译逻辑

## 1. 无需 Rust 插件依赖

### 决策
**不添加** `org.rust.lang` 依赖。

### 理由
- **插件不做解析**: 插件通过调用 CLI 的 `scan` 命令获取翻译数据，CLI 内部已使用 tree-sitter 解析 Rust
- **避免依赖膨胀**: 不需要用户安装 Rust 插件即可使用翻译功能
- **简化维护**: 无需关心 Rust PSI 兼容性问题
- **架构一致性**: Go 文件支持也不依赖特定语言的 PSI，仅依赖通用 `PsiComment` 接口

### 原有误解
最初认为需要用 `org.rust.lang` 的 PSI 来解析注释，但实际上：
- CLI 通过 `--stdin` 接收文件内容
- CLI 根据 `--file` 参数的扩展名自动选择对应的 adapter
- 插件只需渲染 CLI 返回的 JSON 数据

## 2. 复用现有 Provider 实现

### 决策
**不创建新的** Rust 专用 Provider 类，直接复用现有的：
- `CommentTranslationFoldingBuilder`
- `CommentDocumentationProvider`

### 理由
- **语言无关设计**: 现有实现已经通过 `PsiComment` 接口处理注释，对语言类型无感知
- **数据驱动**: 翻译数据来自 CLI 的 JSON 响应，与语言无关
- **最小化修改**: 只需修改文件类型检查逻辑（`isGoFile()` → `isSupportedFile()`）

### 现有实现分析
查看 `CommentTranslationFoldingBuilder.java` 发现：
```java
// 当前只检查 .go 文件
private boolean isGoFile(PsiElement root) {
    return root.getContainingFile().getName().endsWith(".go");
}
```

**修改方案**:
```java
private boolean isSupportedFile(PsiElement root) {
    String filename = root.getContainingFile().getName();
    return filename.endsWith(".go") || filename.endsWith(".rs");
}
```

## 3. CLI 集成策略

### 决策
复用现有的 `CliService.scanFile()` 方法，无需任何修改。

### 理由
CLI 已经实现了 Rust 支持：
- `adapters/rust/adapter.go` 使用 tree-sitter 解析 Rust
- CLI 通过文件扩展名自动路由到对应的 adapter
- `--stdin` 模式确保插件能传递编辑器的"脏"缓冲区内容

### CLI 调用流程
```bash
# 插件调用（通过 CliService）
cat <editor_buffer> | codei18n scan \
  --file src/main.rs \      # CLI 识别 .rs 后缀
  --stdin \                 # 接收编辑器内容
  --format json \           # 返回 JSON
  --with-translations       # 包含翻译数据
```

CLI 返回格式（语言无关）:
```json
{
  "file": "src/main.rs",
  "comments": [
    {
      "id": "a1b2c3...",
      "range": {"startLine": 10, "startCol": 1, "endLine": 10, "endCol": 20},
      "sourceText": "Calculate sum",
      "translation": "计算总和"
    }
  ]
}
```

## 4. 无需新增扩展点

### 决策
**不注册**新的 `lang.foldingBuilder` 或 `lang.documentationProvider` 扩展点。

### 理由
- 现有扩展点已注册为 `language="go"`，但实际实现是语言无关的
- 注册多个语言扩展点会导致重复代码
- 文件类型检查在运行时进行（`isSupportedFile()`），更灵活

### 可选优化（未来）
如果需要严格按语言分离，可以重构为：
```xml
<lang.foldingBuilder language="ANY" 
    implementationClass="...CommentTranslationFoldingBuilder"/>
```

## 5. 测试策略

### 决策
添加集成测试验证 Rust 文件的完整流程。

### 测试范围
1. **CLI Mock 测试**: 验证插件能正确解析 CLI 返回的 Rust 文件 JSON
2. **UI 渲染测试**: 验证 Rust 文件的折叠和文档显示功能
3. **边界测试**: 验证混合项目（Go + Rust）的支持

### 测试文件示例
创建 `src/test/resources/testData/rust/sample.rs`:
```rust
// Calculate fibonacci numbers
fn fib(n: u32) -> u32 {
    /// Returns the nth fibonacci number
    //! This is an inner doc comment
    /* Block comment */
    if n <= 1 { n } else { fib(n-1) + fib(n-2) }
}
```

## 6. 配置文件验证

### 前提条件
项目需要初始化 `.codei18n/config.json`：
```json
{
  "sourceLanguage": "en",
  "localLanguage": "zh-CN",
  "excludePatterns": ["vendor/**", ".git/**"]
}
```

插件会检查配置文件是否存在，如不存在则提示用户运行：
```bash
codei18n init --source-lang en --local-lang zh-CN
```

## 总结

**关键洞察**: Rust 支持的实现远比最初想象简单，因为：

1. ✅ **CLI 已完成所有重活**: AST 解析、ID 生成、翻译管理
2. ✅ **插件只是薄薄一层 GUI**: 调用 CLI、解析 JSON、渲染 UI
3. ✅ **现有代码已具备扩展性**: 通用接口 + 文件类型检查
4. ✅ **无需引入新依赖**: 不需要 Rust 插件，不需要新库

**工作量估算**: 
- 修改 1 个方法（`isGoFile` → `isSupportedFile`）
- 添加集成测试
- 更新文档

总计约 **2-4 小时**（相比最初估计的 4-5 天）。
