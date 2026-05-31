import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    implementation(libs.ktor.client.java)

    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.storage)
}

compose.desktop {
    application {
        mainClass = "com.daytimegaming.ourdailyfinances.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.daytimegaming.ourdailyfinances"
            packageVersion = "1.0.0"
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