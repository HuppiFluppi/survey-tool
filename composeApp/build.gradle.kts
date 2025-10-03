import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.kotlinMultiplatform)
//    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
//    androidTarget {
//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        compilerOptions {
//            jvmTarget.set(JvmTarget.JVM_17)
//        }
//    }

    jvm {
        mainRun {
            mainClass = "com.zinkel.survey.MainKt"
        }
    }

    sourceSets {
//        androidMain.dependencies {
//            implementation(compose.preview)
//            implementation(libs.androidx.activity.compose)
//        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.snakeyaml)
            implementation(libs.kotlin.csv)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.mockk)

            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

//android {
//    namespace = "com.zinkel.survey"
//    compileSdk = libs.versions.android.compileSdk.get().toInt()
//
//    defaultConfig {
//        applicationId = "com.zinkel.survey"
//        minSdk = libs.versions.android.minSdk.get().toInt()
//        targetSdk = libs.versions.android.targetSdk.get().toInt()
//        versionCode = 1
//        versionName = "1.0"
//    }
//    packaging {
//        resources {
//            excludes += "/META-INF/{AL2.0,LGPL2.1}"
//        }
//    }
//    buildTypes {
//        getByName("release") {
//            isMinifyEnabled = false
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//}
//
//dependencies {
//    debugImplementation(compose.uiTooling)
//}

tasks.register("fillAppInfoXml") {
    val version = project.version
    val appInfoFile = file("src/commonMain/composeResources/values/appinfo.xml")

    doLast {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd.HHmmss"))

        appInfoFile.writeText(
            """
            <resources>
                <string name="app_version">v$version</string>
                <string name="app_build">$timestamp</string>
            </resources>
        """.trimIndent()
        )
    }
}
tasks.getByName("generateResourceAccessorsForCommonMain").dependsOn("fillAppInfoXml")

compose.desktop {
    application {
        mainClass = "com.zinkel.survey.MainKt"

        buildTypes.release.proguard {
            // disable because of a problem with enum class valueOf and proguard (`-keepclassmembers enum * { *; }` needed)
            isEnabled = false
        }

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "survey-tool"
            packageVersion = "$version"
        }
    }
}
