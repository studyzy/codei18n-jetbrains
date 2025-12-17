# codei18n-jetbrains

**JetBrains plugin for codei18n** - 在 IDE 中展示源代码注释的中英文翻译。

## 功能特性

- ✅ **注释翻译显示**: 在编辑器中直接查看注释的中英文翻译
- ✅ **多语言支持**: 
  - Go (.go)
  - Rust (.rs) 🆕
- ✅ **多种显示模式**:
  - 折叠显示 (Folding): 注释折叠显示为翻译，点击展开查看原文
  - 内联提示 (Inlay Hint): 注释旁边显示翻译
  - 工具提示 (Tooltip): 鼠标悬停显示翻译
  - 行号图标 (Gutter Icon): 点击行号旁图标显示翻译

## 支持的IDE

- GoLand 2023.3+
- IntelliJ IDEA 2023.3+
- RustRover 2023.3+

## 安装

1. 从 JetBrains Plugin Marketplace 安装（即将发布）
2. 或手动安装：
   - 下载最新的 `.zip` 发布包
   - Settings -> Plugins -> ⚙️ -> Install Plugin from Disk
   - 选择下载的 `.zip` 文件

## 使用方法

### 前置条件

1. 安装 [codei18n](https://github.com/studyzy/codei18n) CLI 工具：
   ```bash
   # 具体安装方法参考 codei18n 项目文档
   ```

2. 初始化项目配置：
   ```bash
   cd your-project
   codei18n init --source-lang en --local-lang zh-CN
   ```

3. 扫描并翻译注释：
   ```bash
   codei18n scan
   codei18n translate
   ```

### 插件配置

1. 打开 Settings -> Tools -> CodeI18n
2. 启用插件: ✓ Enable CodeI18n Plugin
3. 选择显示模式: Folding / Inlay Hint / Tooltip / Gutter Icon
4. 配置 CLI 路径（如果不在 PATH 中）

### 使用示例

#### Go 文件示例

```go
// Calculate the sum of two numbers
func add(a, b int) int {
    return a + b
}
```

在 IDE 中将显示：

```go
// 计算两个数字的和
func add(a, b int) int {
    return a + b
}
```

#### Rust 文件示例 🆕

```rust
// Calculate fibonacci numbers
fn fib(n: u32) -> u32 {
    /// Returns the nth fibonacci number
    if n <= 1 { n } else { fib(n-1) + fib(n-2) }
}
```

在 IDE 中将显示：

```rust
// 计算斐波那契数
fn fib(n: u32) -> u32 {
    /// 返回第 n 个斐波那契数
    if n <= 1 { n } else { fib(n-1) + fib(n-2) }
}
```

## 开发

### 构建项目

```bash
./gradlew build
```

### 运行沙盒 IDE

```bash
./gradlew runIde
```

### 测试

```bash
# 手动测试（推荐）
./gradlew runIde
# 然后在沙盒 IDE 中打开测试文件

# 查看测试策略
参见 TEST_STRATEGY_RUST.md
```

## 技术架构

本插件是 [codei18n](https://github.com/studyzy/codei18n) CLI 工具的 GUI 前端：

- **codei18n CLI**: 负责 AST 解析、注释提取、翻译管理
  - 使用 tree-sitter 解析 Rust 代码
  - 生成注释的唯一 ID（语义绑定）
  - 管理多语言映射文件
  
- **JetBrains Plugin**: 负责 UI 渲染和用户交互
  - 调用 CLI 的 `scan` 命令获取翻译数据
  - 在编辑器中渲染翻译结果
  - 提供多种显示模式

## 许可证

详见 [LICENSE](LICENSE) 文件

## 相关项目

- [codei18n](https://github.com/studyzy/codei18n) - 核心 CLI 工具
- [codei18n/docs](https://github.com/studyzy/codei18n/tree/main/docs) - 完整文档

## 贡献

欢迎提交 Issue 和 Pull Request！

开发文档：
- [快速开始](specs/002-add-rust-support/quickstart.md)
- [测试策略](TEST_STRATEGY_RUST.md)
- [手动测试清单](MANUAL_TEST_CHECKLIST.md)

