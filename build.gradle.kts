plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.github.studyzy"
version = "0.1.0"

// Configure Java toolchain to use Java 21
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read properties
val ideType = project.findProperty("intellij.type")?.toString() ?: "GO"
val localIdePath = project.findProperty("intellij.localPath")?.toString()

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
    
    // IntelliJ Platform dependencies
    intellijPlatform {
        // Configure IDE based on type and local path
        when {
            localIdePath != null -> {
                local(localIdePath)
            }
            ideType == "GO" -> {
                goland("2024.3")
            }
            ideType == "RR" -> {
                rustRover("2024.3")
            }
            ideType == "IC" -> {
                intellijIdeaCommunity("2024.3")
            }
            else -> {
                create("IC", "2024.3")
            }
        }
        
        // Configure bundled plugins based on IDE type - only when not using local path
        if (localIdePath == null) {
            when (ideType) {
                "GO" -> {
                    bundledPlugin("org.jetbrains.plugins.go")
                    bundledPlugin("JavaScript")
                }
                "RR" -> {
                    bundledPlugin("com.jetbrains.rust")
                    bundledPlugin("JavaScript")
                }
                "IC" -> {
                    bundledPlugin("com.intellij.java")
                    // Note: JavaScript plugin is not available in IntelliJ IDEA Community
                }
                else -> {}
            }
        }
        
        pluginVerifier()
        
        // Use JetBrains Runtime
        jetbrainsRuntime()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            untilBuild = "253.*"
        }
    }
    
    pluginVerification {
        ides {
            recommended()
        }
        freeArgs = listOf("-mute", "TemplateWordInPluginId")
    }
    
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    // Set the JVM compatibility versions - use 21 for latest IDE compatibility
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    test {
        useJUnitPlatform()
    }
}