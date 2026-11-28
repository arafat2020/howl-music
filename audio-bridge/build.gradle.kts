import javax.inject.Inject

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.howl.music.bridge"
    compileSdk = 35
    ndkVersion = "28.2.13676358"

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
                // Oboe Prefab + our .so need the shared STL.
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        prefab = true
    }

    externalNativeBuild {
        cmake {
            // Owns the single shared lib: libhowl_engine.so
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Prefab package — stable Oboe binaries (see native/audio-android/README.md).
    implementation(libs.oboe)
}

abstract class GenerateThirdPartyAssets : DefaultTask() {
    @get:InputFile
    abstract val index: RegularFileProperty

    @get:InputDirectory
    abstract val licenses: DirectoryProperty

    @get:InputFile
    abstract val ndkNotice: RegularFileProperty

    @get:InputFile
    abstract val toolchainNotice: RegularFileProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun generate() {
        fileSystem.sync {
            into(output.get().asFile.resolve("third-party"))
            from(index)
            from(licenses)
            from(ndkNotice) { rename { "Android-NDK-NOTICE.txt" } }
            from(toolchainNotice) { rename { "Android-NDK-NOTICE.toolchain.txt" } }
        }
    }
}

val generateThirdPartyAssets by tasks.registering(GenerateThirdPartyAssets::class) {
    val root = rootProject.layout.projectDirectory
    val version = checkNotNull(android.ndkVersion)
    val ndk = androidComponents.sdkComponents.sdkDirectory.map { it.dir("ndk/$version") }
    index.set(root.file("THIRD_PARTY_NOTICES.txt"))
    licenses.set(root.dir("licenses"))
    ndkNotice.set(ndk.map { it.file("NOTICE") })
    toolchainNotice.set(ndk.map { it.file("NOTICE.toolchain") })
    output.set(layout.buildDirectory.dir("generated/assets/thirdParty"))
}

androidComponents.onVariants {
    it.sources.assets?.addGeneratedSourceDirectory(generateThirdPartyAssets, GenerateThirdPartyAssets::output)
}
