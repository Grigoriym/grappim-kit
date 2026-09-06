import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.publish)
}

// jvm() + androidLibrary only, deliberately no iOS targets: X509ExtendedTrustManager/javax.net.ssl
// don't exist there, and neither source app's own version of this class was ever KMP-common —
// each shipped an identical androidMain/jvmMain copy, which this module mirrors.
kotlin {
    jvmToolchain(21)

    jvm()

    androidLibrary {
        namespace = "com.grappim.kit.trustmanager"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation(project(":storage"))
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":testing"))
        }
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            androidVariantsToPublish = listOf("release")
        )
    )
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}
