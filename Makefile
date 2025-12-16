.PHONY: build test run-ide clean help

help:
	@echo "Available commands:"
	@echo "  build    - Build the plugin"
	@echo "  test     - Run tests"
	@echo "  run-ide  - Run the plugin in a sandboxed IDE"
	@echo "  clean    - Clean build artifacts"

build:
	./gradlew buildPlugin -x buildSearchableOptions

test:
	./gradlew test

run-ide:
	./gradlew runIde

clean:
	./gradlew clean
