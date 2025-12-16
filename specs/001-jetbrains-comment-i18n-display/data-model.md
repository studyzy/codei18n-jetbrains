# 数据模型: JetBrains IDE 代码注释国际化显示插件

**功能**: JetBrains IDE 代码注释国际化显示插件
**日期**: 2025-12-16
**输入**: 功能规范 spec.md 中的关键实体

---

## 概述

本文档定义插件的核心数据模型,包括翻译注释、CLI 配置、插件设置和缓存数据等实体。所有模型遵循 Java Bean 规范,支持序列化和反序列化。

---

## 核心实体

### 1. TranslatedComment (翻译注释)

表示一条注释及其翻译信息。

**字段**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| commentId | String | 是 | 注释的唯一标识符 (由 CLI 提供的 SHA1 哈希) |
| sourceText | String | 是 | 原始注释文本 (英文,不包含注释符号 `//` 或 `/*`) |
| translation | String | 是 | 翻译后的文本 (中文) |
| startOffset | int | 是 | 注释在文件中的起始偏移量 (字节) |
| endOffset | int | 是 | 注释在文件中的结束偏移量 (字节) |
| lineNumber | int | 是 | 注释所在行号 (从 1 开始) |
| commentType | CommentType | 是 | 注释类型 (LINE, BLOCK, DOC) |
| symbolPath | String | 否 | 注释绑定的语义符号路径 (如 "package.main.CalculateBalance") |

**Java 实现**:

```java
public class TranslatedComment {
    private String commentId;
    private String sourceText;
    private String translation;
    private int startOffset;
    private int endOffset;
    private int lineNumber;
    private CommentType commentType;
    private String symbolPath;
    
    // Getters and Setters
    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }
    
    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }
    
    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }
    
    public int getStartOffset() { return startOffset; }
    public void setStartOffset(int startOffset) { this.startOffset = startOffset; }
    
    public int getEndOffset() { return endOffset; }
    public void setEndOffset(int endOffset) { this.endOffset = endOffset; }
    
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    
    public CommentType getCommentType() { return commentType; }
    public void setCommentType(CommentType commentType) { this.commentType = commentType; }
    
    public String getSymbolPath() { return symbolPath; }
    public void setSymbolPath(String symbolPath) { this.symbolPath = symbolPath; }
}

public enum CommentType {
    LINE,   // 行注释 //
    BLOCK,  // 块注释 /* */
    DOC     // 文档注释 (函数/类型前的注释)
}
```

**验证规则**:
- `commentId` 不能为空或 null
- `originalText` 和 `translatedText` 不能为空字符串
- `startOffset` < `endOffset`
- `lineNumber` >= 1

**状态转换**:
- 无状态转换 (不可变对象,创建后不修改)

---

### 2. CliConfiguration (CLI 配置)

表示 codei18n CLI 工具的配置信息。

**字段**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cliPath | String | 是 | CLI 可执行文件的绝对路径或命令名 (如 "codei18n") |
| targetLanguage | String | 是 | 目标翻译语言代码 (如 "zh-CN", "ja", "ko") |
| timeout | int | 是 | CLI 调用超时时间 (毫秒, 默认 5000) |
| workingDirectory | String | 否 | CLI 工作目录 (默认为项目根目录) |
| environmentVariables | Map<String, String> | 否 | 额外的环境变量 |

**Java 实现**:

```java
public class CliConfiguration {
    private String cliPath = "codei18n";
    private String targetLanguage = "zh-CN";
    private int timeout = 5000;
    private String workingDirectory;
    private Map<String, String> environmentVariables = new HashMap<>();
    
    // Getters and Setters
    public String getCliPath() { return cliPath; }
    public void setCliPath(String cliPath) { this.cliPath = cliPath; }
    
    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }
    
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    
    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }
    
    public Map<String, String> getEnvironmentVariables() { return environmentVariables; }
    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }
}
```

**验证规则**:
- `cliPath` 不能为空
- `targetLanguage` 必须符合 ISO 639-1 + ISO 3166-1 格式 (如 "zh-CN")
- `timeout` 必须在 1000 - 60000 范围内 (1-60 秒)

---

### 3. PluginSettings (插件设置)

表示插件的用户配置,持久化到 IDE 配置文件。

**字段**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| enabled | boolean | 是 | 是否启用翻译显示 (默认 true) |
| displayMode | DisplayMode | 是 | 显示模式 (默认 INLAY_HINT) |
| cliConfiguration | CliConfiguration | 是 | CLI 配置 |
| cacheEnabled | boolean | 是 | 是否启用缓存 (默认 true) |
| maxCacheSize | int | 是 | 最大缓存文件数 (默认 100) |

**Java 实现**:

```java
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(
    name = "CodeI18nSettings",
    storages = @Storage("codei18n.xml")
)
public class PluginSettings implements PersistentStateComponent<PluginSettings> {
    
    public boolean enabled = true;
    public DisplayMode displayMode = DisplayMode.INLAY_HINT;
    public CliConfiguration cliConfiguration = new CliConfiguration();
    public boolean cacheEnabled = true;
    public int maxCacheSize = 100;
    
    @Override
    public PluginSettings getState() {
        return this;
    }
    
    @Override
    public void loadState(PluginSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    public static PluginSettings getInstance() {
        return ApplicationManager.getApplication()
            .getService(PluginSettings.class);
    }
}

public enum DisplayMode {
    INLAY_HINT,    // 内联提示 (注释下方灰色小字)
    TOOLTIP,       // 工具提示 (鼠标悬停显示)
    GUTTER_ICON    // 行号旁图标 (点击显示)
}
```

**验证规则**:
- `maxCacheSize` 必须在 10 - 1000 范围内

**默认值**:
- `enabled = true`
- `displayMode = INLAY_HINT`
- `cacheEnabled = true`
- `maxCacheSize = 100`

---

### 4. TranslationCache (翻译缓存)

表示单个文件的翻译数据缓存。

**字段**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| filePath | String | 是 | 文件的绝对路径 |
| translations | List<TranslatedComment> | 是 | 翻译列表 |
| timestamp | long | 是 | 缓存创建时间戳 (毫秒) |
| mappingFileHash | String | 是 | 映射文件的 MD5 哈希值 (用于检测变更) |
| fileModificationStamp | long | 是 | 文件修改时间戳 (用于检测文件变更) |

**Java 实现**:

```java
public class TranslationCache {
    private String filePath;
    private List<TranslatedComment> translations;
    private long timestamp;
    private String mappingFileHash;
    private long fileModificationStamp;
    
    public TranslationCache(String filePath, List<TranslatedComment> translations) {
        this.filePath = filePath;
        this.translations = translations;
        this.timestamp = System.currentTimeMillis();
        this.mappingFileHash = calculateMappingHash();
        this.fileModificationStamp = getFileModificationStamp(filePath);
    }
    
    // 检查缓存是否有效
    public boolean isValid(String currentMappingHash, long currentFileStamp) {
        return this.mappingFileHash.equals(currentMappingHash) &&
               this.fileModificationStamp == currentFileStamp;
    }
    
    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - timestamp > ttlMillis;
    }
    
    // Getters
    public String getFilePath() { return filePath; }
    public List<TranslatedComment> getTranslations() { return translations; }
    public long getTimestamp() { return timestamp; }
    public String getMappingFileHash() { return mappingFileHash; }
    public long getFileModificationStamp() { return fileModificationStamp; }
}
```

**验证规则**:
- 缓存失效条件:
  - 文件内容变更 (`fileModificationStamp` 改变)
  - 映射文件变更 (`mappingFileHash` 改变)
  - 缓存过期 (超过 TTL,默认 1 小时)

---

### 5. CliResponse (CLI 响应)

表示 codei18n CLI 返回的 JSON 数据结构。

**字段**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| version | String | 是 | CLI 版本号 (如 "0.1.0") |
| comments | List<CommentData> | 是 | 注释数据列表 |
| errors | List<String> | 否 | 错误消息列表 |

**CommentData 子结构**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | String | 是 | 注释 ID (SHA1 哈希) |
| file | String | 是 | 文件路径 (相对于项目根目录) |
| language | String | 是 | 编程语言 (固定为 "go") |
| symbol | String | 否 | 语义符号路径 |
| range | TextRange | 是 | 文本范围 |
| sourceText | String | 是 | 原始注释文本 (英文) |
| translation | String | 否 | 翻译文本 (中文,可能为空) |

**TextRange 子结构**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| startLine | int | 是 | 起始行号 (从 1 开始) |
| startCol | int | 是 | 起始列号 (从 1 开始) |
| endLine | int | 是 | 结束行号 |
| endCol | int | 是 | 结束列号 |

**Java 实现**:

```java
public class CliResponse {
    private String version;
    private List<CommentData> comments;
    private List<String> errors;
    
    // Getters and Setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public List<CommentData> getComments() { return comments; }
    public void setComments(List<CommentData> comments) { this.comments = comments; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}

public class CommentData {
    private String id;
    private String file;
    private String language;
    private String symbol;
    private TextRange range;
    private String sourceText;
    private String translation;
    
    // Getters and Setters (略)
}

public class TextRange {
    private int startLine;
    private int startCol;
    private int endLine;
    private int endCol;
    
    // Getters and Setters (略)
}
```

**JSON 示例**:

```json
{
  "version": "0.1.0",
  "comments": [
    {
      "id": "a8f9c3e2d1b0",
      "file": "main.go",
      "language": "go",
      "symbol": "package.main.CalculateBalance",
      "range": {
        "startLine": 10,
        "startCol": 1,
        "endLine": 10,
        "endCol": 30
      },
      "sourceText": "Calculate account balance",
      "translation": "计算账户余额"
    }
  ],
  "errors": []
}
```

---

## 实体关系图

```
PluginSettings
    │
    ├──> CliConfiguration (1:1)
    │
    └──> DisplayMode (enum)

TranslationCache
    │
    └──> List<TranslatedComment> (1:N)

CliResponse
    │
    └──> List<CommentData> (1:N)
             │
             └──> TextRange (1:1)

TranslatedComment
    │
    └──> CommentType (enum)
```

---

## 数据流转

1. **用户配置 → 插件设置**:
   - 用户在设置界面修改配置
   - `PluginSettingsConfigurable` 调用 `PluginSettings.loadState()`
   - 配置持久化到 `codei18n.xml`

2. **文件打开 → CLI 调用**:
   - 用户打开 Go 文件
   - `TranslationService` 检查缓存
   - 缓存未命中,调用 `CliService.scanFile()`
   - `CliService` 使用 `CliConfiguration` 构建命令行
   - 执行 CLI,获取 JSON 响应

3. **CLI 响应 → 翻译注释**:
   - `CliService` 解析 JSON 为 `CliResponse`
   - 转换 `CommentData` 为 `TranslatedComment`
   - 存入 `TranslationCache`

4. **翻译注释 → Inlay Hints 显示**:
   - `InlayHintsProvider` 从 `TranslationService` 获取 `TranslatedComment` 列表
   - 根据 `startOffset` 和 `endOffset` 定位注释位置
   - 创建 `InlayPresentation` 显示 `translatedText`

---

## 持久化策略

| 实体 | 持久化方式 | 存储位置 |
|------|-----------|---------|
| PluginSettings | PersistentStateComponent (XML) | `~/.config/JetBrains/GoLand/options/codei18n.xml` |
| TranslationCache | 内存缓存 (LRU) | 内存 |
| CliResponse | 临时对象 | 无持久化 |
| TranslatedComment | 临时对象 | 无持久化 |

---

## 版本演进

**当前版本**: 1.0 (MVP)

**未来扩展**:
- 支持多目标语言 (同时显示日语、韩语等)
- 支持用户自定义翻译 (覆盖 CLI 提供的翻译)
- 支持翻译历史记录和回滚

---

**文档状态**: 数据模型设计完成
**下一步**: 生成 contracts/ (CLI 调用合同)
