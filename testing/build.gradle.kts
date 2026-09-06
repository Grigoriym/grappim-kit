import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.publish)
}

kotlin {
    jvmToolchain(21)

    jvm()
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.grappim.kit.testing"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    sourceSets {
        commonMain.dependencies {
            // `configureTests()`-style consumers put this module on every other module's
            // `commonTest`, so `runTest` reaches them through here rather than being declared
            // per module.
            api(libs.kotlinx.coroutines.test)

            // `FakeCrashReporter`/`FakeAppInfoProvider` implement these -- consumers resolve
            // the interfaces through here, same shape the source apps' own `:testing` used.
            api(project(":crash"))
            api(project(":appinfo"))
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
