# 快速开始: Rust 支持开发

**目标**: 设置开发环境并验证 Rust 注释翻译功能。

## 前置条件

1. **JDK 21**: 确保已安装并配置 `JAVA_HOME`.
2. **Gradle**: 项目使用 Gradle Wrapper, 无需手动安装.
3. **GoLand / IntelliJ IDEA**: 用于开发和调试插件.
4. **codei18n CLI**: 确保 `codei18n` 已安装并在 PATH 中.
   ```bash
   codei18n version
   ```

## 环境配置

### 1. 初始化测试项目

在项目根目录创建 `.codei18n/config.json`:
```bash
cd /path/to/codei18n-jetbrains
codei18n init --source-lang en --local-lang zh-CN
```

验证配置文件已创建:
```bash
cat .codei18n/config.json
# 应该看到:
# {
#   "sourceLanguage": "en",
#   "localLanguage": "zh-CN",
#   ...
# }
```

### 2. 验证 CLI Rust 支持

创建测试 Rust 文件:
```bash
mkdir -p test_data
cat > test_data/sample.rs << 'EOF'
// Calculate fibonacci numbers
fn fib(n: u32) -> u32 {
    /// Returns the nth fibonacci number
    if n <= 1 { n } else { fib(n-1) + fib(n-2) }
}
EOF
```

测试 CLI 扫描:
```bash
codei18n scan --file test_data/sample.rs --format json --with-translations
```

预期输出:
```json
{
  "file": "test_data/sample.rs",
  "comments": [
    {
      "id": "...",
      "range": {...},
      "sourceText": "Calculate fibonacci numbers",
      "translation": ""  // 首次扫描无翻译
    }
  ]
}
```

触发翻译:
```bash
codei18n translate
```

重新扫描验证翻译:
```bash
codei18n scan --file test_data/sample.rs --with-translations
```

## 运行与调试

1. **启动沙盒 IDE**:
   运行 Gradle 任务 `runIde`.
   ```bash
   ./gradlew runIde
   ```
   这将启动一个新的 GoLand (或 IDEA) 实例，其中已安装当前开发的插件。

2. **在沙盒中配置插件**:
   - 打开 `Settings -> Tools -> CodeI18n`
   - 设置 CLI 路径（如果 `codei18n` 不在 PATH 中）
   - 启用插件

3. **验证功能**:
   - 在沙盒 IDE 中打开测试项目
   - 打开 `test_data/sample.rs` 文件
   - 观察注释是否自动折叠并显示为中文 (e.g., `// 计算斐波那契数`)
   - 鼠标悬停验证显示英文原文

4. **测试编辑器集成**:
   - 修改 Rust 注释为新的英文文本
   - 等待 300ms debounce 后应自动刷新
   - 保存文件后运行 `codei18n translate` 更新映射

## 开发工作流

### 修改代码
```bash
# 修改 CommentTranslationFoldingBuilder.java
vim src/main/java/com/github/studyzy/codei18n/providers/CommentTranslationFoldingBuilder.java

# 只需修改文件类型检查方法：
# isGoFile() → isSupportedFile()
```

### 运行测试
```bash
# 运行所有测试
./gradlew test

# 运行特定测试
./gradlew test --tests "*.RustSupportIntegrationTest"

# 查看测试报告
open build/reports/tests/test/index.html
```

### 验证覆盖率
```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## 常见问题

### CLI 未找到
**症状**: 插件日志显示 "Failed to execute CLI command"

**解决**:
```bash
# 检查 CLI 是否在 PATH
which codei18n

# 或在插件设置中配置绝对路径
Settings -> Tools -> CodeI18n -> CLI Path: /usr/local/bin/codei18n
```

### 翻译未显示
**症状**: Rust 文件打开但无折叠显示

**检查清单**:
1. ✅ `.codei18n/config.json` 是否存在
2. ✅ CLI `scan` 命令是否返回翻译数据
3. ✅ 插件是否已启用
4. ✅ 文件扩展名是否为 `.rs`
5. ✅ 查看 `idea.log` 是否有错误

**调试**:
```bash
# 手动测试 CLI
cat test_data/sample.rs | codei18n scan \
  --file test_data/sample.rs \
  --stdin \
  --format json \
  --with-translations

# 查看插件日志
# Help -> Show Log in Finder -> idea.log
```

### JSON 解析失败
**症状**: 日志显示 "Failed to parse CLI response"

**原因**: CLI 输出格式变化或错误

**验证**:
```bash
# 检查 CLI 输出
codei18n scan --file test.rs --format json 2>&1 | tee output.json

# 验证 JSON 有效性
cat output.json | jq .
```

### Go 文件仍然工作正常
**验证**: 修改后不应影响现有 Go 支持

```bash
# 创建 Go 测试文件
cat > test_data/sample.go << 'EOF'
package main

// Calculate sum of two numbers
func add(a, b int) int {
    return a + b
}
EOF

# 在沙盒 IDE 中打开，验证 Go 翻译仍然正常
```

## 提交前检查

### 代码质量
```bash
# 运行所有检查
./gradlew check

# 格式化代码（如果项目有配置）
./gradlew spotlessApply

# 运行插件验证
./gradlew verifyPlugin
```

### 测试覆盖率
```bash
# 必须 ≥ 60%
./gradlew test jacocoTestReport
# 检查 build/reports/jacoco/test/html/index.html
```

### 集成测试
```bash
# 手动测试场景：
# 1. Go 文件支持仍然正常
# 2. Rust 文件支持正常工作
# 3. 混合项目（Go + Rust）同时支持
# 4. 性能：大文件 (1000+ 注释) 响应时间 < 5s
```

## 性能基准测试

### 创建大文件
```bash
# 生成包含 1000 个注释的 Rust 文件
cat > test_data/large.rs << 'EOF'
// This is comment 1
fn func1() {}
// This is comment 2
fn func2() {}
...
EOF

# 或使用脚本生成
for i in {1..1000}; do
  echo "// This is comment $i"
  echo "fn func$i() {}"
done > test_data/large.rs
```

### 测试性能
```bash
time codei18n scan --file test_data/large.rs --with-translations
# 目标: < 5 秒
```

## 下一步

完成开发后，参考 tasks.md 中的后续任务：
- 补充测试用例
- 更新文档
- 性能优化（如需要）
- 准备发布说明
