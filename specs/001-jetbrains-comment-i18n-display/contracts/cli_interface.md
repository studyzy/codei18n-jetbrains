# CLI 接口合同

**功能**: JetBrains IDE 插件与 codei18n CLI 工具的接口定义
**版本**: 1.0
**CLI 版本要求**: codei18n v0.1.0+

---

## 概述

本文档定义 JetBrains IDE 插件与 codei18n CLI 工具之间的接口合同,包括命令格式、参数、输入输出格式和错误处理规范。

---

## 1. 扫描文件获取翻译

### 命令

```bash
codei18n scan --file <path> --lang <lang> --format json [--with-translations]
```

### 参数

| 参数 | 必填 | 类型 | 说明 | 示例 |
|------|------|------|------|------|
| `--file` | 是 | String | 要扫描的 Go 文件路径 (绝对或相对) | `/path/to/main.go` |
| `--lang` | 是 | String | 目标语言代码 (ISO 639-1 + ISO 3166-1) | `zh-CN`, `ja`, `ko` |
| `--format` | 是 | String | 输出格式,固定为 `json` | `json` |
| `--with-translations` | 否 | Flag | 是否包含翻译文本 (需要映射文件存在) | - |

### 输入

无 stdin 输入。

### 输出 (stdout)

**Content-Type**: `application/json`

**成功响应**:

```json
{
  "version": "0.1.0",
  "file": "/path/to/main.go",
  "language": "go",
  "comments": [
    {
      "id": "a8f9c3e2d1b0f5a7",
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
    },
    {
      "id": "b7e6d5c4a3b2f1e0",
      "file": "main.go",
      "language": "go",
      "symbol": "package.main.main",
      "range": {
        "startLine": 15,
        "startCol": 1,
        "endLine": 15,
        "endCol": 20
      },
      "sourceText": "Main entry point",
      "translation": null
    }
  ],
  "errors": []
}
```

**字段说明**:

- `version`: CLI 工具版本号
- `file`: 扫描的文件路径
- `language`: 编程语言 (固定为 "go")
- `comments`: 注释数组
  - `id`: 注释唯一标识 (SHA1 哈希)
  - `file`: 文件路径 (相对于项目根)
  - `language`: 编程语言
  - `symbol`: 绑定的语义符号 (如 `package.main.FuncName`)
  - `range`: 文本范围
    - `startLine`: 起始行号 (从 1 开始)
    - `startCol`: 起始列号 (从 1 开始)
    - `endLine`: 结束行号
    - `endCol`: 结束列号
  - `sourceText`: 原始注释文本 (不含 `//` 或 `/*`)
  - `translation`: 翻译文本 (如果 `--with-translations` 且映射存在,否则为 `null`)
- `errors`: 错误消息数组 (为空表示成功)

### 错误输出 (stderr)

**格式**: 纯文本

**示例**:

```
Error: file not found: /path/to/main.go
Error: failed to parse Go file: syntax error at line 10
Error: mapping file not found: .codei18n/mappings.json
```

### 退出码

| 退出码 | 说明 |
|--------|------|
| 0 | 成功 |
| 1 | 文件不存在 |
| 2 | 文件解析失败 (语法错误) |
| 3 | 映射文件不存在或损坏 |
| 127 | CLI 工具未找到 |

### 性能要求

- 单文件扫描 (< 1000 行): < 500ms
- 大文件扫描 (>1000 行): < 2s

### 超时处理

- 插件应设置 5 秒超时
- 超时后终止进程,记录警告日志

---

## 2. 通过 stdin 扫描文件 (未保存的缓冲区)

### 命令

```bash
codei18n scan --stdin --file <path> --lang <lang> --format json [--with-translations]
```

### 参数

| 参数 | 必填 | 类型 | 说明 | 示例 |
|------|------|------|------|------|
| `--stdin` | 是 | Flag | 从 stdin 读取文件内容 | - |
| `--file` | 是 | String | 文件路径 (用于生成 comment ID) | `main.go` |
| `--lang` | 是 | String | 目标语言代码 | `zh-CN` |
| `--format` | 是 | String | 输出格式,固定为 `json` | `json` |
| `--with-translations` | 否 | Flag | 是否包含翻译文本 | - |

### 输入 (stdin)

**Content-Type**: `text/plain; charset=utf-8`

**示例**:

```go
package main

// Calculate account balance
func CalculateBalance() {
    // TODO: implement
}
```

### 输出 (stdout)

与命令 1 相同的 JSON 格式。

### 使用场景

- 用户正在编辑文件但未保存
- 避免频繁的文件读写
- 实时显示翻译

---

## 3. 检查 CLI 版本

### 命令

```bash
codei18n version
```

### 参数

无。

### 输出 (stdout)

**格式**: 纯文本

**示例**:

```
codei18n version 0.1.0
```

### 退出码

| 退出码 | 说明 |
|--------|------|
| 0 | 成功 |
| 127 | CLI 工具未找到 |

### 用途

- 插件启动时检测 CLI 是否可用
- 验证 CLI 版本是否兼容 (最低 v0.1.0)

---

## 4. 初始化项目配置

### 命令

```bash
codei18n init [--source-lang <lang>] [--local-lang <lang>]
```

### 参数

| 参数 | 必填 | 类型 | 默认值 | 说明 |
|------|------|------|--------|------|
| `--source-lang` | 否 | String | `en` | 源语言代码 |
| `--local-lang` | 否 | String | `zh-CN` | 本地语言代码 |

### 输出 (stdout)

**格式**: 纯文本

**示例**:

```
Initialized CodeI18n project at /path/to/project
Created .codei18n/config.json
Created .codei18n/mappings.json
```

### 用途

- 用户首次使用插件时,提示运行此命令初始化项目
- 生成 `.codei18n/config.json` 和 `.codei18n/mappings.json`

---

## 错误处理规范

### 插件侧处理

1. **CLI 未找到** (退出码 127):
   - 显示通知: "CodeI18n CLI 未找到,请安装或配置路径"
   - 提供"打开设置"按钮
   - 禁用翻译显示功能

2. **文件不存在** (退出码 1):
   - 记录警告日志
   - 跳过该文件,不显示翻译

3. **文件解析失败** (退出码 2):
   - 显示通知: "文件解析失败: <stderr 内容>"
   - 不阻塞 IDE 其他功能

4. **映射文件不存在** (退出码 3):
   - 显示通知: "CodeI18n 项目未初始化,请运行 codei18n init"
   - 提供"初始化项目"操作按钮

5. **超时** (5 秒):
   - 记录警告日志: "CLI execution timeout for file: <path>"
   - 不显示翻译,不阻塞 IDE

6. **JSON 解析失败**:
   - 记录错误日志: "Invalid JSON response from CLI"
   - 显示通知: "CLI 返回无效数据,请检查版本"

### CLI 侧要求

- 所有正常输出必须写入 stdout
- 所有错误信息必须写入 stderr
- stdout 必须严格为 JSON 格式 (不包含日志)
- 使用标准退出码表示错误类型

---

## 兼容性保证

### 向后兼容

- CLI v0.1.x 版本保证 API 稳定
- 新增字段使用可选字段 (插件忽略未知字段)
- 不移除或重命名现有字段

### 版本检测

插件在启动时调用 `codei18n version`,解析版本号:

```java
String versionOutput = executeCommand("codei18n version");
// 解析: "codei18n version 0.1.0" -> "0.1.0"
String version = versionOutput.split(" ")[2];

if (compareVersion(version, "0.1.0") < 0) {
    showError("CodeI18n CLI 版本过低,最低要求 v0.1.0");
}
```

---

## 性能优化建议

1. **批量处理**: 如果需要扫描多个文件,考虑一次性传递多个文件路径 (未来扩展)
2. **缓存**: 插件缓存 CLI 响应,避免重复调用
3. **异步调用**: 在后台线程执行 CLI,不阻塞 IDE UI
4. **超时控制**: 设置合理超时,避免长时间等待

---

## 测试用例

### 测试 1: 正常扫描文件

**输入**:
```bash
codei18n scan --file test.go --lang zh-CN --format json --with-translations
```

**预期输出**:
- 退出码: 0
- stdout: 有效的 JSON,包含注释数组和翻译
- stderr: 空

### 测试 2: 文件不存在

**输入**:
```bash
codei18n scan --file nonexistent.go --lang zh-CN --format json
```

**预期输出**:
- 退出码: 1
- stdout: 空或错误 JSON
- stderr: "Error: file not found: nonexistent.go"

### 测试 3: 映射文件缺失

**输入**:
```bash
codei18n scan --file test.go --lang zh-CN --format json --with-translations
```

**预期输出**:
- 退出码: 3
- stdout: 空或错误 JSON
- stderr: "Error: mapping file not found"

### 测试 4: stdin 输入

**输入**:
```bash
echo 'package main\n// Test comment' | codei18n scan --stdin --file test.go --lang zh-CN --format json
```

**预期输出**:
- 退出码: 0
- stdout: 有效的 JSON,包含注释
- stderr: 空

---

**文档状态**: CLI 接口合同定义完成
**下一步**: 生成 quickstart.md
