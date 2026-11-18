import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.hkemtrentainment.cinthiasquestr"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hkemtrentainment.cinthiasquestr"
        minSdk = 24
        targetSdk = 37
        versionCode = 4
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    buildTypes {
        release {
            ndk {
                debugSymbolLevel = "FULL"
            }
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

// Tarea 100% compatible con Gradle 9 Configuration Cache para exportar mapping.txt y native-debug-symbols.zip
@DisableCachingByDefault(because = "Empaqueta y copia artefactos de depuración de release")
abstract class ExportPlayStoreArtifactsTask : DefaultTask() {

    @get:Internal
    abstract val buildDirProperty: DirectoryProperty

    @get:Internal
    abstract val projectDirProperty: DirectoryProperty

    @TaskAction
    fun exportArtifacts() {
        val buildDirFile = buildDirProperty.get().asFile
        val projectDirFile = projectDirProperty.get().asFile
        val releaseOutDir = File(projectDirFile, "release").apply { mkdirs() }

        // 1. Localizar mapping.txt (Archivo de asignación ReTrace) y copiarlo a app/release/mapping.txt
        val mappingOutDir = File(buildDirFile, "outputs/mapping/release").apply { mkdirs() }
        val standardMappingFile = File(mappingOutDir, "mapping.txt")
        val foundMapping = if (standardMappingFile.exists() && standardMappingFile.length() > 0L) {
            standardMappingFile
        } else {
            buildDirFile.walkTopDown().firstOrNull {
                it.isFile && (it.name == "mapping.txt" || it.name == "proguard.map") &&
                    it.length() > 0L && it.path.contains("release", ignoreCase = true)
            }
        }

        if (foundMapping != null) {
            if (foundMapping.canonicalPath != standardMappingFile.canonicalPath) {
                foundMapping.copyTo(standardMappingFile, overwrite = true)
            }
            foundMapping.copyTo(File(releaseOutDir, "mapping.txt"), overwrite = true)
        }

        // 2. Generar native-debug-symbols.zip a partir de las librerías .so del build y copiarlo a app/release/
        val symbolsOutDir = File(buildDirFile, "outputs/native-debug-symbols/release").apply { mkdirs() }
        val symbolsZip = File(symbolsOutDir, "native-debug-symbols.zip")

        if (!symbolsZip.exists() || symbolsZip.length() == 0L) {
            val intermediatesDir = File(buildDirFile, "intermediates")
            val abiDirs = setOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            val soFilesByAbiAndName = linkedMapOf<String, File>()

            if (intermediatesDir.exists()) {
                intermediatesDir.walkTopDown()
                    .filter { it.isFile && (it.name.endsWith(".so") || it.name.endsWith(".dbg")) }
                    .forEach { file ->
                        val abi = file.parentFile?.name
                        if (abi in abiDirs) {
                            val key = "$abi/${file.name}"
                            if (!soFilesByAbiAndName.containsKey(key) || file.absolutePath.contains("merged_native_libs")) {
                                soFilesByAbiAndName[key] = file
                            }
                        }
                    }
            }

            if (soFilesByAbiAndName.isNotEmpty()) {
                ZipOutputStream(FileOutputStream(symbolsZip)).use { zos ->
                    soFilesByAbiAndName.forEach { (entryPath, file) ->
                        zos.putNextEntry(ZipEntry(entryPath))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }

        if (symbolsZip.exists() && symbolsZip.length() > 0L) {
            symbolsZip.copyTo(File(releaseOutDir, "native-debug-symbols.zip"), overwrite = true)
        }
    }
}

val exportPlayStoreArtifacts = tasks.register<ExportPlayStoreArtifactsTask>("exportPlayStoreArtifacts") {
    buildDirProperty.set(layout.buildDirectory)
    projectDirProperty.set(layout.projectDirectory)
}

tasks.matching {
    it.name in setOf(
        "bundleRelease",
        "signReleaseBundle",
        "packageReleaseBundle",
        "assembleRelease",
        "minifyReleaseWithR8"
    )
}.configureEach {
    finalizedBy(exportPlayStoreArtifacts)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.adaptive)
    implementation(libs.androidx.compose.adaptive.layout)
    implementation(libs.androidx.compose.adaptive.navigation3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.material)

    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
