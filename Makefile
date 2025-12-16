.PHONY: build test run-ide clean help

help:
	@echo "Available commands:"
	@echo "  build    - Build the plugin"
	@echo "  test     - Run tests"
	@echo "  run-ide  - Run the plugin in a sandboxed IDE"
	@echo "  clean    - Clean build artifacts"

build:
	JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home /opt/homebrew/opt/gradle@8/bin/gradle buildPlugin -x buildSearchableOptions

test:
	JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home /opt/homebrew/opt/gradle@8/bin/gradle test

run-ide:
	JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home /opt/homebrew/opt/gradle@8/bin/gradle runIde

clean:
	JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home /opt/homebrew/opt/gradle@8/bin/gradle clean
