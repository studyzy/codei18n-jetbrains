# 技术研究: JetBrains IDE 插件开发

**功能**: JetBrains IDE 代码注释国际化显示插件
**日期**: 2025-12-16
**目的**: 研究 IntelliJ Platform Plugin 开发的关键技术领域,为实施计划提供技术决策依据

---

## 1. Inlay Hints Provider 实现

### 决策: 使用 InlayHintsProvider API

**选择理由**:
- IntelliJ Platform 2021.1+ 引入的官方 API,稳定且文档完善
- 支持异步数据加载,性能优异
- 与 IDE 原生 Inlay Hints 样式一致,用户体验良好
- 提供细粒度的位置控制和自定义渲染能力

**替代方案**:
- **Declarative Inlay Hints Provider** (2023.1+): 更简单但功能受限,适合简单场景
- **Code Vision Provider**: 用于代码上方的统计信息显示,不适合行内翻译
- **Annotation Provider**: 可以实现类似效果但性能较差,已被 Inlay Hints 替代

### 核心 API 和示例代码

```java
import com.intellij.codeInsight.hints.*;
import com.intellij.psi.PsiFile;

@InlayHintProvider
public class TranslationInlayHintsProvider implements InlayHintsProvider<NoSettings> {
    
    @Override
    public InlayHintsCollector getCollectorFor(
            PsiFile file,
            Editor editor,
            NoSettings settings,
            InlayHintsSink sink
    ) {
        return new FactoryInlayHintsCollector(editor) {
            @Override
            public boolean collect(PsiElement element, Editor editor, InlayHintsSink sink) {
                // 1. 检查是否为 Go 文件
                if (!(file instanceof GoFile)) return true;
                
                // 2. 查找注释元素
                if (element instanceof PsiComment) {
                    String commentText = element.getText();
                    
                    // 3. 获取翻译 (异步,从缓存或 CLI)
                    String translation = translationService.getTranslation(commentText);
                    if (translation != null) {
                        // 4. 创建 Inlay Hint
                        InlayPresentation presentation = getFactory()
                            .roundWithBackground(
                                getFactory().smallText(translation)
                            );
                        
                        // 5. 添加到指定位置 (注释下方)
                        sink.addInlineElement(
                            element.getTextRange().getEndOffset(),
                            false, // relatesToPrecedingText
                            presentation,
                            false  // showAbove
                        );
                    }
                }
                return true;
            }
        };
    }
    
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }
}
```

### plugin.xml 配置

```xml
<extensions defaultExtensionNs="com.intellij">
    <codeInsight.inlayProvider 
        language="go" 
        implementationClass="com.github.studyzy.codei18n.providers.TranslationInlayHintsProvider"/>
</extensions>
```

### 性能优化技巧

1. **缓存翻译数据**:
   - 使用 `CachedValue` 或自定义 LRU Cache
   - 避免每次渲染都调用 CLI

2. **批量处理**:
   - 一次性获取文件所有注释的翻译,而非逐个查询
   - 使用 `scan --with-translations` 命令

3. **避免 EDT 阻塞**:
   - 在后台线程加载翻译数据
   - 使用 `ApplicationManager.getApplication().executeOnPooledThread()`

4. **智能刷新**:
   - 仅在文件内容变更或映射文件更新时刷新
   - 使用 `InlayHintsPassFactory.forceHintsUpdateOnNextPass()`

### 常见坑点

- ❌ **在 collect() 中执行耗时操作**: 会阻塞 EDT,导致 IDE 卡顿
- ❌ **未处理 PsiInvalidElementAccessException**: PSI 元素可能已失效
- ❌ **Inlay Hint 位置计算错误**: 使用 `TextRange` 而非行号
- ❌ **内存泄漏**: 未正确管理缓存,导致翻译数据无限增长

---

## 2. PSI (Program Structure Interface) 使用

### 决策: 使用 PsiRecursiveElementVisitor 遍历注释

**选择理由**:
- PSI 是 IntelliJ Platform 的核心抽象,提供语法树访问
- Visitor 模式适合遍历所有注释,性能优于多次查询
- Go Plugin 提供完善的 PSI 支持,包括注释节点

**Go 语言注释 PSI 类型**:
- `GoLineComment`: 行注释 (`//`)
- `GoBlockComment`: 块注释 (`/* */`)
- `GoDocComment`: 文档注释 (函数/类型前的 `//`)

### 核心 API 示例

```java
import com.goide.psi.*;
import com.intellij.psi.*;

public class GoCommentExtractor {
    
    public List<PsiComment> extractComments(PsiFile file) {
        List<PsiComment> comments = new ArrayList<>();
        
        // 使用 Visitor 遍历
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof GoLineComment || 
                    element instanceof GoBlockComment) {
                    comments.add((PsiComment) element);
                }
                super.visitElement(element);
            }
        });
        
        return comments;
    }
    
    // 获取注释绑定的符号 (函数/类型/变量)
    public PsiElement getCommentOwner(PsiComment comment) {
        PsiElement parent = comment.getParent();
        PsiElement nextSibling = PsiTreeUtil.skipWhitespacesForward(comment);
        
        if (nextSibling instanceof GoFunctionDeclaration ||
            nextSibling instanceof GoTypeSpec) {
            return nextSibling;
        }
        return null;
    }
}
```

### 常用工具类

```java
// PsiTreeUtil: PSI 树操作工具
PsiComment comment = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiComment.class, false);

// 查找父元素
GoFunctionDeclaration func = PsiTreeUtil.getParentOfType(comment, GoFunctionDeclaration.class);

// 查找子元素
List<PsiComment> comments = PsiTreeUtil.findChildrenOfType(file, PsiComment.class);
```

### 关键坑点

- ❌ **未在 Read Action 中访问 PSI**: 必须使用 `ReadAction.run(() -> {...})`
- ❌ **PSI 元素生命周期管理**: 使用 `SmartPsiElementPointer` 持有长期引用
- ❌ **忽略 PSI 缓存失效**: 文件修改后 PSI 树会重建

---

## 3. 进程管理和 CLI 集成

### 决策: 使用 GeneralCommandLine 而非 ProcessBuilder

**选择理由**:
- `GeneralCommandLine` 是 IntelliJ Platform 推荐的进程管理 API
- 内置超时控制、字符编码处理、工作目录设置
- 与 `CapturingProcessHandler` 配合,简化输出捕获

**替代方案对比**:
- `ProcessBuilder`: Java 标准 API,但需手动处理超时和编码
- `Runtime.exec()`: 已过时,不推荐使用

### 核心实现示例

```java
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;

public class CliExecutor {
    
    public String executeCliCommand(String cliPath, String filePath, String targetLang) {
        try {
            // 1. 构建命令行
            GeneralCommandLine commandLine = new GeneralCommandLine()
                .withExePath(cliPath)
                .withParameters("scan", "--file", filePath, "--lang", targetLang, "--format", "json")
                .withWorkDirectory(project.getBasePath())
                .withCharset(StandardCharsets.UTF_8);
            
            // 2. 创建进程处理器 (带超时)
            CapturingProcessHandler handler = new CapturingProcessHandler(commandLine);
            ProcessOutput output = handler.runProcess(5000); // 5 秒超时
            
            // 3. 检查执行结果
            if (output.isTimeout()) {
                throw new TimeoutException("CLI execution timeout");
            }
            if (output.getExitCode() != 0) {
                String stderr = output.getStderr();
                throw new RuntimeException("CLI failed: " + stderr);
            }
            
            // 4. 返回 stdout (JSON 格式)
            return output.getStdout();
            
        } catch (ExecutionException e) {
            LOG.error("Failed to execute CLI", e);
            return null;
        }
    }
}
```

### stdin/stdout 通信示例

```java
// 使用 OSProcessHandler 支持 stdin 输入
OSProcessHandler handler = new OSProcessHandler(commandLine);

// 写入 stdin
OutputStream stdin = handler.getProcessInput();
stdin.write(fileContent.getBytes(StandardCharsets.UTF_8));
stdin.close();

// 读取 stdout (异步)
handler.addProcessListener(new ProcessAdapter() {
    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {
        if (outputType == ProcessOutputTypes.STDOUT) {
            String line = event.getText();
            // 处理输出
        }
    }
});

handler.startNotify();
handler.waitFor(5000);
```

### 常见问题和解决方案

| 问题 | 解决方案 |
|------|----------|
| 字符编码错误 (中文乱码) | 使用 `withCharset(StandardCharsets.UTF_8)` |
| 路径包含空格 | GeneralCommandLine 自动处理引号,无需手动转义 |
| 工作目录不正确 | 使用 `withWorkDirectory(project.getBasePath())` |
| 进程僵尸 | 使用 `handler.destroyProcess()` 确保清理 |
| 环境变量缺失 | 使用 `withEnvironment("PATH", systemPath)` |

---

## 4. 配置持久化

### 决策: 使用 PersistentStateComponent

**选择理由**:
- IntelliJ Platform 官方配置持久化机制
- 自动序列化/反序列化到 XML
- 支持应用级和项目级配置
- 线程安全,支持配置迁移

### 核心实现 (Java)

```java
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(
    name = "CodeI18nSettings",
    storages = @Storage("codei18n.xml")
)
public class PluginSettings implements PersistentStateComponent<PluginSettings> {
    
    // 配置字段
    public boolean enabled = true;
    public String cliPath = "codei18n";
    public String targetLanguage = "zh-CN";
    public DisplayMode displayMode = DisplayMode.INLAY_HINT;
    
    @Override
    public PluginSettings getState() {
        return this;
    }
    
    @Override
    public void loadState(PluginSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    // 获取实例 (应用级)
    public static PluginSettings getInstance() {
        return ApplicationManager.getApplication()
            .getService(PluginSettings.class);
    }
}
```

### plugin.xml 注册

```xml
<extensions defaultExtensionNs="com.intellij">
    <applicationService 
        serviceImplementation="com.github.studyzy.codei18n.settings.PluginSettings"/>
</extensions>
```

### 创建设置界面

```java
import com.intellij.openapi.options.Configurable;

public class PluginSettingsConfigurable implements Configurable {
    private PluginSettingsPanel panel;
    
    @Override
    public String getDisplayName() {
        return "CodeI18n";
    }
    
    @Override
    public JComponent createComponent() {
        panel = new PluginSettingsPanel();
        reset(); // 加载当前设置
        return panel.getRootPanel();
    }
    
    @Override
    public boolean isModified() {
        PluginSettings settings = PluginSettings.getInstance();
        return !panel.getEnabled().equals(settings.enabled) ||
               !panel.getCliPath().equals(settings.cliPath) ||
               !panel.getTargetLanguage().equals(settings.targetLanguage);
    }
    
    @Override
    public void apply() {
        PluginSettings settings = PluginSettings.getInstance();
        settings.enabled = panel.getEnabled();
        settings.cliPath = panel.getCliPath();
        settings.targetLanguage = panel.getTargetLanguage();
        
        // 通知配置变更
        ApplicationManager.getApplication().getMessageBus()
            .syncPublisher(SettingsChangeListener.TOPIC)
            .settingsChanged(settings);
    }
    
    @Override
    public void reset() {
        PluginSettings settings = PluginSettings.getInstance();
        panel.setEnabled(settings.enabled);
        panel.setCliPath(settings.cliPath);
        panel.setTargetLanguage(settings.targetLanguage);
    }
}
```

### 配置变更通知

```java
// 定义消息总线主题
public interface SettingsChangeListener {
    Topic<SettingsChangeListener> TOPIC = Topic.create(
        "CodeI18nSettingsChanged",
        SettingsChangeListener.class
    );
    
    void settingsChanged(PluginSettings settings);
}

// 订阅配置变更
project.getMessageBus()
    .connect(disposable)
    .subscribe(SettingsChangeListener.TOPIC, settings -> {
        // 清除缓存、刷新 Inlay Hints 等
        cacheService.clear();
        InlayHintsPassFactory.forceHintsUpdateOnNextPass(editor);
    });
```

---

## 5. 性能优化和缓存

### 决策: 使用 CachedValue + LRU Cache 组合

**选择理由**:
- `CachedValue`: IntelliJ Platform 内置缓存机制,自动失效
- `LRU Cache`: 控制内存占用,淘汰最少使用数据
- 组合使用兼顾性能和内存管理

### CachedValue 使用示例

```java
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;

public class TranslationService {
    
    public Map<String, String> getFileTranslations(PsiFile file) {
        CachedValuesManager manager = CachedValuesManager.getManager(file.getProject());
        
        return manager.getCachedValue(file, () -> {
            // 计算翻译数据
            Map<String, String> translations = loadTranslationsFromCli(file);
            
            // 定义依赖 (当文件修改或映射文件变更时自动失效)
            return CachedValueProvider.Result.create(
                translations,
                file,  // 依赖文件内容
                getMappingFileModificationTracker()  // 依赖映射文件
            );
        });
    }
}
```

### LRU Cache 实现

```java
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;
    
    public LRUCache(int maxSize) {
        super(16, 0.75f, true); // accessOrder = true
        this.maxSize = maxSize;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}

// 使用
LRUCache<String, Map<String, String>> cache = new LRUCache<>(100);
```

### 文件变更监听

```java
// 1. 监听虚拟文件系统变更
VirtualFileManager.getInstance().addVirtualFileListener(
    new VirtualFileListener() {
        @Override
        public void contentsChanged(VirtualFileEvent event) {
            if (isMappingFile(event.getFile())) {
                cacheService.clear();
            }
        }
    },
    disposable
);

// 2. 监听 PSI 变更 (更精确)
PsiManager.getInstance(project).addPsiTreeChangeListener(
    new PsiTreeChangeAdapter() {
        @Override
        public void childrenChanged(PsiTreeChangeEvent event) {
            if (event.getParent() instanceof PsiComment) {
                // 注释内容变更,清除缓存
                cacheService.clearFile(event.getFile());
            }
        }
    },
    disposable
);
```

### 性能优化最佳实践

1. **减少 PSI 访问频率**:
   - 批量收集注释,一次性获取翻译
   - 避免在循环中重复访问 PSI

2. **延迟计算**:
   - 仅在用户查看代码时才加载翻译
   - 使用 `LazyValue` 延迟初始化

3. **异步预加载**:
   - 文件打开时在后台预加载翻译数据
   - 使用 `StartupActivity` 预热缓存

---

## 6. 异步任务和后台线程

### 决策: 使用 Task.Backgroundable + ProgressManager

**选择理由**:
- 标准的后台任务执行方式
- 自动显示进度条,用户体验友好
- 支持取消操作
- 与 IntelliJ Platform 生命周期集成

### 核心 API 示例

```java
import com.intellij.openapi.progress.*;

public class TranslationLoader {
    
    public void loadTranslationsAsync(Project project, PsiFile file) {
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, "Loading Translations", true) {
                private Map<String, String> translations;
                
                @Override
                public void run(ProgressIndicator indicator) {
                    indicator.setIndeterminate(false);
                    indicator.setText("Calling codei18n CLI...");
                    indicator.setFraction(0.3);
                    
                    // 执行 CLI 调用 (耗时操作)
                    String output = cliService.scanFile(file.getVirtualFile().getPath());
                    
                    if (indicator.isCanceled()) {
                        return;
                    }
                    
                    indicator.setText("Parsing JSON response...");
                    indicator.setFraction(0.7);
                    
                    // 解析 JSON
                    translations = jsonParser.parse(output);
                    
                    indicator.setFraction(1.0);
                }
                
                @Override
                public void onSuccess() {
                    // 回到 EDT 更新 UI
                    cacheService.putTranslations(file, translations);
                    InlayHintsPassFactory.forceHintsUpdateOnNextPass(editor);
                }
                
                @Override
                public void onThrowable(Throwable error) {
                    LOG.error("Failed to load translations", error);
                    Notifications.Bus.notify(
                        new Notification(
                            "CodeI18n",
                            "Translation Load Failed",
                            error.getMessage(),
                            NotificationType.ERROR
                        ),
                        project
                    );
                }
            }
        );
    }
}
```

### Read Action 中执行异步任务

```java
// 在 Read Action 中访问 PSI,但 CLI 调用在后台线程
ReadAction.nonBlocking(() -> {
    // Read Action: 访问 PSI
    List<PsiComment> comments = extractComments(file);
    return comments;
})
.finishOnUiThread(ModalityState.defaultModalityState(), comments -> {
    // EDT: 启动后台任务
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
        // 后台线程: 调用 CLI
        String output = cliService.getTranslations(comments);
        
        // 回到 EDT 更新 UI
        ApplicationManager.getApplication().invokeLater(() -> {
            updateInlayHints(output);
        });
    });
})
.submit(AppExecutorUtil.getAppExecutorService());
```

### 关键坑点

- ❌ **在 EDT 中执行耗时操作**: 导致 IDE 冻结
- ❌ **在后台线程访问 PSI**: 必须包裹在 `ReadAction.run()` 中
- ❌ **忘记检查 indicator.isCanceled()**: 用户取消后仍继续执行
- ❌ **在 Read Action 中调用 CLI**: 会阻塞其他 Read Action

---

## 总结和技术决策

| 领域 | 选择方案 | 关键依赖 |
|------|----------|----------|
| Inlay Hints 显示 | InlayHintsProvider API | com.intellij.codeInsight.hints |
| PSI 解析 | PsiRecursiveElementVisitor | com.goide.psi, com.intellij.psi |
| CLI 集成 | GeneralCommandLine + CapturingProcessHandler | com.intellij.execution |
| 配置持久化 | PersistentStateComponent | com.intellij.openapi.components |
| 缓存管理 | CachedValue + LRU Cache | com.intellij.psi.util |
| 异步任务 | Task.Backgroundable + ProgressManager | com.intellij.openapi.progress |

### 关键注意事项

1. **性能优先**: 所有耗时操作必须异步执行,不阻塞 EDT
2. **缓存策略**: 使用 `CachedValue` 自动失效,避免手动管理
3. **错误处理**: CLI 调用失败时优雅降级,不影响 IDE 正常使用
4. **测试覆盖**: 核心服务 (CliService, TranslationService) 必须达到 80%+ 覆盖率
5. **向后兼容**: 最低支持 IntelliJ Platform 2023.3,避免使用过新的 API

---

**文档状态**: 研究完成,可进入阶段 1 设计
**下一步**: 生成 data-model.md 和 contracts/
