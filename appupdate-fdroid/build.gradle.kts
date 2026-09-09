import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.publish)
}

// Android-only, same reasoning as :appupdate. No-op implementation for an F-Droid/non-Play
// build variant, which can't pull in Google Play Core; use :appupdate-gplay for a Google
// Play distribution build variant instead.
kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.grappim.kit.appupdate.fdroid"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":appupdate"))
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
