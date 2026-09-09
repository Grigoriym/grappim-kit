import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.publish)
}

// Android-only, deliberately no jvm()/iOS targets: Activity and the Play In-App Update APIs
// this interface models don't exist off Android, and neither source app's version of this
// interface was ever KMP-common -- both shipped an Android-only androidApp/di type.
kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.grappim.kit.appupdate"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
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
