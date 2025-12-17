.PHONY: build test run-ide clean help

help:
	@echo "Available commands:"
	@echo "  build    - Build the plugin"
	@echo "  test     - Run tests"
	@echo "  run-ide  - Run the plugin in a sandboxed IDE"
	@echo "  release  - Build the plugin package for offline installation"
	@echo "  clean    - Clean build artifacts"

build:
	./gradlew buildPlugin -x buildSearchableOptions

test:
	./gradlew test

run-ide:
	./gradlew runIde

release:
	@echo "Building plugin package for offline installation..."
	./gradlew buildPlugin
	@echo "Plugin package built successfully!"
	@echo "You can find the plugin package at: build/distributions/"
	@ls -lh build/distributions/*.zip 2>/dev/null || true

clean:
	./gradlew clean
