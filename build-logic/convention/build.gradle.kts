import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.grappim.kit.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.compose.multiplatform.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "com.grappim.kit.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpLibraryCompose") {
            id = "com.grappim.kit.kmp.library.compose"
            implementationClass = "KmpLibraryComposeConventionPlugin"
        }
        register("kmpLibraryStability") {
            id = "com.grappim.kit.kmp.library.stability"
            implementationClass = "KmpLibraryStabilityConventionPlugin"
        }
        register("kmpSerialization") {
            id = "com.grappim.kit.kmp.serialization"
            implementationClass = "KmpSerializationConventionPlugin"
        }
        register("kmpDi") {
            id = "com.grappim.kit.kmp.di"
            implementationClass = "KmpDiConventionPlugin"
        }
        register("kmpNetwork") {
            id = "com.grappim.kit.kmp.network"
            implementationClass = "KmpNetworkConventionPlugin"
        }
    }
}
