# 实现指南: 注释替换显示技术方案

**功能**: JetBrains IDE 代码注释国际化显示插件
**日期**: 2025-12-16
**技术方案**: 使用 FoldingBuilder + DocumentationProvider 实现注释替换显示

---

## 概述

本文档详细说明如何实现"默认显示中文翻译，鼠标悬停显示英文原文"的技术方案。

### 核心技术

1. **FoldingBuilder**: 实现注释的折叠和替换显示
2. **DocumentationProvider**: 实现鼠标悬停时显示英文原文
3. **PSI**: 解析和定位注释节点
4. **TranslationService**: 管理翻译数据的获取和缓存

---

## 技术方案 1: FoldingBuilder (推荐)

### 1.1 核心原理

使用 IntelliJ Platform 的代码折叠(Folding)机制:
- 将英文注释标记为可折叠区域
- 设置折叠占位符(Placeholder)为中文翻译
- 默认折叠状态(显示中文)
- 用户可以展开查看英文原文

### 1.2 实现步骤

#### 步骤 1: 创建 FoldingBuilder

```java
package com.github.studyzy.codei18n.providers;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 注释翻译折叠构建器
 * 将英文注释折叠显示为中文翻译
 */
public class CommentTranslationFoldingBuilder extends FoldingBuilderEx {
    
    private final TranslationService translationService;
    
    public CommentTranslationFoldingBuilder() {
        this.translationService = TranslationService.getInstance();
    }
    
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(
            @NotNull PsiElement root,
            @NotNull Document document,
            boolean quick
    ) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        
        // 仅处理 Go 文件
        if (!isGoFile(root)) {
            return FoldingDescriptor.EMPTY;
        }
        
        // 遍历所有注释
        root.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitComment(@NotNull PsiComment comment) {
                super.visitComment(comment);
                
                // 获取注释文本
                String commentText = comment.getText();
                String cleanText = cleanCommentText(commentText);
                
                // 获取翻译
                String translation = translationService.getTranslation(
                    root.getContainingFile().getVirtualFile().getPath(),
                    cleanText
                );
                
                if (translation != null && !translation.isEmpty()) {
                    // 创建折叠描述符
                    FoldingGroup group = FoldingGroup.newGroup("comment-translation");
                    FoldingDescriptor descriptor = new FoldingDescriptor(
                        comment.getNode(),
                        comment.getTextRange(),
                        group,
                        translation  // 占位符文本(中文翻译)
                    );
                    descriptors.add(descriptor);
                }
            }
        });
        
        return descriptors.toArray(new FoldingDescriptor[0]);
    }
    
    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        // 返回中文翻译作为占位符
        PsiElement element = node.getPsi();
        if (element instanceof PsiComment) {
            PsiComment comment = (PsiComment) element;
            String commentText = comment.getText();
            String cleanText = cleanCommentText(commentText);
            
            String translation = translationService.getTranslation(
                element.getContainingFile().getVirtualFile().getPath(),
                cleanText
            );
            
            if (translation != null) {
                // 保留注释符号，只替换内容
                if (commentText.startsWith("//")) {
                    return "// " + translation;
                } else if (commentText.startsWith("/*")) {
                    return "/* " + translation + " */";
                }
                return translation;
            }
        }
        return null;
    }
    
    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        // 默认折叠(显示中文翻译)
        return true;
    }
    
    /**
     * 清理注释文本，移除注释符号
     */
    private String cleanCommentText(String commentText) {
        if (commentText.startsWith("//")) {
            return commentText.substring(2).trim();
        } else if (commentText.startsWith("/*") && commentText.endsWith("*/")) {
            return commentText.substring(2, commentText.length() - 2).trim();
        }
        return commentText.trim();
    }
    
    /**
     * 检查是否为 Go 文件
     */
    private boolean isGoFile(PsiElement root) {
        return root.getContainingFile().getName().endsWith(".go");
    }
}
```

#### 步骤 2: 注册 FoldingBuilder

在 `plugin.xml` 中注册:

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- 注释翻译折叠 -->
    <lang.foldingBuilder 
        language="go" 
        implementationClass="com.github.studyzy.codei18n.providers.CommentTranslationFoldingBuilder"
        order="first"/>
</extensions>
```

### 1.3 优化建议

#### 性能优化

```java
// 使用缓存避免重复计算
private final Map<String, String> translationCache = new ConcurrentHashMap<>();

private String getTranslationCached(String filePath, String commentText) {
    String key = filePath + ":" + commentText;
    return translationCache.computeIfAbsent(key, k -> 
        translationService.getTranslation(filePath, commentText)
    );
}
```

#### 异步加载

```java
@Override
public FoldingDescriptor @NotNull [] buildFoldRegions(
        @NotNull PsiElement root,
        @NotNull Document document,
        boolean quick
) {
    // 如果是快速模式，返回空数组，避免阻塞
    if (quick) {
        return FoldingDescriptor.EMPTY;
    }
    
    // 在后台线程加载翻译数据
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
        translationService.preloadTranslations(
            root.getContainingFile().getVirtualFile().getPath()
        );
    });
    
    // ... 继续构建折叠区域
}
```

---

## 技术方案 2: DocumentationProvider

### 2.1 核心原理

使用 DocumentationProvider 在鼠标悬停时显示英文原文:
- 检测鼠标悬停在注释上
- 显示包含英文原文的文档窗口
- 可以包含额外信息(如翻译来源、置信度等)

### 2.2 实现步骤

#### 步骤 1: 创建 DocumentationProvider

```java
package com.github.studyzy.codei18n.providers;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * 注释文档提供者
 * 鼠标悬停时显示英文原文
 */
public class CommentDocumentationProvider extends AbstractDocumentationProvider {
    
    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        // 仅处理注释元素
        if (!(element instanceof PsiComment)) {
            return null;
        }
        
        PsiComment comment = (PsiComment) element;
        String commentText = comment.getText();
        
        // 构建 HTML 文档
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<div style='padding: 8px;'>");
        
        // 显示英文原文
        html.append("<p><b>Original Comment:</b></p>");
        html.append("<pre style='background-color: #f5f5f5; padding: 8px; border-radius: 4px;'>");
        html.append(escapeHtml(commentText));
        html.append("</pre>");
        
        // 可选: 显示翻译信息
        String translation = getTranslation(comment);
        if (translation != null) {
            html.append("<p><b>Translation (中文):</b></p>");
            html.append("<p style='color: #666;'>");
            html.append(escapeHtml(translation));
            html.append("</p>");
        }
        
        html.append("</div>");
        html.append("</body></html>");
        
        return html.toString();
    }
    
    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        // 快速信息(鼠标悬停短暂显示)
        if (element instanceof PsiComment) {
            return "Comment: " + ((PsiComment) element).getText();
        }
        return null;
    }
    
    private String getTranslation(PsiComment comment) {
        TranslationService service = TranslationService.getInstance();
        String cleanText = cleanCommentText(comment.getText());
        return service.getTranslation(
            comment.getContainingFile().getVirtualFile().getPath(),
            cleanText
        );
    }
    
    private String cleanCommentText(String commentText) {
        if (commentText.startsWith("//")) {
            return commentText.substring(2).trim();
        } else if (commentText.startsWith("/*") && commentText.endsWith("*/")) {
            return commentText.substring(2, commentText.length() - 2).trim();
        }
        return commentText.trim();
    }
    
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
```

#### 步骤 2: 注册 DocumentationProvider

在 `plugin.xml` 中注册:

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- 注释文档提供者 -->
    <lang.documentationProvider 
        language="go" 
        implementationClass="com.github.studyzy.codei18n.providers.CommentDocumentationProvider"
        order="first"/>
</extensions>
```

---

## 技术方案 3: 组合使用

### 3.1 完整工作流程

```
1. 用户打开 Go 文件
   ↓
2. FoldingBuilder 构建折叠区域
   ↓
3. 默认折叠(显示中文翻译)
   ↓
4. 用户鼠标悬停在注释上
   ↓
5. DocumentationProvider 显示英文原文
   ↓
6. 用户可以点击展开/折叠切换
```

### 3.2 用户交互

```java
// 在设置中添加选项
public class PluginSettings {
    public boolean showOriginalOnHover = true;  // 悬停显示原文
    public boolean collapsedByDefault = true;   // 默认折叠
    public DisplayMode displayMode = DisplayMode.FOLDING;  // 显示模式
}

public enum DisplayMode {
    FOLDING,        // 折叠替换显示(默认)
    INLAY_HINT,     // 在注释下方显示
    TOOLTIP,        // 仅悬停显示
    GUTTER_ICON     // 行号旁图标
}
```

---

## 备选方案: Custom Language Injection

### 4.1 原理

使用语言注入(Language Injection)机制:
- 将注释内容注入为自定义语言
- 自定义语法高亮和渲染规则
- 更灵活但实现复杂度更高

### 4.2 适用场景

- 需要更复杂的注释渲染(如 Markdown 格式)
- 需要在注释中支持代码高亮
- 需要更精细的样式控制

**注意**: 此方案实现复杂，不推荐作为 MVP 方案。

---

## 性能优化策略

### 5.1 缓存策略

```java
public class TranslationService {
    // LRU 缓存
    private final Map<String, TranslationCache> cache = 
        Collections.synchronizedMap(new LinkedHashMap<String, TranslationCache>(100, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, TranslationCache> eldest) {
                return size() > 100;  // 最多缓存 100 个文件
            }
        });
    
    public String getTranslation(String filePath, String commentText) {
        TranslationCache fileCache = cache.get(filePath);
        
        if (fileCache == null || fileCache.isExpired()) {
            // 缓存未命中或过期，重新加载
            fileCache = loadTranslations(filePath);
            cache.put(filePath, fileCache);
        }
        
        return fileCache.getTranslation(commentText);
    }
}
```

### 5.2 异步加载

```java
// 在后台线程预加载翻译数据
ApplicationManager.getApplication().executeOnPooledThread(() -> {
    try {
        TranslationCache cache = cliService.loadTranslations(filePath);
        translationCache.put(filePath, cache);
        
        // 刷新编辑器
        ApplicationManager.getApplication().invokeLater(() -> {
            CodeFoldingManager.getInstance(project).updateFoldRegions(editor);
        });
    } catch (Exception e) {
        LOG.error("Failed to load translations", e);
    }
});
```

### 5.3 防抖机制

```java
// 避免频繁刷新
private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
private static final long DEBOUNCE_DELAY = 500; // 500ms

public void requestUpdate(String filePath) {
    long now = System.currentTimeMillis();
    Long lastUpdate = lastUpdateTime.get(filePath);
    
    if (lastUpdate == null || now - lastUpdate > DEBOUNCE_DELAY) {
        lastUpdateTime.put(filePath, now);
        performUpdate(filePath);
    }
}
```

---

## 测试策略

### 6.1 单元测试

```java
public class CommentTranslationFoldingBuilderTest extends BasePlatformTestCase {
    
    @Test
    public void testBuildFoldRegions() {
        // 准备测试文件
        PsiFile file = myFixture.configureByText("test.go", 
            "// Calculate balance\n" +
            "func calculateBalance() {}\n"
        );
        
        // 模拟翻译服务
        TranslationService mockService = mock(TranslationService.class);
        when(mockService.getTranslation(anyString(), eq("Calculate balance")))
            .thenReturn("计算余额");
        
        // 执行折叠构建
        CommentTranslationFoldingBuilder builder = new CommentTranslationFoldingBuilder();
        FoldingDescriptor[] descriptors = builder.buildFoldRegions(
            file, 
            myFixture.getEditor().getDocument(), 
            false
        );
        
        // 验证结果
        assertEquals(1, descriptors.length);
        assertEquals("// 计算余额", builder.getPlaceholderText(descriptors[0].getElement()));
        assertTrue(builder.isCollapsedByDefault(descriptors[0].getElement()));
    }
}
```

### 6.2 集成测试

```java
public class CommentTranslationIntegrationTest extends BasePlatformTestCase {
    
    @Test
    public void testEndToEndTranslation() {
        // 1. 配置 CLI
        PluginSettings.getInstance().cliPath = "/path/to/codei18n";
        
        // 2. 打开文件
        PsiFile file = myFixture.configureByFile("testdata/sample.go");
        
        // 3. 等待翻译加载
        PlatformTestUtil.waitForPromise(
            translationService.loadTranslationsAsync(file.getVirtualFile().getPath())
        );
        
        // 4. 验证折叠区域
        FoldingModel foldingModel = myFixture.getEditor().getFoldingModel();
        FoldRegion[] regions = foldingModel.getAllFoldRegions();
        
        assertTrue(regions.length > 0);
        assertTrue(regions[0].isCollapsed());
        assertEquals("// 计算余额", regions[0].getPlaceholderText());
    }
}
```

---

## 常见问题

### Q1: 折叠后如何保持代码格式？

**A**: 使用 `FoldingGroup` 确保折叠区域的一致性:

```java
FoldingGroup group = FoldingGroup.newGroup("comment-translation");
FoldingDescriptor descriptor = new FoldingDescriptor(
    comment.getNode(),
    comment.getTextRange(),
    group,
    translation
);
```

### Q2: 如何处理多行注释？

**A**: 多行注释作为一个整体折叠:

```java
if (comment.getText().startsWith("/*")) {
    // 整个块注释作为一个折叠区域
    String fullText = comment.getText();
    String cleanText = fullText.substring(2, fullText.length() - 2).trim();
    String translation = getTranslation(cleanText);
    return "/* " + translation + " */";
}
```

### Q3: 如何支持用户手动切换？

**A**: 用户可以通过以下方式切换:
- 点击折叠图标(编辑器左侧)
- 快捷键: `Ctrl+.` (展开) / `Ctrl+,` (折叠)
- 右键菜单: "Folding" → "Expand" / "Collapse"

---

## 总结

### 推荐方案

**FoldingBuilder + DocumentationProvider** 组合:
- ✅ 实现简单，代码量少
- ✅ 性能优秀，不阻塞 UI
- ✅ 用户体验自然，符合 IDE 习惯
- ✅ 支持展开/折叠切换
- ✅ 鼠标悬停显示原文

### 下一步

1. 实现 `CommentTranslationFoldingBuilder`
2. 实现 `CommentDocumentationProvider`
3. 集成 `TranslationService` 和 `CliService`
4. 编写单元测试和集成测试
5. 优化性能和用户体验

---

**文档状态**: 实现指南完成
**参考**: plan.md, data-model.md, research.md
