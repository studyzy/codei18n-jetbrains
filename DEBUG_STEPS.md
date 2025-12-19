# 调试步骤

## 1. 验证 CLI 工具工作正常
```bash
cd /Users/devinzeng/go/src/github.com/studyzy/codei18n-jetbrains
codei18n scan --file src/main/java/com/github/studyzy/codei18n/models/CliResponse.java --format json --with-translations --stdin < src/main/java/com/github/studyzy/codei18n/models/CliResponse.java
```
✅ CLI 工具工作正常，返回了翻译数据

## 2. 检查插件设置
在 RustRover 中：
1. 打开 Settings/Preferences → Tools → CodeI18n
2. 确认：
   - ✅ Enabled: 勾选
   - ✅ Display Mode: FOLDING（折叠模式）
   - ✅ CLI Path: codei18n（或完整路径）

## 3. 查看日志
在 RustRover 中：
1. Help → Show Log in Finder/Explorer
2. 打开 idea.log
3. 搜索 "CommentTranslationFoldingBuilder" 或 "TranslationService"
4. 查看是否有错误信息

## 4. 重启 IDE
修改代码后需要：
1. 停止当前运行的插件实例
2. 重新运行 `make run-rustrover`
3. 打开 Java 文件

## 5. 手动触发折叠
在编辑器中：
1. 右键点击行号区域
2. 选择 "Folding" → "Expand All" 或 "Collapse All"
3. 或者使用快捷键：Cmd+Shift+Plus/Minus (Mac) 或 Ctrl+Shift+Plus/Minus (Windows)

## 6. 预期行为
- 注释应该默认显示为中文（折叠状态）
- 点击注释旁边的 + 号可以展开查看英文原文
- 鼠标悬停在注释上应该显示工具提示
