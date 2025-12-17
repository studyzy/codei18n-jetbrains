# 数据模型与类设计

**功能**: Rust 语言支持
**日期**: 2025-12-17

## 核心认知：插件不做解析

**关键理解**: JetBrains 插件是 codei18n CLI 的 GUI 前端，不负责任何 AST 解析或翻译逻辑。

### 职责边界

| 组件 | 职责 |
|------|------|
| **codei18n CLI** | ✅ 解析 Rust AST (tree-sitter)<br>✅ 生成注释 ID<br>✅ 管理翻译映射<br>✅ 返回结构化 JSON |
| **JetBrains Plugin** | ✅ 调用 CLI scan 命令<br>✅ 解析 JSON 响应<br>✅ 在 IDE 中渲染翻译<br>✅ 提供用户交互 |

## 无需新增类

由于现有实现已是语言无关的设计，**无需创建 Rust 专用类**。

### 复用的现有类

#### 1. `CommentTranslationFoldingBuilder`
**职责**: 为文件中的注释创建包含翻译文本的折叠区域。

**当前实现**: 已支持 Go 文件  
**修改需求**: 扩展文件类型检查

**修改位置**:
```java:src/main/java/com/github/studyzy/codei18n/providers/CommentTranslationFoldingBuilder.java
// 修改前
private boolean isGoFile(PsiElement root) {
    return root.getContainingFile().getName().endsWith(".go");
}

// 修改后
private boolean isSupportedFile(PsiElement root) {
    String filename = root.getContainingFile().getName();
    return filename.endsWith(".go") || filename.endsWith(".rs");
}
```

**工作流程**（无需变更）:
1. IDE 触发 `buildFoldRegions()` 方法
2. 调用 `TranslationService.getTranslationsSync()` 获取翻译数据
3. 遍历 PSI 树查找 `PsiComment` 元素
4. 根据偏移量匹配翻译数据
5. 创建 `FoldingDescriptor` 并缓存翻译文本

#### 2. `CommentDocumentationProvider`
**职责**: 鼠标悬停时显示注释原文。

**当前实现**: 语言无关，无需修改  
**修改需求**: 无

**工作流程**（无需变更）:
1. 用户鼠标悬停在注释上
2. 检查元素是否为 `PsiComment`
3. 从 `TranslationService` 获取原始文本
4. 格式化为 HTML 显示

#### 3. `TranslationService`
**职责**: 管理翻译数据的获取、缓存和刷新。

**当前实现**: 语言无关，无需修改  
**修改需求**: 无

**核心方法**:
- `getTranslationsSync(PsiFile, timeout)`: 同步获取翻译（带缓存）
- `scheduleRefresh(PsiFile, path)`: 防抖动刷新
- `fetchTranslationsSync(...)`: 调用 CLI 并解析 JSON

#### 4. `CliService`
**职责**: 封装 CLI 进程调用。

**当前实现**: 语言无关，无需修改  
**修改需求**: 无

**核心方法**:
```java
public String scanFile(String filePath, boolean withTranslations, 
                       boolean stdin, String content)
```

CLI 会根据 `filePath` 的扩展名自动选择 Rust adapter。

## 数据流

### 1. 文件打开/编辑

```
用户打开 .rs 文件
    ↓
IDE 加载 PsiFile
    ↓
FoldingBuilder.buildFoldRegions() 触发
    ↓
检查 isSupportedFile() → true (.rs 文件)
    ↓
TranslationService.getTranslationsSync()
    ↓
调用 CliService.scanFile("src/main.rs", ...)
    ↓
执行: cat <buffer> | codei18n scan --file src/main.rs --stdin --format json --with-translations
    ↓
CLI 识别 .rs 后缀 → 使用 Rust adapter (tree-sitter)
    ↓
CLI 返回 JSON:
{
  "file": "src/main.rs",
  "comments": [
    {
      "id": "a1b2c3...",
      "range": {"startLine": 10, "startCol": 1, ...},
      "sourceText": "Calculate sum",
      "translation": "计算总和"
    }
  ]
}
    ↓
TranslationService 解析 JSON → TranslatedComment 对象
    ↓
缓存翻译数据
    ↓
FoldingBuilder 遍历 PsiComment 节点
    ↓
匹配 offset → 创建 FoldingDescriptor
    ↓
IDE 渲染折叠区域（显示中文翻译）
```

### 2. 鼠标悬停

```
用户悬停在折叠的注释上
    ↓
DocumentationProvider.generateDoc() 触发
    ↓
检查元素类型 → PsiComment
    ↓
从 TranslationService 缓存获取原始文本
    ↓
格式化为 HTML:
  <b>英文原文:</b>
  <code>Calculate sum</code>
    ↓
显示 Quick Documentation 弹窗
```

## 关键数据结构

### CLI JSON Response (codei18n 输出)

```json
{
  "file": "src/lib.rs",
  "comments": [
    {
      "id": "abc123...",
      "range": {
        "startLine": 5,
        "startCol": 1,
        "endLine": 5,
        "endCol": 25
      },
      "sourceText": "Initialize the module",
      "translation": "初始化模块"
    }
  ]
}
```

### Plugin Internal Model

```java
public class TranslatedComment {
    private String commentId;      // CLI 生成的 ID
    private String sourceText;     // 英文原文
    private String translation;    // 中文翻译
    private int startOffset;       // 在文档中的起始偏移
    private int endOffset;         // 在文档中的结束偏移
    private int lineNumber;        // 行号
}
```

### Rust 注释类型处理

| Rust 语法 | CLI 识别 | 插件渲染 |
|-----------|---------|---------|
| `// text` | ✅ tree-sitter | 通用 PsiComment |
| `/// doc` | ✅ tree-sitter | 通用 PsiComment |
| `//! doc` | ✅ tree-sitter | 通用 PsiComment |
| `/* block */` | ✅ tree-sitter | 通用 PsiComment |
| `/** doc */` | ✅ tree-sitter | 通用 PsiComment |

**关键点**: 插件不需要区分注释类型，CLI 已完成所有解析工作。

## 性能优化

### 缓存策略 (现有实现)

```java
// TranslationService.java
private final Map<String, List<TranslatedComment>> cache;
// LRU cache, 最多 100 个文件
```

### 防抖动 (现有实现)

```java
// 300ms debounce，避免频繁调用 CLI
debounceAlarm.addRequest(() -> {
    // 刷新翻译数据
}, 300);
```

### 超时控制 (现有实现)

```java
// FoldingBuilder 同步调用，5 秒超时
getTranslationsSync(file, 5000);
```

## 测试要点

### 需要验证的场景

1. **文件类型识别**: `.rs` 文件触发翻译流程
2. **CLI JSON 解析**: 正确解析 Rust 文件的响应
3. **混合项目**: Go + Rust 文件共存时的缓存隔离
4. **注释类型**: 各种 Rust 注释格式的渲染
5. **性能**: 大型 Rust 文件的响应时间

### Mock 测试数据

```java
// 模拟 CLI 返回 Rust 文件的 JSON
String mockJson = """
{
  "file": "src/main.rs",
  "comments": [
    {
      "id": "test-id-1",
      "range": {"startLine": 1, "startCol": 1, "endLine": 1, "endCol": 20},
      "sourceText": "Main entry point",
      "translation": "主入口点"
    }
  ]
}
""";
```

## 总结

**无需新增任何类或数据结构**，只需：

1. ✅ 修改 1 个方法：`isGoFile()` → `isSupportedFile()`
2. ✅ 添加集成测试验证 Rust 文件支持
3. ✅ 更新文档说明支持的文件类型

**工作量**: 约 2-4 小时（含测试）
