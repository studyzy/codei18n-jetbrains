# 运行指南

由于本地 GoLand 2025.3 与插件开发环境存在兼容性问题,推荐使用以下方式开发和测试插件。

## 问题说明

使用本地安装的 GoLand 2025.3 运行插件时,会遇到类加载错误:
```
java.lang.ClassNotFoundException: com.intellij.platform.core.nio.fs.MultiRoutingFileSystemProvider
```

这是因为本地IDE的JBR (JetBrains Runtime)与插件开发环境不完全兼容。

## 解决方案

### 方案 1: 使用下载的 IDE (推荐)

让 Gradle 自动下载 GoLand 2024.3 用于开发:

```bash
# 运行插件 (首次会下载 GoLand 2024.3, 约 1GB)
make run-goland

# 或直接使用 Gradle
./gradlew runIde -Pintellij.type=GO
```

**优点**:
- ✅ 完全兼容,没有类加载问题
- ✅ 使用稳定的 2024.3 版本
- ✅ 自动配置正确的JBR

**缺点**:
- ❌ 首次下载约 1GB
- ❌ 启动稍慢(需要解压IDE)

### 方案 2: 快速构建 + 手动测试

快速构建后,在本地 IDE 中手动安装测试:

```bash
# 1. 使用本地 GoLand 快速构建 (~10秒)
make build-local

# 2. 手动安装插件
# - 打开本地 GoLand 2025.3
# - Settings -> Plugins -> ⚙️ -> Install Plugin from Disk
# - 选择 build/distributions/codei18n-jetbrains-0.1.0.zip
# - 重启 IDE
```

**优点**:
- ✅ 构建超快(使用本地IDE)
- ✅ 在真实环境中测试

**缺点**:
- ❌ 需要手动安装和重启
- ❌ 每次修改都需要重新安装

### 方案 3: 降级本地 GoLand (不推荐)

如果您希望使用本地 IDE 运行,可以:

1. 卸载 GoLand 2025.3
2. 安装 GoLand 2024.3
3. 使用本地IDE运行

```bash
# 下载 GoLand 2024.3
# https://www.jetbrains.com/goland/download/other.html

# 运行
./gradlew runIde -Pintellij.localPath=/Applications/GoLand.app
```

## 推荐工作流程

### 开发流程

1. **修改代码**
2. **快速构建** (使用本地IDE,10秒)
   ```bash
   make build-local
   ```
3. **手动测试** (在本地 GoLand 2025.3 中安装插件)

### 发布前测试

1. **完整测试** (使用下载的IDE)
   ```bash
   make run-goland
   ```
2. **验证功能**
3. **构建发布包**
   ```bash
   make release
   ```

## 命令参考

```bash
# 构建 (下载IDE)
make build

# 构建 (使用本地IDE,更快)
make build-local

# 运行 GoLand (下载IDE)
make run-goland

# 运行 RustRover (下载IDE)
make run-rustrover

# 构建发布包
make release

# 清理
make clean

# 查看帮助
make help
```

## 常见问题

### Q: 为什么不能使用本地 GoLand 2025.3?

A: GoLand 2025.3 比插件配置的 2024.3 更新,JBR 内部结构有变化,导致类加载问题。这是IntelliJ Platform开发的已知问题。

### Q: 下载的IDE占用多少空间?

A: 约 1GB,解压后约 1.5GB。会缓存在 `~/.gradle/caches/modules-2/files-2.1/` 下。

### Q: 可以删除下载的IDE吗?

A: 可以运行 `./gradlew clean` 清理构建产物,但不会删除缓存的IDE。要清理缓存:
```bash
rm -rf ~/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.goland/
```

### Q: 构建很慢怎么办?

A: 使用 `make build-local` 用于日常开发,只在发布前使用 `make build`。

## 总结

**日常开发**:
- 使用 `make build-local` 快速构建
- 手动安装到本地 GoLand 2025.3 测试

**发布前**:
- 使用 `make run-goland` 完整测试
- 使用 `make release` 构建发布包

这样可以兼顾开发速度和测试完整性。
