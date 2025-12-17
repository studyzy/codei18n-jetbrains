# 规格说明: 升级到最新 IDE 版本支持

## 概述

将 codei18n-jetbrains 插件升级到只支持最新版本的 JetBrains IDE (2024.3+),使用 Java 21 和最新的 IDE API 特性。

## 目标

1. **IDE 版本升级**: 只支持 GoLand 2024.3+ 和 RustRover 2024.3+
2. **Java 版本升级**: 从 Java 17 升级到 Java 21
3. **本地 IDE 开发**: 使用本地已安装的 IDE,而不下载新的 IDE
4. **现代化代码**: 使用 Java 21 新特性优化代码

## 技术要求

### IDE 版本

- **最低版本**: 2024.3 (build 243)
- **最高版本**: 2025.3.* (build 253.*)
- **支持的 IDE**:
  - GoLand 2024.3+
  - RustRover 2024.3+
  - IntelliJ IDEA 2024.3+

### Java 版本

- **编译版本**: Java 21
- **运行时版本**: Java 21
- **特性使用**:
  - Records (Java 16+)
  - Pattern Matching (Java 21)
  - Switch Expressions (Java 14+)
  - var 关键字 (Java 10+)

### 构建系统

- **Gradle**: 8.11+ (通过 Gradle Wrapper)
- **Java Toolchain**: 自动下载 Java 21
- **IntelliJ Platform Gradle Plugin**: 2.2.1

## 配置更改

### gradle.properties

```properties
pluginSinceBuild=243
pluginUntilBuild=253.*
platformVersion=2024.3
javaVersion=21
```

### build.gradle.kts

```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}
```

## 本地 IDE 配置

### 默认路径

- GoLand: `/Applications/GoLand.app`
- RustRover: `/Applications/RustRover.app`

### Makefile 配置

```makefile
GOLAND_PATH := /Applications/GoLand.app
RUSTROVER_PATH := /Applications/RustRover.app

build:
	./gradlew buildPlugin -Pintellij.localPath=$(GOLAND_PATH)

run-goland:
	./gradlew runIde -Pintellij.localPath=$(GOLAND_PATH)

run-rustrover:
	./gradlew runIde -Pintellij.localPath=$(RUSTROVER_PATH)
```

### Gradle 参数

```bash
# 使用本地 GoLand
./gradlew buildPlugin -Pintellij.localPath=/Applications/GoLand.app

# 使用本地 RustRover
./gradlew runIde -Pintellij.localPath=/Applications/RustRover.app
```

## 代码现代化

### Java Records

将 POJO 类转换为 Records:

**CliResponse.java**:
```java
public record CliResponse(
    String version,
    List<CommentData> comments,
    List<String> errors
) {
    public record CommentData(
        String id,
        String file,
        String language,
        String symbol,
        TextRange range,
        String sourceText,
        String localizedText
    ) {
        public String getTranslation() {
            return localizedText;
        }
    }

    public record TextRange(
        int startLine,
        int startCol,
        int endLine,
        int endCol
    ) {}
}
```

**TranslatedComment.java**:
```java
public record TranslatedComment(
    String commentId,
    String sourceText,
    String translation,
    int startOffset,
    int endOffset,
    int lineNumber,
    CommentType commentType,
    String symbolPath
) {
    public enum CommentType {
        LINE, BLOCK, DOC
    }
}
```

### 现代化语法

**CliService.java**:
```java
// 使用 var
var args = new ArrayList<String>();

// 使用 List.of()
return executeCommand(cliPath, List.of("version"), null, 5000);

// 使用 addAll()
args.addAll(List.of("--source-lang", sourceLang));
```

## 依赖版本

### 测试依赖

- JUnit Jupiter: 5.11.4 (从 5.10.0 升级)
- Mockito: 5.14.2 (从 5.6.0 升级)

### IDE 依赖

- GoLand: 2024.3
- RustRover: 2024.3
- IntelliJ IDEA Community: 2024.3

## 构建优化

### 优势

1. **节省磁盘空间**: 不下载额外的 IDE (~1GB+)
2. **加快构建速度**: 直接使用本地 IDE
3. **保持一致性**: 使用日常开发相同的 IDE 版本
4. **减少样板代码**: Records 减少约 50% 代码量

### 构建时间对比

| 操作 | 下载 IDE | 本地 IDE |
|------|----------|----------|
| 首次构建 | ~5-10 分钟 | ~10 秒 |
| 增量构建 | ~10 秒 | ~10 秒 |
| 磁盘占用 | +1GB | 0 |

## 兼容性

### 不再支持

- GoLand < 2024.3
- RustRover < 2024.3
- IntelliJ IDEA < 2024.3
- Java < 21

### 迁移指南

用户需要:

1. 升级 IDE 到 2024.3 或更高版本
2. 无需安装 Java 21 (Gradle 会自动下载)

开发者需要:

1. 安装 GoLand 2024.3+ 或 RustRover 2024.3+
2. 让 Gradle 自动下载 Java 21
3. 更新代码以适应新的 record 访问器语法

## 验收标准

- [ ] 使用 `make build` 成功构建插件
- [ ] 使用 `make run-goland` 在 GoLand 中运行
- [ ] 使用 `make run-rustrover` 在 RustRover 中运行
- [ ] 所有测试通过
- [ ] 插件在 GoLand 2024.3 中正常工作
- [ ] 插件在 RustRover 2024.3 中正常工作
- [ ] Records 正确实现并可访问
- [ ] 文档已更新

## 风险与缓解

### 风险 1: Java 21 不可用

**缓解**: 使用 Gradle Java Toolchain 自动下载 Java 21

### 风险 2: 本地 IDE 路径不同

**缓解**: 提供三种配置方式:
1. 修改 Makefile
2. 使用 local.properties
3. 命令行参数 `-Pintellij.localPath`

### 风险 3: Record 语法变更

**缓解**: Records 是稳定特性,从 Java 16 开始支持,语法不会变更

## 参考资料

- [IntelliJ Platform SDK - Version Compatibility](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html)
- [Java 21 Records](https://openjdk.org/jeps/395)
- [Gradle Java Toolchain](https://docs.gradle.org/current/userguide/toolchains.html)
- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html)
