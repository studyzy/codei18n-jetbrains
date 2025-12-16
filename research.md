# IntelliJ Platform Plugin 开发最佳实践研究

> 研究日期: 2025-12-16
> 目标: 为 codei18n-jetbrains 插件开发提供技术指导

---

## 目录
1. [Inlay Hints Provider 实现](#1-inlay-hints-provider-实现)
2. [PSI (Program Structure Interface) 使用](#2-psi-program-structure-interface-使用)
3. [进程管理和 CLI 集成](#3-进程管理和-cli-集成)
4. [配置持久化](#4-配置持久化)
5. [性能优化和缓存](#5-性能优化和缓存)
6. [异步任务和后台线程](#6-异步任务和后台线程)

---

## 1. Inlay Hints Provider 实现

### 1.1 推荐的实现方案

#### API 选择策略

IntelliJ Platform 提供了多种 Inlay Hints API,选择应基于版本和需求:

| 版本 | 推荐 API | 适用场景 |
|------|---------|---------|
| 2023.1+ | **Declarative Inlay Hints Provider** | 内联提示(推荐) |
| 2022.1+ | **Code Vision Provider** | 块提示/代码视图 |
| 旧版本或定制需求 | **InlayHintsProvider** | 最大灵活性 |

#### 基本实现步骤

1. **实现 InlayHintsProvider 接口**
2. **在 plugin.xml 中注册扩展点**:
   ```xml
   <extensions defaultExtensionNs="com.intellij">
       <codeInsight.inlayProvider
           language="go"
           implementationClass="com.example.MyInlayHintsProvider"/>
   </extensions>
   ```

### 1.2 关键 API 和示例

#### 示例 1: 简单变量类型提示 (Groovy)
```kotlin
// 参考: GroovyLocalVariableTypeHintsInlayProvider
class GoCommentInlayProvider : InlayHintsProvider<NoSettings> {
    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(
                element: PsiElement,
                editor: Editor,
                sink: InlayHintsSink
            ): Boolean {
                // 遍历 PSI 元素,添加提示
                if (element is GoVariableDefinition) {
                    val hint = factory.smallText("翻译文本")
                    sink.addInlineElement(element.textRange.endOffset, false, hint)
                }
                return true
            }
        }
    }
}
```

#### 示例 2: 复杂提示 (Kotlin Lambda)
```kotlin
// 参考: KotlinLambdasHintsProvider
// 支持交互式提示,点击跳转等高级功能
class InteractiveInlayProvider : InlayHintsProvider<Settings> {
    override fun createSettings() = Settings()
    
    override fun getCollectorFor(/* ... */): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                // 创建可点击的提示
                val presentation = factory.seq(
                    factory.smallText("参数: "),
                    factory.referenceOnHover(element.textRange) {
                        // 点击处理逻辑
                    }
                )
                sink.addInlineElement(offset, relatesToPrecedingText = true, presentation)
                return true
            }
        }
    }
}
```

### 1.3 Go 语言 Inlay Hints 最佳实践

#### 注释提取和显示策略

```kotlin
class GoI18nInlayProvider : InlayHintsProvider<NoSettings> {
    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        // 仅处理 Go 文件
        if (file.language.id != "go") return null
        
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(
                element: PsiElement,
                editor: Editor,
                sink: InlayHintsSink
            ): Boolean {
                // 查找行注释
                if (element is PsiComment) {
                    val commentText = element.text
                    // 提取翻译键
                    val translationKey = extractI18nKey(commentText)
                    if (translationKey != null) {
                        val translation = getTranslation(translationKey)
                        val hint = factory.roundWithBackground(
                            factory.smallText(translation)
                        )
                        sink.addInlineElement(
                            element.textRange.endOffset,
                            relatesToPrecedingText = false,
                            hint,
                            showOnlyIfExistedBefore = false
                        )
                    }
                }
                return true
            }
        }
    }
}
```

### 1.4 性能优化技巧

#### 避免频繁刷新

```kotlin
class OptimizedInlayProvider : InlayHintsProvider<Settings> {
    
    // 1. 使用缓存避免重复计算
    private val cache = ConcurrentHashMap<String, String>()
    
    override fun getCollectorFor(/* ... */): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(
                element: PsiElement,
                editor: Editor,
                sink: InlayHintsSink
            ): Boolean {
                // 2. 快速路径:跳过不相关元素
                if (!isRelevantElement(element)) return true
                
                // 3. 批量处理
                val hints = mutableListOf<InlayData>()
                element.accept(object : PsiRecursiveElementWalkingVisitor() {
                    override fun visitComment(comment: PsiComment) {
                        hints.add(extractHintData(comment))
                    }
                })
                
                // 4. 统一添加提示
                hints.forEach { data ->
                    sink.addInlineElement(data.offset, false, data.presentation)
                }
                
                return true
            }
        }
    }
    
    // 5. 监听配置变更,清除缓存
    init {
        ApplicationManager.getApplication().messageBus
            .connect()
            .subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
                override fun settingsChanged() {
                    cache.clear()
                    // 触发编辑器刷新
                    DaemonCodeAnalyzer.getInstance(project).restart()
                }
            })
    }
}
```

#### 性能优化清单

- ✅ **快速过滤**: 在 `collect()` 开始就过滤不相关元素
- ✅ **批量处理**: 收集所有提示后统一添加,减少 API 调用
- ✅ **缓存结果**: 对于不常变化的数据(如翻译),使用缓存
- ✅ **增量更新**: 只在必要时触发刷新,而非每次编辑
- ✅ **异步加载**: 复杂计算(如网络请求)应异步执行

### 1.5 已知坑点和注意事项

#### ⚠️ 坑点 1: EDT 阻塞
```kotlin
// ❌ 错误:在 collect() 中执行耗时操作
override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
    val translation = fetchTranslationFromNetwork(key) // 阻塞 UI 线程!
    sink.addInlineElement(offset, false, factory.text(translation))
    return true
}

// ✅ 正确:异步加载,使用占位符
override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
    val cachedValue = cache[key]
    if (cachedValue != null) {
        sink.addInlineElement(offset, false, factory.text(cachedValue))
    } else {
        // 显示加载中占位符
        sink.addInlineElement(offset, false, factory.text("..."))
        // 异步加载
        ApplicationManager.getApplication().executeOnPooledThread {
            val translation = fetchTranslationFromNetwork(key)
            cache[key] = translation
            // 刷新 Inlay Hints
            ApplicationManager.getApplication().invokeLater {
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }
    }
    return true
}
```

#### ⚠️ 坑点 2: 错误的 `relatesToPrecedingText`
```kotlin
// 提示位置控制
sink.addInlineElement(
    offset,
    relatesToPrecedingText = false, // false: 提示在元素后; true: 提示在元素前
    presentation
)
```

#### ⚠️ 坑点 3: 内存泄漏
```kotlin
// ❌ 错误:持有 Editor 引用
class LeakyInlayProvider : InlayHintsProvider<Settings> {
    private var editor: Editor? = null // 可能导致内存泄漏
}

// ✅ 正确:不在 Provider 中持有 Editor
class SafeInlayProvider : InlayHintsProvider<Settings> {
    // Provider 是单例,不要持有短生命周期对象
}
```

### 1.6 兼容性和版本建议

| 功能 | 最低版本 | 推荐版本 | 备注 |
|------|---------|---------|------|
| InlayHintsProvider | 2020.1+ | 2020.3+ | 基础 API |
| Declarative Provider | 2023.1+ | 2023.2+ | 简化实现 |
| Code Vision | 2022.1+ | 2022.3+ | 块提示 |
| InlayHintsProviderFactory | 2019.3+ | 2020.1+ | 多语言支持 |

---

## 2. PSI (Program Structure Interface) 使用

### 2.1 推荐的实现方案

#### PSI 基础概念

PSI (Program Structure Interface) 是 IntelliJ Platform 的核心,用于:
- **解析源代码文件**,构建语法树
- **提供语义模型**,支持代码分析和操作
- **支持多语言**,统一的 API 接口

#### PSI 树结构层次

```
PsiFile (根节点)
  ├─ PsiElement (语句/声明)
  │   ├─ PsiComment (注释)
  │   ├─ PsiIdentifier (标识符)
  │   └─ ...
  └─ PsiWhiteSpace (空白)
```

### 2.2 关键 API 和示例代码

#### 获取 PSI 元素的三种方式

**方式 1: 从 Action 上下文获取**
```kotlin
class MyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val element: PsiElement? = e.getData(CommonDataKeys.PSI_ELEMENT)
        // 注意:如果光标在引用上,返回的是引用解析后的目标元素
    }
}
```

**方式 2: 从 PSI 文件获取**
```kotlin
fun findElementAtCaret(editor: Editor, file: PsiFile): PsiElement? {
    val offset = editor.caretModel.offset
    // 返回叶子节点(通常是 token)
    val leaf = file.findElementAt(offset)
    
    // 查找特定类型的父元素
    return PsiTreeUtil.getParentOfType(leaf, GoFunctionDeclaration::class.java)
}
```

**方式 3: 从引用解析**
```kotlin
fun resolveReference(element: PsiElement): PsiElement? {
    val reference = element.reference ?: return null
    return reference.resolve()
}
```

#### PSI 树遍历

**推荐方式: PsiRecursiveElementWalkingVisitor**
```kotlin
fun collectComments(file: PsiFile): List<PsiComment> {
    val comments = mutableListOf<PsiComment>()
    
    file.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitComment(comment: PsiComment) {
            super.visitComment(comment)
            comments.add(comment)
        }
    })
    
    return comments
}
```

**性能优化: 提前终止遍历**
```kotlin
fun findFirstComment(file: PsiFile): PsiComment? {
    var result: PsiComment? = null
    
    file.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitComment(comment: PsiComment) {
            if (result == null) {
                result = comment
                stopWalking() // 找到后立即停止遍历
            }
        }
    })
    
    return result
}
```

### 2.3 Go 语言注释提取

#### Go Plugin PSI 树结构

Go 语言的 PSI 树结构(依赖 Go Plugin):

```
GoFile
  ├─ GoPackageClause
  ├─ GoImportList
  ├─ GoFunctionDeclaration
  │   ├─ GoComment (文档注释)
  │   ├─ GoSignature
  │   └─ GoBlock
  │       ├─ GoComment (行注释)
  │       └─ GoStatement
  └─ ...
```

#### 提取不同类型的注释

```kotlin
import com.goide.psi.*

// 1. 提取所有注释
fun extractAllComments(goFile: GoFile): List<PsiComment> {
    val comments = mutableListOf<PsiComment>()
    
    goFile.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitComment(comment: PsiComment) {
            super.visitComment(comment)
            comments.add(comment)
        }
    })
    
    return comments
}

// 2. 区分行注释和块注释
fun extractCommentsByType(goFile: GoFile): Map<String, List<PsiComment>> {
    val lineComments = mutableListOf<PsiComment>()
    val blockComments = mutableListOf<PsiComment>()
    
    goFile.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitComment(comment: PsiComment) {
            super.visitComment(comment)
            when {
                comment.text.startsWith("//") -> lineComments.add(comment)
                comment.text.startsWith("/*") -> blockComments.add(comment)
            }
        }
    })
    
    return mapOf(
        "line" to lineComments,
        "block" to blockComments
    )
}

// 3. 提取文档注释 (函数/类型前的注释)
fun extractDocComments(element: GoNamedElement): String? {
    // Go 文档注释紧邻声明之前
    val docComment = PsiTreeUtil.getPrevSiblingOfType(element, PsiComment::class.java)
    return docComment?.text?.removePrefix("//")?.trim()
}

// 4. 提取特定格式的注释 (如 i18n 标记)
fun extractI18nComments(goFile: GoFile): List<Pair<PsiComment, String>> {
    val i18nComments = mutableListOf<Pair<PsiComment, String>>()
    val i18nPattern = Regex("""//\s*i18n:\s*(.+)""")
    
    goFile.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitComment(comment: PsiComment) {
            super.visitComment(comment)
            val match = i18nPattern.find(comment.text)
            if (match != null) {
                val key = match.groupValues[1].trim()
                i18nComments.add(comment to key)
            }
        }
    })
    
    return i18nComments
}
```

### 2.4 常用 PSI 工具类

#### PsiTreeUtil - PSI 树操作工具

```kotlin
import com.intellij.psi.util.PsiTreeUtil

// 1. 查找父元素
val functionDecl = PsiTreeUtil.getParentOfType(
    element,
    GoFunctionDeclaration::class.java
)

// 2. 查找所有子元素
val allComments = PsiTreeUtil.findChildrenOfType(
    goFile,
    PsiComment::class.java
)

// 3. 获取兄弟元素
val nextSibling = PsiTreeUtil.getNextSiblingOfType(
    element,
    GoStatement::class.java
)

// 4. 收集特定类型的元素
val results = mutableListOf<GoFunctionDeclaration>()
PsiTreeUtil.collectElementsOfType(
    goFile,
    GoFunctionDeclaration::class.java,
    results
)

// 5. 查找共同父元素
val commonParent = PsiTreeUtil.findCommonParent(element1, element2)
```

#### PsiManager 和 PsiFile 操作

```kotlin
import com.intellij.psi.PsiManager

// 1. 从 VirtualFile 获取 PsiFile
val psiManager = PsiManager.getInstance(project)
val psiFile = psiManager.findFile(virtualFile)

// 2. 监听 PSI 变化
PsiManager.getInstance(project).addPsiTreeChangeListener(
    object : PsiTreeChangeAdapter() {
        override fun childAdded(event: PsiTreeChangeEvent) {
            // PSI 节点添加
        }
        
        override fun childRemoved(event: PsiTreeChangeEvent) {
            // PSI 节点删除
        }
    }
)

// 3. 获取文件语言
val language = psiFile.language // Language.findLanguageByID("go")
```

### 2.5 已知坑点和注意事项

#### ⚠️ 坑点 1: PSI 访问必须在 Read Action 中

```kotlin
// ❌ 错误:直接访问 PSI
fun badExample(file: PsiFile) {
    val comments = file.children // 可能抛出异常!
}

// ✅ 正确:使用 Read Action
fun goodExample(file: PsiFile) {
    ApplicationManager.getApplication().runReadAction {
        val comments = file.children
        // 安全访问 PSI
    }
}

// ✅ 更好:使用 ReadAction.compute
fun betterExample(file: PsiFile): List<PsiElement> {
    return ReadAction.compute<List<PsiElement>, Throwable> {
        file.children.toList()
    }
}
```

#### ⚠️ 坑点 2: PSI 引用失效

```kotlin
// ❌ 错误:长时间持有 PSI 引用
class BadService {
    private var cachedElement: PsiElement? = null // 可能失效!
    
    fun process() {
        cachedElement?.text // 可能抛出 PsiInvalidElementAccessException
    }
}

// ✅ 正确:使用 SmartPsiElementPointer
class GoodService(project: Project) {
    private val pointerManager = SmartPointerManager.getInstance(project)
    private var elementPointer: SmartPsiElementPointer<PsiElement>? = null
    
    fun cacheElement(element: PsiElement) {
        elementPointer = pointerManager.createSmartPsiElementPointer(element)
    }
    
    fun process() {
        val element = elementPointer?.element // 自动更新到最新的 PSI
        element?.text
    }
}
```

#### ⚠️ 坑点 3: findElementAt 返回叶子节点

```kotlin
// 常见误解
val element = file.findElementAt(offset) // 返回的是 token,不是语句!

// 正确用法:向上查找有意义的元素
val statement = PsiTreeUtil.getParentOfType(
    file.findElementAt(offset),
    GoStatement::class.java
)
```

### 2.6 性能优化建议

```kotlin
// 1. 避免重复遍历
// ❌ 低效
fun badPerformance(file: PsiFile) {
    val comments = PsiTreeUtil.findChildrenOfType(file, PsiComment::class.java)
    val strings = PsiTreeUtil.findChildrenOfType(file, GoStringLiteral::class.java)
    val functions = PsiTreeUtil.findChildrenOfType(file, GoFunctionDeclaration::class.java)
}

// ✅ 高效:一次遍历收集所有
fun goodPerformance(file: PsiFile) {
    val comments = mutableListOf<PsiComment>()
    val strings = mutableListOf<GoStringLiteral>()
    val functions = mutableListOf<GoFunctionDeclaration>()
    
    file.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            super.visitElement(element)
            when (element) {
                is PsiComment -> comments.add(element)
                is GoStringLiteral -> strings.add(element)
                is GoFunctionDeclaration -> functions.add(element)
            }
        }
    })
}

// 2. 使用缓存
val cachedValue = CachedValuesManager.getCachedValue(file) {
    CachedValueProvider.Result.create(
        extractComments(file),
        PsiModificationTracker.MODIFICATION_COUNT // 依赖项:PSI 变化时失效
    )
}
```

---

## 3. 进程管理和 CLI 集成

### 3.1 推荐的实现方案

IntelliJ Platform 提供了完善的外部进程执行 API,推荐使用 `GeneralCommandLine` 而非 `ProcessBuilder`。

#### API 选择

| 方式 | 适用场景 | 优缺点 |
|------|---------|--------|
| **GeneralCommandLine** | ✅ 推荐,所有场景 | + IDE 集成好<br>+ 字符编码处理<br>+ 工作目录管理<br>+ 环境变量支持 |
| ProcessBuilder | ❌ 不推荐 | - 需要手动处理编码<br>- 与 IDE 集成差 |
| Runtime.exec() | ❌ 过时 | - API 过时<br>- 功能有限 |

### 3.2 关键 API 和示例代码

#### 基本用法: 执行 CLI 命令

```kotlin
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key

fun executeCommand(
    executablePath: String,
    args: List<String>,
    workingDir: String? = null
): String {
    // 1. 构建命令行
    val commandLine = GeneralCommandLine(executablePath)
        .withParameters(args)
        .withCharset(Charsets.UTF_8) // 设置字符编码
    
    // 2. 设置工作目录(可选)
    if (workingDir != null) {
        commandLine.withWorkDirectory(workingDir)
    }
    
    // 3. 执行命令并获取输出
    val process = commandLine.createProcess()
    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    
    if (exitCode != 0) {
        val error = process.errorStream.bufferedReader().readText()
        throw RuntimeException("Command failed: $error")
    }
    
    return output
}
```

#### 异步执行带进度提示

```kotlin
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.progress.ProgressIndicator

fun executeWithProgress(
    commandLine: GeneralCommandLine,
    indicator: ProgressIndicator
): String {
    indicator.text = "Executing ${commandLine.commandLineString}"
    
    // 使用 CapturingProcessHandler 自动捕获输出
    val processHandler = CapturingProcessHandler(commandLine)
    
    // 添加进度监听
    processHandler.addProcessListener(object : ProcessAdapter() {
        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            indicator.text2 = event.text.trim()
        }
    })
    
    // 执行并等待完成(可被用户取消)
    val output = processHandler.runProcess(60_000, true) // 60 秒超时
    
    if (output.exitCode != 0) {
        throw RuntimeException("Process failed: ${output.stderr}")
    }
    
    return output.stdout
}
```

#### 完整示例: CLI 工具集成

```kotlin
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class CodeI18nCLI(private val project: Project) {
    
    private val cliPath: String by lazy {
        // 从设置中获取 CLI 路径
        MySettings.getInstance(project).cliPath
            ?: throw IllegalStateException("CLI path not configured")
    }
    
    /**
     * 提取文件中的 i18n 键
     */
    fun extractKeys(file: VirtualFile): List<String> {
        val commandLine = GeneralCommandLine(cliPath)
            .withParameters("extract", file.path)
            .withWorkDirectory(project.basePath)
            .withCharset(Charsets.UTF_8)
            .withEnvironment("LANG", "en_US.UTF-8") // 设置环境变量
        
        val output = executeCommand(commandLine, timeout = 30_000)
        
        // 解析 JSON 输出
        return parseJsonKeys(output.stdout)
    }
    
    /**
     * 翻译指定的键
     */
    fun translate(
        key: String,
        targetLanguage: String,
        sourceText: String
    ): String {
        // 使用 stdin 传递大量数据
        val commandLine = GeneralCommandLine(cliPath)
            .withParameters("translate", "--key", key, "--lang", targetLanguage)
            .withWorkDirectory(project.basePath)
            .withCharset(Charsets.UTF_8)
        
        val processHandler = CapturingProcessHandler(commandLine)
        
        // 写入 stdin
        val stdin = processHandler.process.outputStream
        stdin.write(sourceText.toByteArray(Charsets.UTF_8))
        stdin.close()
        
        val output = processHandler.runProcess(60_000)
        
        if (output.exitCode != 0) {
            throw RuntimeException("Translation failed: ${output.stderr}")
        }
        
        return output.stdout.trim()
    }
    
    /**
     * 执行命令的通用方法
     */
    private fun executeCommand(
        commandLine: GeneralCommandLine,
        timeout: Int = 30_000
    ): ProcessOutput {
        val processHandler = CapturingProcessHandler(commandLine)
        val output = processHandler.runProcess(timeout, true)
        
        if (output.isTimeout) {
            throw RuntimeException("Command timeout after ${timeout}ms")
        }
        
        if (output.exitCode != 0) {
            throw RuntimeException(
                "Command failed with exit code ${output.exitCode}: ${output.stderr}"
            )
        }
        
        return output
    }
    
    /**
     * 检查 CLI 是否可用
     */
    fun checkAvailability(): Boolean {
        return try {
            val commandLine = GeneralCommandLine(cliPath)
                .withParameters("--version")
            val output = executeCommand(commandLine, timeout = 5_000)
            output.stdout.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}
```

### 3.3 处理超时、错误输出、异步执行

#### 超时处理

```kotlin
fun executeWithTimeout(commandLine: GeneralCommandLine, timeoutMs: Int): ProcessOutput {
    val processHandler = CapturingProcessHandler(commandLine)
    val output = processHandler.runProcess(timeoutMs, true) // true 表示可被中断
    
    when {
        output.isTimeout -> {
            processHandler.destroyProcess() // 强制终止
            throw RuntimeException("Process timeout after ${timeoutMs}ms")
        }
        output.isCancelled -> {
            throw RuntimeException("Process cancelled by user")
        }
        output.exitCode != 0 -> {
            throw RuntimeException("Process failed: ${output.stderr}")
        }
    }
    
    return output
}
```

#### 实时监听输出

```kotlin
fun executeWithRealtimeOutput(
    commandLine: GeneralCommandLine,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
) {
    val processHandler = OSProcessHandler(commandLine)
    
    processHandler.addProcessListener(object : ProcessAdapter() {
        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            when (outputType) {
                ProcessOutputTypes.STDOUT -> onStdout(event.text)
                ProcessOutputTypes.STDERR -> onStderr(event.text)
            }
        }
        
        override fun processTerminated(event: ProcessEvent) {
            val exitCode = event.exitCode
            if (exitCode != 0) {
                onStderr("Process exited with code $exitCode")
            }
        }
    })
    
    processHandler.startNotify() // 启动异步监听
    processHandler.waitFor() // 等待完成
}

// 使用示例
executeWithRealtimeOutput(
    GeneralCommandLine("npm", "install"),
    onStdout = { line -> logger.info(line) },
    onStderr = { line -> logger.warn(line) }
)
```

#### 后台异步执行

```kotlin
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task

fun executeInBackground(
    project: Project,
    title: String,
    commandLine: GeneralCommandLine
) {
    ProgressManager.getInstance().run(
        object : Task.Backgroundable(project, title, true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                
                val processHandler = CapturingProcessHandler(commandLine)
                
                // 监听输出更新进度
                processHandler.addProcessListener(object : ProcessAdapter() {
                    private var lineCount = 0
                    
                    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                        lineCount++
                        indicator.fraction = minOf(lineCount / 100.0, 1.0)
                        indicator.text2 = event.text.trim()
                        
                        // 检查取消
                        if (indicator.isCanceled) {
                            processHandler.destroyProcess()
                        }
                    }
                })
                
                val output = processHandler.runProcess()
                
                // 在 EDT 上显示结果
                ApplicationManager.getApplication().invokeLater {
                    if (output.exitCode == 0) {
                        Notifications.Bus.notify(
                            Notification(
                                "MyPlugin",
                                "Success",
                                "Command completed successfully",
                                NotificationType.INFORMATION
                            )
                        )
                    } else {
                        Messages.showErrorDialog(
                            project,
                            "Command failed: ${output.stderr}",
                            "Execution Error"
                        )
                    }
                }
            }
        }
    )
}
```

### 3.4 stdin/stdout 通信

#### 与 CLI 工具交互式通信

```kotlin
fun interactiveCommand(commandLine: GeneralCommandLine): String {
    val process = commandLine.createProcess()
    
    // 获取输入输出流
    val stdin = process.outputStream.bufferedWriter()
    val stdout = process.inputStream.bufferedReader()
    val stderr = process.errorStream.bufferedReader()
    
    try {
        // 发送输入
        stdin.write("translate\n")
        stdin.write("hello world\n")
        stdin.flush()
        
        // 读取输出(逐行)
        val response = buildString {
            var line: String?
            while (stdout.readLine().also { line = it } != null) {
                appendLine(line)
                if (line == "EOF") break // 自定义结束标记
            }
        }
        
        // 关闭输入流,触发进程结束
        stdin.close()
        
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val error = stderr.readText()
            throw RuntimeException("Command failed: $error")
        }
        
        return response
    } finally {
        process.destroy()
    }
}
```

#### JSON-RPC 风格的 CLI 通信

```kotlin
import kotlinx.serialization.json.*

class CLIRpcClient(cliPath: String) {
    private val process: Process
    private val stdin: BufferedWriter
    private val stdout: BufferedReader
    
    init {
        val commandLine = GeneralCommandLine(cliPath)
            .withParameters("--json-rpc")
        process = commandLine.createProcess()
        stdin = process.outputStream.bufferedWriter()
        stdout = process.inputStream.bufferedReader()
    }
    
    fun call(method: String, params: JsonObject): JsonObject {
        // 构建 JSON-RPC 请求
        val request = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
            put("id", System.currentTimeMillis())
        }
        
        // 发送请求
        stdin.write(request.toString())
        stdin.write("\n")
        stdin.flush()
        
        // 接收响应
        val responseLine = stdout.readLine()
            ?: throw RuntimeException("No response from CLI")
        
        val response = Json.parseToJsonElement(responseLine).jsonObject
        
        // 检查错误
        if (response.containsKey("error")) {
            val error = response["error"]!!.jsonObject
            throw RuntimeException("RPC error: ${error["message"]}")
        }
        
        return response["result"]?.jsonObject
            ?: throw RuntimeException("Invalid response")
    }
    
    fun close() {
        stdin.close()
        process.destroy()
    }
}

// 使用示例
val client = CLIRpcClient("/path/to/cli")
try {
    val result = client.call("translate", buildJsonObject {
        put("text", "Hello")
        put("lang", "zh")
    })
    val translation = result["translation"]?.jsonPrimitive?.content
} finally {
    client.close()
}
```

### 3.5 已知坑点和注意事项

#### ⚠️ 坑点 1: 字符编码问题

```kotlin
// ❌ 错误:未设置编码,中文乱码
val commandLine = GeneralCommandLine("my-cli")

// ✅ 正确:显式设置 UTF-8
val commandLine = GeneralCommandLine("my-cli")
    .withCharset(Charsets.UTF_8)
    .withEnvironment("LANG", "en_US.UTF-8") // Linux/Mac
```

#### ⚠️ 坑点 2: 路径空格和特殊字符

```kotlin
// ❌ 错误:路径包含空格,可能失败
val commandLine = GeneralCommandLine("C:\\Program Files\\My Tool\\cli.exe")

// ✅ 正确:路径会自动转义
val commandLine = GeneralCommandLine("C:\\Program Files\\My Tool\\cli.exe")
// 或者使用 File
val commandLine = GeneralCommandLine(File("C:\\Program Files\\My Tool\\cli.exe").absolutePath)
```

#### ⚠️ 坑点 3: 忘记关闭流导致挂起

```kotlin
// ❌ 错误:读取 stdout 但进程继续写入 stderr,导致缓冲区满
val process = commandLine.createProcess()
val output = process.inputStream.bufferedReader().readText() // 可能挂起!

// ✅ 正确:同时消费 stdout 和 stderr
val processHandler = CapturingProcessHandler(commandLine)
val output = processHandler.runProcess() // 自动处理所有流
```

#### ⚠️ 坑点 4: 在 EDT 上执行长时间进程

```kotlin
// ❌ 错误:阻塞 UI 线程
fun onButtonClick() {
    val output = executeCommand(longRunningCommand) // UI 卡死!
}

// ✅ 正确:后台执行
fun onButtonClick() {
    ProgressManager.getInstance().run(
        object : Task.Backgroundable(project, "Running Command", true) {
            override fun run(indicator: ProgressIndicator) {
                val output = executeCommand(longRunningCommand)
                // 处理结果...
            }
        }
    )
}
```

### 3.6 性能和兼容性建议

#### 性能优化

1. **进程池**: 对于频繁调用,考虑维护一个长期运行的 CLI 进程
2. **批处理**: 合并多个请求到一次调用
3. **缓存结果**: 对于幂等操作,缓存结果避免重复执行

```kotlin
// 进程池示例
class CLIProcessPool(val cliPath: String, poolSize: Int = 4) {
    private val processes = ConcurrentLinkedQueue<Process>()
    
    init {
        repeat(poolSize) {
            val process = GeneralCommandLine(cliPath)
                .withParameters("--server-mode")
                .createProcess()
            processes.offer(process)
        }
    }
    
    fun <T> execute(block: (Process) -> T): T {
        val process = processes.poll()
            ?: throw IllegalStateException("No available process")
        try {
            return block(process)
        } finally {
            processes.offer(process)
        }
    }
}
```

#### 兼容性

| 平台 | 注意事项 |
|------|---------|
| Windows | 使用 `.exe` 扩展名,注意路径分隔符 `\` |
| macOS | 检查执行权限 `chmod +x`,处理 Gatekeeper |
| Linux | 检查执行权限,依赖库路径(LD_LIBRARY_PATH) |

```kotlin
fun getCLIPath(): String {
    val os = SystemInfo.getOS()
    val cliName = when {
        os.isWindows -> "cli.exe"
        os.isMac || os.isLinux -> "cli"
        else -> throw UnsupportedOperationException("Unsupported OS: $os")
    }
    
    return Paths.get(project.basePath, "bin", cliName).toString()
}
```

---

## 4. 配置持久化

### 4.1 推荐的实现方案

IntelliJ Platform 提供了完善的配置持久化 API,核心是 `PersistentStateComponent` 接口。

#### API 选择策略

| 方案 | 适用场景 | 推荐度 |
|------|---------|--------|
| **SimplePersistentStateComponent** | Kotlin + 简单配置 | ⭐⭐⭐⭐⭐ |
| **SerializablePersistentStateComponent** | Kotlin + 不可变状态 | ⭐⭐⭐⭐⭐ |
| **PersistentStateComponent (手动)** | Java / 复杂需求 | ⭐⭐⭐⭐ |
| **PropertiesComponent** | 简单键值对 | ⭐⭐⭐ |

### 4.2 关键 API 和示例代码

#### 方案 1: SimplePersistentStateComponent (Kotlin 推荐)

```kotlin
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag

@Service
@State(
    name = "CodeI18nSettings",
    storages = [Storage("codei18n.xml")], // 配置文件名
    category = SettingsCategory.PLUGINS
)
class CodeI18nSettings : SimplePersistentStateComponent<CodeI18nSettings.State>(State()) {
    
    class State : BaseState() {
        // 使用 property delegates 定义字段
        var cliPath by string("") // 默认值为空字符串
        var autoTranslate by property(false) // 默认值为 false
        var targetLanguages by list<String>() // 默认为空列表
        var apiKey by string()
        var timeout by property(30_000) // 默认 30 秒
        
        // 复杂对象需要使用 @OptionTag
        @OptionTag(converter = LanguageMapConverter::class)
        var languageMap by property(mapOf<String, String>())
    }
    
    companion object {
        fun getInstance(project: Project): CodeI18nSettings =
            project.service()
    }
}

// 使用示例
val settings = CodeI18nSettings.getInstance(project)
settings.state.cliPath = "/usr/local/bin/cli"
settings.state.autoTranslate = true
settings.state.targetLanguages = listOf("zh", "ja", "fr")
```

#### 方案 2: SerializablePersistentStateComponent (线程安全)

```kotlin
@Service
@State(
    name = "CodeI18nSettings",
    storages = [Storage("codei18n.xml")]
)
class CodeI18nSettings : SerializablePersistentStateComponent<CodeI18nSettings.State>(State()) {
    
    @Serializable
    data class State(
        @JvmField val cliPath: String = "",
        @JvmField val autoTranslate: Boolean = false,
        @JvmField val targetLanguages: List<String> = emptyList(),
        @JvmField val timeout: Int = 30_000
    )
    
    // 提供便捷的访问器
    var cliPath: String
        get() = state.cliPath
        set(value) = updateState { it.copy(cliPath = value) }
    
    var autoTranslate: Boolean
        get() = state.autoTranslate
        set(value) = updateState { it.copy(autoTranslate = value) }
    
    fun addTargetLanguage(lang: String) {
        updateState {
            it.copy(targetLanguages = it.targetLanguages + lang)
        }
    }
    
    companion object {
        fun getInstance(project: Project): CodeI18nSettings =
            project.service()
    }
}
```

#### 方案 3: 传统 PersistentStateComponent (Java)

```java
@Service
@State(
    name = "CodeI18nSettings",
    storages = @Storage("codei18n.xml")
)
public final class CodeI18nSettings implements PersistentStateComponent<CodeI18nSettings.State> {
    
    public static class State {
        public String cliPath = "";
        public boolean autoTranslate = false;
        public List<String> targetLanguages = new ArrayList<>();
        public int timeout = 30_000;
    }
    
    private State myState = new State();
    
    public static CodeI18nSettings getInstance(Project project) {
        return project.getService(CodeI18nSettings.class);
    }
    
    @Override
    public State getState() {
        return myState;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }
    
    // Getter 和 Setter
    public String getCliPath() {
        return myState.cliPath;
    }
    
    public void setCliPath(String path) {
        myState.cliPath = path;
    }
}
```

### 4.3 创建插件设置界面 (Configurable)

#### 基本设置界面

```kotlin
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class CodeI18nConfigurable(private val project: Project) : Configurable {
    
    private val settings = CodeI18nSettings.getInstance(project)
    
    // UI 组件
    private val cliPathField = JBTextField()
    private val autoTranslateCheckbox = JBCheckBox("Enable auto-translation")
    private val timeoutField = JBTextField()
    
    override fun getDisplayName(): String = "Code I18n"
    
    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("CLI Path:", cliPathField)
            .addComponent(autoTranslateCheckbox)
            .addLabeledComponent("Timeout (ms):", timeoutField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
    
    override fun isModified(): Boolean {
        return cliPathField.text != settings.state.cliPath ||
               autoTranslateCheckbox.isSelected != settings.state.autoTranslate ||
               timeoutField.text.toIntOrNull() != settings.state.timeout
    }
    
    override fun apply() {
        settings.state.cliPath = cliPathField.text
        settings.state.autoTranslate = autoTranslateCheckbox.isSelected
        settings.state.timeout = timeoutField.text.toIntOrNull() ?: 30_000
        
        // 通知配置变更
        project.messageBus
            .syncPublisher(CodeI18nSettingsListener.TOPIC)
            .settingsChanged(settings.state)
    }
    
    override fun reset() {
        cliPathField.text = settings.state.cliPath
        autoTranslateCheckbox.isSelected = settings.state.autoTranslate
        timeoutField.text = settings.state.timeout.toString()
    }
}
```

#### 在 plugin.xml 中注册

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- 项目级别设置 -->
    <projectConfigurable
        instance="com.example.CodeI18nConfigurable"
        displayName="Code I18n"
        id="com.example.codei18n.settings"/>
    
    <!-- 或应用级别设置 -->
    <applicationConfigurable
        instance="com.example.CodeI18nConfigurable"
        displayName="Code I18n"
        id="com.example.codei18n.settings"/>
    
    <!-- 服务定义 -->
    <projectService
        serviceImplementation="com.example.CodeI18nSettings"/>
</extensions>
```

#### 高级设置界面 (带验证和文件选择)

```kotlin
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.*
import java.io.File

class AdvancedConfigurable(private val project: Project) : Configurable {
    
    private val settings = CodeI18nSettings.getInstance(project)
    private val cliPathField = TextFieldWithBrowseButton()
    
    init {
        // 添加文件选择器
        cliPathField.addBrowseFolderListener(
            "Select CLI Executable",
            "Choose the code-i18n CLI tool",
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
    }
    
    override fun createComponent(): JComponent = panel {
        row("CLI Path:") {
            cliPathField()
                .withValidation {
                    val path = it.text
                    when {
                        path.isBlank() -> ValidationInfo("CLI path is required", it)
                        !File(path).exists() -> ValidationInfo("File does not exist", it)
                        !File(path).canExecute() -> ValidationInfo("File is not executable", it)
                        else -> null
                    }
                }
        }
        
        row {
            checkBox("Enable auto-translation", settings.state::autoTranslate)
        }
        
        row("Timeout (seconds):") {
            intTextField(settings.state::timeout, 1..300)
                .comment("Maximum time to wait for CLI response")
        }
        
        row("Target Languages:") {
            expandableTextField()
                .comment("Comma-separated list of language codes")
        }
    }
    
    // ... isModified, apply, reset 实现
}
```

### 4.4 配置变更通知

#### 定义监听器接口

```kotlin
import com.intellij.util.messages.Topic

interface CodeI18nSettingsListener {
    fun settingsChanged(newState: CodeI18nSettings.State)
    
    companion object {
        @Topic.ProjectLevel
        val TOPIC = Topic(
            CodeI18nSettingsListener::class.java,
            Topic.BroadcastDirection.NONE
        )
    }
}
```

#### 发布配置变更

```kotlin
// 在 Configurable.apply() 中
override fun apply() {
    val oldState = settings.state.copy() // 保存旧状态
    
    // 更新配置
    settings.state.cliPath = cliPathField.text
    settings.state.autoTranslate = autoTranslateCheckbox.isSelected
    
    // 发布变更事件
    project.messageBus
        .syncPublisher(CodeI18nSettingsListener.TOPIC)
        .settingsChanged(settings.state)
}
```

#### 订阅配置变更

```kotlin
@Service
class MyService(private val project: Project) {
    
    init {
        // 订阅配置变更
        project.messageBus
            .connect()
            .subscribe(
                CodeI18nSettingsListener.TOPIC,
                object : CodeI18nSettingsListener {
                    override fun settingsChanged(newState: CodeI18nSettings.State) {
                        // 清除缓存
                        cache.clear()
                        
                        // 重新初始化 CLI 客户端
                        reinitializeCLI(newState.cliPath)
                        
                        // 刷新 UI
                        DaemonCodeAnalyzer.getInstance(project).restart()
                    }
                }
            )
    }
}
```

### 4.5 存储位置配置

#### 常用存储选项

```kotlin
@State(
    name = "MySettings",
    storages = [
        // 1. 自定义文件(推荐)
        Storage("myPlugin.xml")
        
        // 2. 项目工作区文件(不共享)
        Storage(StoragePathMacros.WORKSPACE_FILE)
        
        // 3. 缓存文件(可删除)
        Storage(StoragePathMacros.CACHE_FILE)
    ],
    // 漫游类型(云同步)
    roamingType = RoamingType.DEFAULT, // DEFAULT | PER_OS | DISABLED
    
    // 设置类别(影响云同步行为)
    category = SettingsCategory.PLUGINS, // PLUGINS | CODE | UI | KEYMAP | OTHER
    
    // 外部变更时是否需要重启
    reloadable = true // true: 自动重新加载; false: 需要重启 IDE
)
```

#### 多级别配置

```kotlin
// 应用级别(全局)
@Service(Service.Level.APP)
@State(
    name = "GlobalI18nSettings",
    storages = [Storage("codei18n-global.xml")]
)
class GlobalI18nSettings : SimplePersistentStateComponent<GlobalI18nSettings.State>(State()) {
    class State : BaseState() {
        var apiEndpoint by string("https://api.i18n.com")
        var apiKey by string()
    }
    
    companion object {
        fun getInstance(): GlobalI18nSettings =
            service()
    }
}

// 项目级别
@Service(Service.Level.PROJECT)
@State(
    name = "ProjectI18nSettings",
    storages = [Storage("codei18n-project.xml")]
)
class ProjectI18nSettings : SimplePersistentStateComponent<ProjectI18nSettings.State>(State()) {
    class State : BaseState() {
        var sourceLanguage by string("en")
        var targetLanguages by list<String>()
    }
    
    companion object {
        fun getInstance(project: Project): ProjectI18nSettings =
            project.service()
    }
}
```

### 4.6 已知坑点和注意事项

#### ⚠️ 坑点 1: 忘记实现 equals() 导致每次都保存

```kotlin
// ❌ 错误:State 没有实现 equals,IDE 无法判断是否变更
class State : BaseState() {
    var value by string()
}

// ✅ 正确:使用 data class 自动生成 equals
@Serializable
data class State(
    @JvmField val value: String = ""
)
```

#### ⚠️ 坑点 2: 在 Extension 中实现 PersistentStateComponent

```kotlin
// ❌ 错误:Extension 不应该是 PersistentStateComponent
class MyInlayProvider : InlayHintsProvider<Settings>, PersistentStateComponent<Settings> {
    // 这会导致问题!
}

// ✅ 正确:创建单独的 Service 存储配置
@Service
class MySettings : PersistentStateComponent<MySettings.State> {
    // ...
}

class MyInlayProvider : InlayHintsProvider<NoSettings> {
    // 使用 Service 获取配置
    private val settings = MySettings.getInstance(project)
}
```

#### ⚠️ 坑点 3: 集合修改后忘记调用 incrementModificationCount

```kotlin
// 使用 SimplePersistentStateComponent
class State : BaseState() {
    var list by list<String>()
}

// ❌ 错误:修改集合后未标记变更
fun addItem(item: String) {
    state.list.add(item) // IDE 不知道状态已变更!
}

// ✅ 正确:手动标记
fun addItem(item: String) {
    state.list.add(item)
    state.incrementModificationCount() // 通知状态已变更
}

// ✅ 更好:使用不可变 State
@Serializable
data class State(
    @JvmField val list: List<String> = emptyList()
)

fun addItem(item: String) {
    updateState { it.copy(list = it.list + item) } // 自动标记变更
}
```

### 4.7 简化配置: PropertiesComponent

对于简单的键值对,可以使用 `PropertiesComponent`:

```kotlin
// 应用级别
val appProps = PropertiesComponent.getInstance()
appProps.setValue("com.example.myKey", "myValue")
val value = appProps.getValue("com.example.myKey")

// 项目级别
val projectProps = PropertiesComponent.getInstance(project)
projectProps.setValue("com.example.projectKey", "projectValue")
projectProps.setBoolean("com.example.enabled", true)

// ⚠️ 注意:使用插件 ID 作为前缀避免冲突
```

---

## 5. 性能优化和缓存

### 5.1 推荐的缓存机制

IntelliJ Platform 提供了多种缓存机制,核心是 `CachedValuesManager`。

#### 缓存策略选择

| 缓存类型 | 适用场景 | 失效时机 |
|---------|---------|---------|
| **CachedValue** | PSI 相关计算 | PSI 修改时 |
| **CachedValue (自定义依赖)** | 配置相关计算 | 依赖项变化时 |
| **UserDataHolder** | 临时数据 | 手动清除 |
| **手动 LRU Cache** | 全局缓存 | 容量限制 |

### 5.2 CachedValue 使用示例

#### 基本用法

```kotlin
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiModificationTracker

fun getCommentsFromFile(file: PsiFile): List<PsiComment> {
    return CachedValuesManager.getCachedValue(file) {
        // 计算结果
        val comments = mutableListOf<PsiComment>()
        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitComment(comment: PsiComment) {
                comments.add(comment)
            }
        })
        
        // 返回结果和依赖项
        CachedValueProvider.Result.create(
            comments,
            PsiModificationTracker.MODIFICATION_COUNT // PSI 修改时失效
        )
    }
}
```

#### 自定义依赖项

```kotlin
fun getTranslations(file: PsiFile, project: Project): Map<String, String> {
    return CachedValuesManager.getCachedValue(file) {
        val settings = CodeI18nSettings.getInstance(project)
        
        // 执行翻译计算...
        val translations = computeTranslations(file, settings)
        
        // 多个依赖项:PSI 修改或配置变更时失效
        CachedValueProvider.Result.create(
            translations,
            PsiModificationTracker.MODIFICATION_COUNT,
            settings // 配置变更时也失效
        )
    }
}
```

#### 项目级别缓存

```kotlin
@Service(Service.Level.PROJECT)
class TranslationCache(private val project: Project) {
    
    fun getTranslation(key: String, language: String): String? {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            // 从 API 加载所有翻译
            val allTranslations = fetchAllTranslations()
            
            CachedValueProvider.Result.create(
                allTranslations,
                // 依赖配置变更
                CodeI18nSettings.getInstance(project)
            )
        }[key to language]
    }
    
    companion object {
        fun getInstance(project: Project): TranslationCache =
            project.service()
    }
}
```

### 5.3 实现 LRU Cache

```kotlin
import com.intellij.util.containers.ContainerUtil
import java.util.concurrent.ConcurrentHashMap

class LRUTranslationCache(maxSize: Int = 1000) {
    
    // IntelliJ 提供的线程安全 LRU Map
    private val cache = ContainerUtil.createConcurrentSoftValueMap<String, String>()
    
    // 或使用 Caffeine (需要添加依赖)
    private val caffeineCache = Caffeine.newBuilder()
        .maximumSize(maxSize.toLong())
        .expireAfterAccess(Duration.ofMinutes(30))
        .build<String, String>()
    
    fun get(key: String, loader: () -> String): String {
        return cache.computeIfAbsent(key) { loader() }
    }
    
    fun invalidate(key: String) {
        cache.remove(key)
    }
    
    fun clear() {
        cache.clear()
    }
}

// 使用示例
@Service
class TranslationService(private val project: Project) {
    private val cache = LRUTranslationCache(maxSize = 5000)
    
    fun translate(text: String, language: String): String {
        val cacheKey = "$text:$language"
        return cache.get(cacheKey) {
            // 实际翻译逻辑
            performTranslation(text, language)
        }
    }
    
    fun clearCache() {
        cache.clear()
    }
}
```

### 5.4 UserDataHolder 临时数据存储

```kotlin
import com.intellij.openapi.util.Key

// 定义 Key
private val TRANSLATION_KEY = Key.create<String>("com.example.translation")

// 存储数据到 PsiElement
fun cacheTranslation(element: PsiElement, translation: String) {
    element.putUserData(TRANSLATION_KEY, translation)
}

// 读取数据
fun getTranslation(element: PsiElement): String? {
    return element.getUserData(TRANSLATION_KEY)
}

// ⚠️ 注意:UserData 不会自动清除,需要手动管理
fun clearTranslation(element: PsiElement) {
    element.putUserData(TRANSLATION_KEY, null)
}
```

### 5.5 监听文件变更清除缓存

#### 方案 1: VirtualFileListener

```kotlin
import com.intellij.openapi.vfs.*

class CacheClearingListener(private val cache: LRUTranslationCache) : VirtualFileListener {
    
    override fun contentsChanged(event: VirtualFileEvent) {
        // 文件内容变更,清除相关缓存
        val file = event.file
        if (file.extension == "go") {
            cache.clear()
        }
    }
    
    override fun fileDeleted(event: VirtualFileEvent) {
        // 文件删除,清除缓存
        cache.clear()
    }
}

// 注册监听器
@Service
class MyCacheService(private val project: Project) {
    private val cache = LRUTranslationCache()
    
    init {
        VirtualFileManager.getInstance().addVirtualFileListener(
            CacheClearingListener(cache),
            project // 项目关闭时自动注销
        )
    }
}
```

#### 方案 2: PsiTreeChangeListener

```kotlin
import com.intellij.psi.*

class PSICacheClearingListener(private val cache: LRUTranslationCache) : PsiTreeChangeAdapter() {
    
    override fun childrenChanged(event: PsiTreeChangeEvent) {
        // PSI 树变更
        val file = event.file
        if (file?.language?.id == "go") {
            cache.clear()
        }
    }
}

// 注册
init {
    PsiManager.getInstance(project).addPsiTreeChangeListener(
        PSICacheClearingListener(cache),
        project
    )
}
```

#### 方案 3: BulkFileListener (批量文件变更)

```kotlin
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class BulkCacheClearingListener(private val cache: LRUTranslationCache) : BulkFileListener {
    
    override fun after(events: MutableList<out VFileEvent>) {
        val hasGoFileChange = events.any { event ->
            event.file?.extension == "go"
        }
        
        if (hasGoFileChange) {
            cache.clear()
        }
    }
}

// 注册到消息总线
init {
    project.messageBus.connect().subscribe(
        VirtualFileManager.VFS_CHANGES,
        BulkCacheClearingListener(cache)
    )
}
```

### 5.6 性能优化最佳实践

#### 1. 减少 PSI 访问

```kotlin
// ❌ 低效:多次访问 PSI
fun processFile(file: PsiFile) {
    val comments = extractComments(file) // 遍历 1
    val strings = extractStrings(file)   // 遍历 2
    val functions = extractFunctions(file) // 遍历 3
}

// ✅ 高效:一次遍历收集所有
fun processFile(file: PsiFile) {
    val result = ApplicationManager.getApplication().runReadAction(Computable {
        val comments = mutableListOf<PsiComment>()
        val strings = mutableListOf<GoStringLiteral>()
        val functions = mutableListOf<GoFunctionDeclaration>()
        
        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                when (element) {
                    is PsiComment -> comments.add(element)
                    is GoStringLiteral -> strings.add(element)
                    is GoFunctionDeclaration -> functions.add(element)
                }
            }
        })
        
        Triple(comments, strings, functions)
    })
}
```

#### 2. 批量处理

```kotlin
// ❌ 低效:逐个处理
fun translateComments(comments: List<PsiComment>) {
    comments.forEach { comment ->
        val translation = translateSingle(comment.text)
        applyTranslation(comment, translation)
    }
}

// ✅ 高效:批量翻译
fun translateComments(comments: List<PsiComment>) {
    // 1. 批量提取文本
    val texts = comments.map { it.text }
    
    // 2. 批量翻译(一次 API 调用)
    val translations = translateBatch(texts)
    
    // 3. 批量应用
    comments.zip(translations).forEach { (comment, translation) ->
        applyTranslation(comment, translation)
    }
}
```

#### 3. 延迟计算

```kotlin
// 使用 lazy 延迟初始化
class MyService(private val project: Project) {
    
    // 仅在首次访问时初始化
    private val expensiveData by lazy {
        computeExpensiveData()
    }
    
    fun process() {
        // 仅在需要时才计算
        if (needExpensiveData) {
            expensiveData.use()
        }
    }
}
```

#### 4. 异步预加载

```kotlin
@Service
class PreloadingService(private val project: Project) {
    
    init {
        // 在后台预加载常用数据
        ApplicationManager.getApplication().executeOnPooledThread {
            preloadTranslations()
            preloadConfigurations()
        }
    }
    
    private fun preloadTranslations() {
        val settings = CodeI18nSettings.getInstance(project)
        settings.state.targetLanguages.forEach { lang ->
            // 预加载常用翻译
            cache.get("common:$lang") {
                fetchCommonTranslations(lang)
            }
        }
    }
}
```

### 5.7 已知坑点

#### ⚠️ 坑点 1: 缓存依赖项错误导致陈旧数据

```kotlin
// ❌ 错误:依赖项不足,配置变更时缓存未失效
CachedValueProvider.Result.create(
    computeResult(),
    PsiModificationTracker.MODIFICATION_COUNT // 仅依赖 PSI 变化
)

// ✅ 正确:添加所有相关依赖
CachedValueProvider.Result.create(
    computeResult(),
    PsiModificationTracker.MODIFICATION_COUNT,
    CodeI18nSettings.getInstance(project), // 配置依赖
    ModificationTracker.EVER_CHANGED // 或其他自定义依赖
)
```

#### ⚠️ 坑点 2: 忘记取消注册监听器导致内存泄漏

```kotlin
// ❌ 错误:未指定作用域,监听器永不释放
VirtualFileManager.getInstance().addVirtualFileListener(listener)

// ✅ 正确:关联到项目生命周期
VirtualFileManager.getInstance().addVirtualFileListener(listener, project)

// ✅ 或手动管理
val connection = project.messageBus.connect()
connection.subscribe(TOPIC, listener)
// 需要时取消:connection.disconnect()
```

---

## 6. 异步任务和后台线程

### 6.1 推荐的实现方案

IntelliJ Platform 提供了完善的线程模型,核心原则:**永不阻塞 EDT (Event Dispatch Thread)**。

#### 线程模型

| 线程类型 | 用途 | API |
|---------|------|-----|
| **EDT** | UI 更新,用户交互 | `invokeLater`, `invokeAndWait` |
| **Read Action** | 读取 PSI/文档 | `runReadAction` |
| **Write Action** | 修改 PSI/文档 | `runWriteAction` |
| **Pooled Thread** | 后台计算 | `executeOnPooledThread` |
| **Background Task** | 带进度的长任务 | `Task.Backgroundable` |

### 6.2 关键 API 和示例

#### EDT 操作

```kotlin
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState

// 1. 在 EDT 上执行(异步)
ApplicationManager.getApplication().invokeLater {
    // 更新 UI
    myLabel.text = "Updated"
}

// 2. 指定 Modality State(模态对话框支持)
ApplicationManager.getApplication().invokeLater(
    { updateUI() },
    ModalityState.stateForComponent(myComponent)
)

// 3. 在 EDT 上执行(同步,阻塞等待)
ApplicationManager.getApplication().invokeAndWait {
    // UI 操作
}

// 4. 检查是否在 EDT 上
if (ApplicationManager.getApplication().isDispatchThread) {
    // 当前在 EDT 上
}
```

#### Read/Write Action

```kotlin
// 1. Read Action (读取 PSI)
val result = ApplicationManager.getApplication().runReadAction(Computable {
    // 安全读取 PSI
    file.findElementAt(offset)
})

// 2. Write Action (修改 PSI/文档)
ApplicationManager.getApplication().runWriteAction {
    // 修改文档
    document.setText("new content")
}

// 3. Smart Mode (等待索引完成)
DumbService.getInstance(project).runReadActionInSmartMode(Computable {
    // 需要索引的操作(如 resolve)
    reference.resolve()
})

// 4. 检查权限
if (ApplicationManager.getApplication().isReadAccessAllowed) {
    // 可以安全访问 PSI
}
```

#### 后台线程执行

```kotlin
// 1. 简单后台任务
ApplicationManager.getApplication().executeOnPooledThread {
    // 执行耗时操作
    val result = performCalculation()
    
    // 切回 EDT 更新 UI
    ApplicationManager.getApplication().invokeLater {
        updateUI(result)
    }
}

// 2. 可取消的后台任务
ApplicationManager.getApplication().executeOnPooledThread {
    while (!Thread.currentThread().isInterrupted) {
        // 执行工作
        if (shouldStop()) break
    }
}
```

### 6.3 ProgressManager 使用

#### Task.Backgroundable - 标准后台任务

```kotlin
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator

// 基本用法
ProgressManager.getInstance().run(
    object : Task.Backgroundable(
        project,
        "Processing Files", // 标题
        true // 可取消
    ) {
        override fun run(indicator: ProgressIndicator) {
            indicator.text = "Loading translations..."
            indicator.isIndeterminate = false
            
            val files = getFilesToProcess()
            files.forEachIndexed { index, file ->
                // 检查取消
                if (indicator.isCanceled) return
                
                // 更新进度
                indicator.fraction = index.toDouble() / files.size
                indicator.text2 = "Processing ${file.name}"
                
                // 执行工作
                processFile(file)
            }
        }
        
        override fun onSuccess() {
            // 任务成功完成(在 EDT 上)
            Notifications.Bus.notify(
                Notification(
                    "MyPlugin",
                    "Success",
                    "All files processed",
                    NotificationType.INFORMATION
                )
            )
        }
        
        override fun onThrowable(error: Throwable) {
            // 任务失败(在 EDT 上)
            Messages.showErrorDialog(
                project,
                "Processing failed: ${error.message}",
                "Error"
            )
        }
        
        override fun onCancel() {
            // 用户取消(在 EDT 上)
            logger.info("User cancelled processing")
        }
    }
)
```

#### Task.Modal - 模态任务(阻塞 UI)

```kotlin
ProgressManager.getInstance().run(
    object : Task.Modal(project, "Initializing...", false) {
        override fun run(indicator: ProgressIndicator) {
            // 阻塞 UI 直到完成
            initializePlugin()
        }
    }
)
```

#### Task.WithResult - 返回结果的任务

```kotlin
val result = ProgressManager.getInstance().run(
    object : Task.WithResult<List<String>, Exception>(
        project,
        "Fetching Data",
        true
    ) {
        override fun compute(indicator: ProgressIndicator): List<String> {
            indicator.text = "Fetching from server..."
            return fetchDataFromServer()
        }
    }
)
```

#### ProgressManager 高级用法

```kotlin
// 1. 嵌套进度(子任务)
ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Main Task", true) {
    override fun run(indicator: ProgressIndicator) {
        indicator.text = "Step 1: Preparing..."
        prepare()
        
        // 子任务 1
        ProgressManager.getInstance().executeProcessUnderProgress(
            {
                indicator.text = "Step 2: Processing..."
                process()
            },
            indicator
        )
        
        // 子任务 2
        indicator.text = "Step 3: Finalizing..."
        finalize()
    }
})

// 2. 可检查进度的代码块
fun heavyComputation(indicator: ProgressIndicator?) {
    val items = getItems()
    items.forEachIndexed { index, item ->
        // 定期检查取消和更新进度
        ProgressManager.checkCanceled() // 如果取消,抛出异常
        
        indicator?.fraction = index.toDouble() / items.size
        indicator?.text2 = "Processing ${item.name}"
        
        processItem(item)
    }
}
```

### 6.4 实际应用场景示例

#### 场景 1: Inlay Hints 异步加载翻译

```kotlin
class GoI18nInlayProvider : InlayHintsProvider<NoSettings> {
    
    private val cache = ConcurrentHashMap<String, String>()
    
    override fun getCollectorFor(/* ... */): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(
                element: PsiElement,
                editor: Editor,
                sink: InlayHintsSink
            ): Boolean {
                if (element is PsiComment) {
                    val key = extractI18nKey(element.text) ?: return true
                    
                    // 尝试从缓存获取
                    val cached = cache[key]
                    if (cached != null) {
                        sink.addInlineElement(
                            element.textRange.endOffset,
                            false,
                            factory.text(cached)
                        )
                    } else {
                        // 显示占位符
                        sink.addInlineElement(
                            element.textRange.endOffset,
                            false,
                            factory.text("⏳")
                        )
                        
                        // 异步加载
                        ApplicationManager.getApplication().executeOnPooledThread {
                            val translation = fetchTranslation(key)
                            cache[key] = translation
                            
                            // 刷新 Inlay Hints
                            ApplicationManager.getApplication().invokeLater {
                                DaemonCodeAnalyzer.getInstance(project).restart(file)
                            }
                        }
                    }
                }
                return true
            }
        }
    }
}
```

#### 场景 2: 批量文件处理

```kotlin
class BatchTranslationAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = getSelectedGoFiles(e)
        
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(
                project,
                "Translating ${files.size} files",
                true
            ) {
                override fun run(indicator: ProgressIndicator) {
                    val results = mutableListOf<TranslationResult>()
                    
                    files.forEachIndexed { index, file ->
                        if (indicator.isCanceled) return
                        
                        indicator.fraction = index.toDouble() / files.size
                        indicator.text2 = "Processing ${file.name}"
                        
                        // 在 Read Action 中访问 PSI
                        val psiFile = ReadAction.compute<PsiFile?, Throwable> {
                            PsiManager.getInstance(project).findFile(file)
                        }
                        
                        if (psiFile != null) {
                            val result = translateFile(psiFile, indicator)
                            results.add(result)
                        }
                    }
                    
                    // 保存结果
                    saveResults(results, indicator)
                }
                
                override fun onSuccess() {
                    ApplicationManager.getApplication().invokeLater {
                        Notifications.Bus.notify(
                            Notification(
                                "I18n",
                                "Translation Complete",
                                "Successfully translated ${files.size} files",
                                NotificationType.INFORMATION
                            ),
                            project
                        )
                    }
                }
            }
        )
    }
}
```

#### 场景 3: CLI 工具异步调用

```kotlin
@Service
class AsyncCLIService(private val project: Project) {
    
    fun translateAsync(
        text: String,
        language: String,
        callback: (String) -> Unit
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // 构建命令
                val commandLine = GeneralCommandLine(getCliPath())
                    .withParameters("translate", "--lang", language)
                
                // 执行
                val processHandler = CapturingProcessHandler(commandLine)
                
                // 写入 stdin
                val stdin = processHandler.process.outputStream
                stdin.write(text.toByteArray())
                stdin.close()
                
                // 获取结果
                val output = processHandler.runProcess(30_000)
                val translation = output.stdout.trim()
                
                // 回调(在 EDT 上)
                ApplicationManager.getApplication().invokeLater {
                    callback(translation)
                }
            } catch (e: Exception) {
                logger.error("Translation failed", e)
                ApplicationManager.getApplication().invokeLater {
                    callback("Error: ${e.message}")
                }
            }
        }
    }
}
```

### 6.5 已知坑点

#### ⚠️ 坑点 1: 在后台线程访问 PSI 未使用 Read Action

```kotlin
// ❌ 错误:后台线程直接访问 PSI
ApplicationManager.getApplication().executeOnPooledThread {
    val element = file.findElementAt(offset) // 可能崩溃!
}

// ✅ 正确:使用 Read Action
ApplicationManager.getApplication().executeOnPooledThread {
    val element = ReadAction.compute<PsiElement?, Throwable> {
        file.findElementAt(offset)
    }
}
```

#### ⚠️ 坑点 2: 在 Read Action 中执行耗时操作

```kotlin
// ❌ 错误:Read Action 中执行网络请求
ApplicationManager.getApplication().runReadAction {
    val data = file.text
    val translation = httpClient.post(data) // 阻塞其他线程!
}

// ✅ 正确:先读取数据,再执行耗时操作
val data = ApplicationManager.getApplication().runReadAction(Computable {
    file.text
})
val translation = httpClient.post(data) // 在外部执行
```

#### ⚠️ 坑点 3: 忘记检查任务取消

```kotlin
// ❌ 错误:长循环未检查取消
override fun run(indicator: ProgressIndicator) {
    for (i in 1..10000) {
        heavyWork(i) // 用户无法取消!
    }
}

// ✅ 正确:定期检查取消
override fun run(indicator: ProgressIndicator) {
    for (i in 1..10000) {
        ProgressManager.checkCanceled() // 或 indicator.checkCanceled()
        heavyWork(i)
    }
}
```

#### ⚠️ 坑点 4: EDT 上执行耗时操作

```kotlin
// ❌ 错误:Action 中直接执行耗时操作
override fun actionPerformed(e: AnActionEvent) {
    val result = longRunningTask() // UI 卡死!
    showDialog(result)
}

// ✅ 正确:使用后台任务
override fun actionPerformed(e: AnActionEvent) {
    ProgressManager.getInstance().run(
        object : Task.Backgroundable(project, "Processing", true) {
            private lateinit var result: String
            
            override fun run(indicator: ProgressIndicator) {
                result = longRunningTask()
            }
            
            override fun onSuccess() {
                showDialog(result) // 在 EDT 上显示结果
            }
        }
    )
}
```

### 6.6 性能和兼容性建议

#### 线程池管理

```kotlin
import com.intellij.util.concurrency.AppExecutorUtil

// 1. 使用应用级线程池
AppExecutorUtil.getAppExecutorService().submit {
    // 工作...
}

// 2. 创建有界线程池
val executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
    "MyPlugin-Worker",
    4 // 最大线程数
)

executor.submit {
    // 工作...
}

// 3. 延迟执行
AppExecutorUtil.getAppScheduledExecutorService().schedule(
    { /* 工作... */ },
    5,
    TimeUnit.SECONDS
)
```

#### 防抖和节流

```kotlin
import com.intellij.util.Alarm

@Service
class DebouncedService(private val project: Project) {
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)
    
    fun debounce(delayMs: Int, action: () -> Unit) {
        alarm.cancelAllRequests()
        alarm.addRequest(action, delayMs)
    }
}

// 使用示例:输入防抖
textField.document.addDocumentListener(object : DocumentAdapter() {
    override fun textChanged(e: DocumentEvent) {
        debouncedService.debounce(300) {
            // 300ms 后执行
            performSearch(textField.text)
        }
    }
})
```

---

## 总结

### 核心要点

1. **Inlay Hints**:
   - 使用新 API (Declarative/Code Vision) 优先
   - 避免 EDT 阻塞,异步加载数据
   - 使用缓存减少重复计算

2. **PSI**:
   - 所有 PSI 访问必须在 Read Action 中
   - 使用 `PsiRecursiveElementWalkingVisitor` 遍历
   - 使用 `SmartPsiElementPointer` 持有引用

3. **进程管理**:
   - 优先使用 `GeneralCommandLine` 而非 `ProcessBuilder`
   - 使用 `CapturingProcessHandler` 自动处理输出
   - 设置超时和字符编码

4. **配置持久化**:
   - Kotlin 推荐 `SimplePersistentStateComponent`
   - 使用消息总线通知配置变更
   - 注意依赖项设置(缓存失效)

5. **性能优化**:
   - 使用 `CachedValue` 缓存 PSI 相关计算
   - 监听文件变更及时清除缓存
   - 减少 PSI 遍历次数

6. **异步任务**:
   - 使用 `Task.Backgroundable` 执行长任务
   - 定期检查 `ProgressIndicator.isCanceled`
   - 在 EDT 上更新 UI

### 下一步

- 参考本文档实现插件核心功能
- 查阅 IntelliJ Platform SDK 官方文档获取最新 API
- 研究相关开源插件源码(Go Plugin, Rust Plugin)
- 进行充分的测试和性能优化
