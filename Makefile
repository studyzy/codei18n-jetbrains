.PHONY: build test run-goland run-rustrover run-intellij build-local clean help

help:
	@echo "Available commands:"
	@echo "  build          - Build the plugin (downloads IDE if needed)"
	@echo "  build-local    - Build using local GoLand (faster but may have issues)"
	@echo "  test           - Run tests"
	@echo "  run-goland     - Run the plugin in GoLand IDE"
	@echo "  run-rustrover  - Run the plugin in RustRover IDE"
	@echo "  run-intellij   - Run the plugin in IntelliJ IDEA Community IDE"
	@echo "  release        - Build the plugin package for offline installation"
	@echo "  clean          - Clean build artifacts"

build:
	./gradlew buildPlugin -x buildSearchableOptions

build-local:
	./gradlew buildPlugin -x buildSearchableOptions -Pintellij.localPath=/Applications/GoLand.app

test:
	./gradlew test

run-goland:
	./gradlew runIde -Pintellij.type=GO

run-rustrover:
	./gradlew runIde -Pintellij.type=RR

run-intellij:
	./gradlew runIde -Pintellij.type=IC

release:
	@echo "Building plugin package for offline installation..."
	./gradlew buildPlugin
	@echo "Plugin package built successfully!"
	@echo "You can find the plugin package at: build/distributions/"
	@ls -lh build/distributions/*.zip 2>/dev/null || true

clean:
	./gradlew clean
