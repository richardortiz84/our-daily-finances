import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    androidLibrary {
        namespace = "com.daytimegaming.ourdailyfinances.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.ktor.client.android)
            implementation(project.dependencies.platform(libs.android.firebase.bom))
        }
        commonMain.dependencies {
            api(libs.datastore)
            api(libs.datastore.preferences)
            api(libs.koin.core)
            api(libs.kotlinx.datetime)

            implementation(libs.adaptive)
            implementation(libs.adaptive.layout)
            implementation(libs.adaptive.navigation)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.okio)

            // Navigation 3
            implementation(libs.navigation3.ui)

            // Ktor networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Firebase
            implementation(libs.firebase.auth)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
        }
    }
}

val env = Properties().apply {
    val envFile = file("${rootDir}/.env")
    if (envFile.exists()) {
        envFile.inputStream().use { stream ->
            this.load(stream)
        }
    }
}

fun getEnv(key: String, defaultValue: String): String {
    return System.getenv(key) ?: env.getProperty(key) ?: defaultValue
}

buildkonfig {
    packageName = "com.daytimegaming.ourdailyfinances"
    defaultConfigs {
        buildConfigField(STRING, "API_URL", getEnv("API_URL", ""))
        buildConfigField(STRING, "STAGING_API_URL", getEnv("STAGING_API_URL", ""))
        buildConfigField(STRING, "STAGING_API_URL", getEnv("STAGING_API_URL", ""))
        buildConfigField(STRING, "FIREBASE_PROJECT_ID", getEnv("FIREBASE_PROJECT_ID", ""))
        buildConfigField(STRING, "FIREBASE_APPLICATION_ID", getEnv("FIREBASE_APPLICATION_ID", ""))
        buildConfigField(STRING, "FIREBASE_API_KEY", getEnv("FIREBASE_API_KEY", ""))
        buildConfigField(STRING, "FIREBASE_AUTH_DOMAIN", getEnv("FIREBASE_AUTH_DOMAIN", ""))
        buildConfigField(STRING, "FIREBASE_STORAGE_BUCKET", getEnv("FIREBASE_STORAGE_BUCKET", ""))
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
