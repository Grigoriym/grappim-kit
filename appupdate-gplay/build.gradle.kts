import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.publish)
}

// Android-only, same reasoning as :appupdate. Pair with a Google Play distribution build
// variant; use :appupdate-fdroid for an F-Droid/non-Play build variant instead.
kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.grappim.kit.appupdate.gplay"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":appupdate"))
            implementation(libs.google.inapp.update.ktx)
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
