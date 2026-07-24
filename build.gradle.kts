plugins {
    alias(libs.plugins.spotless)
}

group = "io.citadel"
version = "0.1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    plugins.apply("java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Test> {
        useJUnitPlatform {
            excludeTags("integration")
        }
        minHeapSize = "128m"
        maxHeapSize = "512m"
    }

    tasks.register<Test>("integrationTest") {
        useJUnitPlatform {
            includeTags("integration")
        }
        minHeapSize = "128m"
        maxHeapSize = "512m"
        shouldRunAfter(tasks.named("test"))
    }

    plugins.apply("pmd")

    configure<PmdExtension> {
        toolVersion = "7.0.0"
        isConsoleOutput = true
        rulesMinimumPriority = 5
        ruleSetFiles = files(rootProject.file("config/pmd/ruleset.xml"))
    }
}

spotless {
    java {
        target("**/src/**/*.java")
        targetExclude("**/build/**")
        googleJavaFormat()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
