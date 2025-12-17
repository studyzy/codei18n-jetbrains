# Changelog

All notable changes to the codei18n-jetbrains plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Rust language support** for comment translation 🆕
  - Support for all Rust comment types: `//`, `///`, `//!`, `/* */`, `/** */`
  - Seamless integration with existing codei18n CLI (using tree-sitter parser)
  - Same user experience as Go file support
- Comprehensive manual testing checklist (`MANUAL_TEST_CHECKLIST.md`)
- Testing strategy documentation (`TEST_STRATEGY_RUST.md`)

### Changed
- Renamed `isGoFile()` method to `isSupportedFile()` for better extensibility
- Updated file type detection to support `.go` and `.rs` files

### Fixed
- Added null safety checks for plugin settings and file names
- Improved error handling in folding builder

## [0.1.0] - 2024-XX-XX

### Added
- Initial release
- Go language support for comment translation
- Multiple display modes:
  - Folding display
  - Inlay hints
  - Tooltips
  - Gutter icons
- Integration with codei18n CLI tool
- Support for GoLand, IntelliJ IDEA, and RustRover (2023.3+)

[Unreleased]: https://github.com/studyzy/codei18n-jetbrains/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/studyzy/codei18n-jetbrains/releases/tag/v0.1.0
