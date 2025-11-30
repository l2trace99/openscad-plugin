import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.grammarkit.tasks.GenerateLexerTask

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "org.openscad"
version = "0.1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://jogamp.org/deployment/maven/") }
}

dependencies {
    // JOGL for 3D rendering (Java OpenGL bindings)
    val joglVersion = "2.3.2"
    implementation("org.jogamp.gluegen:gluegen-rt:$joglVersion")
    implementation("org.jogamp.jogl:jogl-all:$joglVersion")
    
    // Platform-specific natives for JOGL
    val os = org.gradle.internal.os.OperatingSystem.current()
    val arch = System.getProperty("os.arch")
    val classifier = when {
        os.isMacOsX && arch == "aarch64" -> "natives-macosx-universal"
        os.isMacOsX -> "natives-macosx-universal"
        os.isWindows && arch.contains("64") -> "natives-windows-amd64"
        os.isWindows -> "natives-windows-i586"
        os.isLinux && arch.contains("64") -> "natives-linux-amd64"
        os.isLinux -> "natives-linux-i586"
        else -> "natives-linux-amd64"
    }
    
    implementation("org.jogamp.gluegen:gluegen-rt:$joglVersion:$classifier")
    implementation("org.jogamp.jogl:jogl-all:$joglVersion:$classifier")
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2023.2.5")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf())
}

// Configure source sets to include generated code
sourceSets {
    main {
        java.srcDirs("src/main/gen")
    }
}

// Configure Grammar-Kit for lexer generation
tasks.register<GenerateLexerTask>("generateOpenSCADLexer") {
    sourceFile.set(file("src/main/grammars/OpenSCADLexer.flex"))
    targetOutputDir.set(file("src/main/gen/org/openscad/lexer"))
    purgeOldFiles.set(true)
}

// Make compilation depend on lexer generation
tasks.named("compileKotlin") {
    dependsOn("generateOpenSCADLexer")
}
tasks.named("compileJava") {
    dependsOn("generateOpenSCADLexer")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("241.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
