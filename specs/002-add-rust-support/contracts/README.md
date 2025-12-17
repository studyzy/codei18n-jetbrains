# 契约定义: Rust 语言支持

## 重要说明

**无需新增类或接口**。Rust 支持通过复用现有的语言无关实现来完成。

## 现有组件复用

### 1. CommentTranslationFoldingBuilder

**位置**: `src/main/java/com/github/studyzy/codei18n/providers/CommentTranslationFoldingBuilder.java`

**职责**: 为注释创建折叠区域，显示翻译文本

**修改内容**: 扩展文件类型检查

```java
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

**契约保持不变**:
- `buildFoldRegions()`: 构建折叠描述符列表
- `getPlaceholderText()`: 返回翻译文本
- `isCollapsedByDefault()`: 返回 true（默认折叠）

---

### 2. CommentDocumentationProvider

**位置**: `src/main/java/com/github/studyzy/codei18n/providers/CommentDocumentationProvider.java`

**职责**: 鼠标悬停时显示注释原文

**修改内容**: 无（已是语言无关实现）

**契约**:
- `generateDoc()`: 返回格式化的 HTML 文档
- 支持所有实现 `PsiComment` 接口的注释元素

---

### 3. TranslationService

**位置**: `src/main/java/com/github/studyzy/codei18n/services/TranslationService.java`

**职责**: 管理翻译数据的获取和缓存

**修改内容**: 无（已是语言无关实现）

**契约**:
- `getTranslationsSync(PsiFile, timeout)`: 同步获取翻译（带缓存）
- `getTranslations(PsiFile, forceRefresh)`: 异步获取翻译
- 返回 `List<TranslatedComment>` - 与语言无关

---

### 4. CliService

**位置**: `src/main/java/com/github/studyzy/codei18n/services/CliService.java`

**职责**: 封装 CLI 命令调用

**修改内容**: 无（CLI 根据文件扩展名自动路由）

**契约**:
```java
/**
 * 扫描文件获取注释和翻译
 * @param filePath 相对路径（如 "src/main.rs"）
 * @param withTranslations 是否包含翻译数据
 * @param stdin 是否从标准输入读取内容
 * @param content 文件内容（stdin=true 时使用）
 * @return JSON 格式的响应
 */
public String scanFile(String filePath, 
                      boolean withTranslations, 
                      boolean stdin, 
                      String content);
```

**CLI 响应格式**（语言无关）:
```json
{
  "file": "src/main.rs",
  "comments": [
    {
      "id": "abc123...",
      "range": {
        "startLine": 5,
        "startCol": 1,
        "endLine": 5,
        "endCol": 25
      },
      "sourceText": "Calculate sum",
      "translation": "计算总和"
    }
  ]
}
```

---

## CLI 契约（外部依赖）

**前提**: codei18n CLI 必须已实现 Rust adapter

**验证**:
```bash
# 检查 CLI 版本和 Rust 支持
codei18n version

# 测试 Rust 文件扫描
echo "// Test comment" > test.rs
codei18n scan --file test.rs --format json
```

**CLI 职责**:
- ✅ 使用 tree-sitter 解析 Rust AST
- ✅ 识别所有 Rust 注释类型（`//`, `///`, `//!`, `/* */`）
- ✅ 生成稳定的注释 ID
- ✅ 管理翻译映射
- ✅ 返回结构化 JSON

---

## 测试契约

### 集成测试要求

```java
public class RustSupportIntegrationTest {
    
    /**
     * 验证 .rs 文件触发翻译流程
     */
    @Test
    public void testRustFileRecognition() {
        // Given: Rust 文件
        PsiFile rustFile = createRustFile("sample.rs", "// Test");
        
        // When: 检查是否支持
        boolean supported = folding Builder.isSupportedFile(rustFile);
        
        // Then: 应该返回 true
        assertTrue(supported);
    }
    
    /**
     * 验证 CLI JSON 解析
     */
    @Test
    public void testCliJsonParsing() {
        // Given: Mock CLI 响应
        String json = "{\"file\":\"test.rs\",\"comments\":[...]}";
        
        // When: 解析响应
        List<TranslatedComment> comments = parseResponse(json);
        
        // Then: 应该正确解析
        assertNotNull(comments);
        assertEquals(1, comments.size());
    }
    
    /**
     * 验证混合项目支持
     */
    @Test
    public void testMixedProjectSupport() {
        // Given: Go 和 Rust 文件
        PsiFile goFile = createGoFile("main.go", "// Go comment");
        PsiFile rustFile = createRustFile("lib.rs", "// Rust comment");
        
        // When: 分别处理
        boolean goSupported = foldingBuilder.isSupportedFile(goFile);
        boolean rustSupported = foldingBuilder.isSupportedFile(rustFile);
        
        // Then: 都应该支持
        assertTrue(goSupported);
        assertTrue(rustSupported);
    }
}
```

---

## 向后兼容性保证

**必须确保**:
- ✅ Go 文件支持不受影响
- ✅ 现有 API 签名保持不变
- ✅ 配置文件格式兼容
- ✅ CLI 命令参数兼容

**验证方法**:
```bash
# 运行现有所有测试
./gradlew test

# 验证 Go 支持
# 1. 打开 .go 文件
# 2. 验证翻译仍然正常
```

---

## 性能契约

**要求**:
- CLI 调用响应: < 5 秒（大文件可能更长，但有超时保护）
- UI 渲染: < 100ms（通过缓存实现）
- 缓存命中率: > 80%（对于频繁打开的文件）

**测试**:
```bash
# 测试 1000 个注释的文件
time codei18n scan --file large.rs --with-translations
```

---

## 总结

**关键认知**: 
- ❌ 不需要新增任何契约
- ✅ 只需复用现有的语言无关契约
- ✅ 文件类型检查是唯一需要修改的地方

**工作量**: 修改 1 个方法，约 5 行代码
