# 开发指南

本文档介绍如何在本地开发和调试 codei18n-jetbrains 插件。

## 系统要求

- Java 21 (JDK 21)
- GoLand 2024.3+ 或 RustRover 2024.3+
- Gradle 8.x (通过 Gradle Wrapper 自动管理)

## IDE 配置

本项目配置为使用本地已安装的 IDE,而不是下载新的 IDE 实例。这样可以:

1. **节省磁盘空间**: 不需要下载额外的 IDE (通常 1GB+)
2. **加快构建速度**: 直接使用已安装的 IDE
3. **保持一致性**: 使用与日常开发相同的 IDE 版本

### 默认 IDE 路径

项目默认使用以下路径:

```
GoLand:    /Applications/GoLand.app
RustRover: /Applications/RustRover.app
```

### 自定义 IDE 路径

如果您的 IDE 安装在其他位置,有三种方式配置:

#### 方法 1: 修改 Makefile (推荐用于持久化配置)

编辑 `Makefile`:

```makefile
GOLAND_PATH := /custom/path/to/GoLand.app
RUSTROVER_PATH := /custom/path/to/RustRover.app
```

#### 方法 2: 使用 local.properties (推荐用于个人配置)

1. 复制模板文件:
   ```bash
   cp local.properties.template local.properties
   ```

2. 编辑 `local.properties`:
   ```properties
   intellij.localPath=/custom/path/to/GoLand.app
   ```

3. 这个文件会被 Git 忽略,不会提交到仓库

#### 方法 3: 命令行参数 (临时使用)

```bash
./gradlew runIde -Pintellij.localPath=/custom/path/to/IDE.app
```

## 构建和运行

### 使用 Makefile (推荐)

```bash
# 查看所有可用命令
make help

# 构建插件
make build

# 在 GoLand 中运行
make run-goland

# 在 RustRover 中运行  
make run-rustrover

# 运行测试
make test

# 构建发布包
make release

# 清理构建产物
make clean
```

### 使用 Gradle 命令

```bash
# 构建插件
./gradlew buildPlugin -Pintellij.localPath=/Applications/GoLand.app

# 运行插件
./gradlew runIde -Pintellij.localPath=/Applications/GoLand.app

# 运行测试
./gradlew test -Pintellij.localPath=/Applications/GoLand.app

# 构建发布包
./gradlew buildPlugin -Pintellij.localPath=/Applications/GoLand.app
```

## 开发流程

### 1. 快速测试循环

```bash
# 方式 1: 使用 Makefile
make run-goland

# 方式 2: 使用 Gradle
./gradlew runIde -Pintellij.localPath=/Applications/GoLand.app
```

这会启动一个带有插件的 IDE 实例,您可以:
- 打开测试项目
- 测试插件功能
- 查看日志和调试信息

### 2. 代码修改

1. 修改 Java 代码
2. 重新运行 `make run-goland` 或 `./gradlew runIde`
3. IDE 会自动重新加载插件

### 3. 调试

在 `runIde` 任务中,插件会以调试模式运行。您可以:

1. 在代码中设置断点
2. 使用 IntelliJ IDEA 打开本项目
3. 创建 Remote Debug 配置,连接到端口 5005
4. 运行 `make run-goland`
5. 触发断点进行调试

## 测试

### 单元测试

```bash
make test
```

### 手动测试

参考以下文档:
- [TEST_STRATEGY_RUST.md](TEST_STRATEGY_RUST.md) - Rust 支持测试策略
- [MANUAL_TEST_CHECKLIST.md](MANUAL_TEST_CHECKLIST.md) - 手动测试清单

## 构建发布版本

```bash
# 构建插件包
make release

# 插件包位置
ls -lh build/distributions/
```

生成的 `.zip` 文件可以:
1. 手动安装到 IDE: Settings -> Plugins -> ⚙️ -> Install Plugin from Disk
2. 发布到 JetBrains Plugin Marketplace

## 技术栈

- **语言**: Java 21
  - Records
  - Pattern Matching
  - Switch Expressions
  - var 关键字
  
- **平台**: IntelliJ Platform 2024.3
  - Code Folding API
  - Inlay Hints API
  - Documentation Provider API
  - Annotator API

- **构建工具**: Gradle 8.x
  - IntelliJ Platform Gradle Plugin 2.2.1

## 项目结构

```
codei18n-jetbrains/
├── src/
│   ├── main/
│   │   ├── java/              # Java 源代码
│   │   │   └── com/github/studyzy/codei18n/
│   │   │       ├── models/    # 数据模型 (Records)
│   │   │       ├── services/  # 业务逻辑
│   │   │       ├── providers/ # IDE 功能提供者
│   │   │       ├── settings/  # 插件设置
│   │   │       ├── listeners/ # 事件监听器
│   │   │       └── utils/     # 工具类
│   │   └── resources/
│   │       └── META-INF/
│   │           ├── plugin.xml       # 插件配置
│   │           ├── go-support.xml   # Go 支持
│   │           └── rust-support.xml # Rust 支持
│   └── test/
│       └── java/              # 测试代码
├── build.gradle.kts           # Gradle 构建配置
├── gradle.properties          # Gradle 属性
├── Makefile                   # Make 命令
└── local.properties.template  # 本地配置模板
```

## 常见问题

### 1. 构建时下载 IDE

**问题**: 运行构建时开始下载 GoLand/RustRover

**原因**: 没有正确配置本地 IDE 路径

**解决方案**:
```bash
# 确保使用了 -Pintellij.localPath 参数
./gradlew buildPlugin -Pintellij.localPath=/Applications/GoLand.app

# 或者使用 Makefile
make build
```

### 2. IDE 版本不兼容

**问题**: 插件无法在旧版本 IDE 中运行

**原因**: 插件要求 IDE 2024.3+

**解决方案**: 升级 IDE 到 2024.3 或更高版本

### 3. Java 版本错误

**问题**: 编译错误,提示 Java 版本不匹配

**原因**: 项目需要 Java 21

**解决方案**:
```bash
# 检查 Java 版本
java -version

# 应该显示 Java 21
# 如果不是,请安装 JDK 21
```

## 相关资源

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html)
- [codei18n CLI](https://github.com/studyzy/codei18n)

## 贡献

欢迎提交 Issue 和 Pull Request!

在提交 PR 之前,请确保:
1. 代码通过所有测试
2. 遵循现有的代码风格
3. 更新相关文档
