import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * :data — implements the :domain contracts. Ktor (remote), Room KMP (local
 * cache), offline-first repository, Koin wiring for this layer.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// ---------------------------------------------------------------------------
// GoRest API token: read from local.properties (gitignored) or GOREST_TOKEN
// env var, then generated into source. The token never enters git.
// ---------------------------------------------------------------------------
val gorestToken: String = run {
    val props = Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use(props::load)
    props.getProperty("gorest.token")
        ?: System.getenv("GOREST_TOKEN")
        ?: ""
}

val generateApiConfig = tasks.register("generateApiConfig") {
    val outDir = layout.buildDirectory.dir("generated/apiConfig/kotlin")
    inputs.property("token", gorestToken)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get()
            .file("com/sliide/challenge/users/data/remote/ApiConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |// Generated at build time from local.properties — do not edit, do not commit.
            |package com.sliide.challenge.users.data.remote
            |
            |internal object ApiConfig {
            |    const val BASE_URL: String = "https://gorest.co.in/public/v2"
            |    const val API_TOKEN: String = "$gorestToken"
            |}
            """.trimMargin()
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateApiConfig)
            dependencies {
                api(project(":domain"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
            }
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.android) // androidContext() in platformDataModule
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
        }
    }
}

android {
    namespace = "com.sliide.challenge.users.data"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
