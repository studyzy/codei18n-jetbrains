# 快速开始

本文档帮助您快速开始开发 codei18n-jetbrains 插件。

## 1. 环境准备

### 必需
- ✅ GoLand 2024.3+ 或 RustRover 2024.3+
- ✅ Git

### 可选
- Java 21 (如果没有,Gradle 会自动下载,首次构建会下载约 190MB)

### 检查环境

```bash
# 检查 GoLand 是否已安装
ls /Applications/GoLand.app

# 检查 RustRover 是否已安装 (可选)
ls /Applications/RustRover.app
```

**注意**: 不需要手动安装 Java 21,Gradle 会在首次构建时自动下载。

## 2. 克隆项目

```bash
git clone https://github.com/studyzy/codei18n-jetbrains.git
cd codei18n-jetbrains
```

## 3. 快速构建和运行

使用 Makefile (推荐):

```bash
# 查看所有可用命令
make help

# 构建插件
make build

# 在 GoLand 中运行插件
make run-goland
```

## 4. 在 IDE 中测试

当 `make run-goland` 启动 IDE 后:

1. **打开测试项目**
   - 在启动的 GoLand 中打开一个 Go 项目
   - 或者使用项目中的测试文件

2. **配置插件**
   - Settings -> Tools -> CodeI18n
   - 勾选 "Enable CodeI18n Plugin"
   - 选择显示模式: "Folding" (推荐)

3. **测试功能**
   - 打开一个 Go 文件
   - 注释应该显示为中文翻译
   - 点击折叠区域查看英文原文

## 5. 常用命令

```bash
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

## 6. 开发循环

典型的开发流程:

```bash
# 1. 修改代码
vim src/main/java/com/github/studyzy/codei18n/...

# 2. 运行测试 (可选)
make test

# 3. 在 IDE 中测试
make run-goland

# 4. 在弹出的 IDE 中验证功能
# 5. 重复步骤 1-4
```

## 7. 自定义配置

### 使用不同的 IDE 路径

如果您的 IDE 安装在其他位置:

**方法 1: 修改 Makefile**

编辑 `Makefile`:
```makefile
GOLAND_PATH := /custom/path/to/GoLand.app
```

**方法 2: 使用命令行参数**

```bash
./gradlew runIde -Pintellij.localPath=/custom/path/to/GoLand.app
```

## 8. 故障排除

### 问题: 构建时下载 IDE

如果构建时开始下载 GoLand,说明没有正确使用本地 IDE。

**解决方案**:
```bash
# 使用 Makefile
make build

# 或者明确指定路径
./gradlew buildPlugin -Pintellij.localPath=/Applications/GoLand.app
```

### 问题: Java 版本错误

确保使用 Java 21:

```bash
java -version
# 应该显示: openjdk version "21.x.x"
```

### 问题: IDE 版本太旧

插件需要 IDE 2024.3+,请升级您的 IDE。

## 9. 下一步

- 📖 阅读 [DEVELOPMENT.md](DEVELOPMENT.md) 了解详细开发指南
- 🧪 查看 [TEST_STRATEGY_RUST.md](TEST_STRATEGY_RUST.md) 了解测试策略
- 🔧 参考 [README.md](README.md) 了解项目功能

## 10. 获取帮助

- 提交 Issue: https://github.com/studyzy/codei18n-jetbrains/issues
- 查看文档: https://github.com/studyzy/codei18n

---

Happy Coding! 🚀
