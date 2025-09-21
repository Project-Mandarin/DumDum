import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.AbstractAppExtension
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.File
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.util.Base64
import java.util.Properties
import kotlin.system.exitProcess
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

private val Project.android get() = extensions.getByName<ApplicationExtension>("android")

private lateinit var metadata: Properties
private lateinit var localProperties: Properties
private lateinit var flavor: String

fun Project.downloadLibcore() {
    afterEvaluate {
        tasks.names.forEach { taskName ->
            if (taskName.contains("compile")) {
                tasks.named(taskName) { dependsOn("downloadLibcore") }
            }
        }
    }

    fun verifyChecksum(file: File): Boolean {
        val currentMD5 = file.inputStream().use { DigestUtils.md5Hex(it) }

        if (currentMD5 != requireMetadata().getProperty("LIBCORE_MD5")) {
            throw org.gradle.api.GradleException("Failed to verify libcore.aar MD5 checksum!")
        } else {
            return true
        }
    }

    tasks.create("downloadLibcore") {
        doLast {
            val libcoreRepo = requireMetadata().getProperty("LIBCORE_REPO")
            val libcoreVersion = requireMetadata().getProperty("LIBCORE_VERSION")
            val libcoreUrl = "$libcoreRepo/releases/download/$libcoreVersion/libcore-$libcoreVersion.aar"
            val libcoreFile = File(projectDir, "libs/libcore.aar").apply {
                parentFile.mkdirs()
            }

            if (libcoreFile.exists()) {
                if (verifyChecksum(libcoreFile)) {
                    logger.lifecycle("${libcoreFile.absolutePath} already exists")
                    return@doLast
                }
            }

            try {
                val client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build()
                val response = client.send(
                    HttpRequest.newBuilder().uri(URI.create(libcoreUrl)).build(),
                    HttpResponse.BodyHandlers.ofInputStream()
                )

                if (response.statusCode() == 200) {
                    response.body().use { input ->
                        libcoreFile.outputStream().use { input.copyTo(it) }
                    }
                    verifyChecksum(libcoreFile)
                } else {
                    throw org.gradle.api.GradleException("Failed to download $libcoreUrl (${response.statusCode()})")
                }
            } catch (e: Exception) {
                throw org.gradle.api.GradleException("Failed to download libcore: ${e.message}", e)
            }
        }
    }
}

fun Project.requireFlavor(): String {
    if (::flavor.isInitialized) return flavor
    if (gradle.startParameter.taskNames.isNotEmpty()) {
        val taskName = gradle.startParameter.taskNames[0]
        when {
            taskName.contains("assemble") -> {
                flavor = taskName.substringAfter("assemble")
                return flavor
            }

            taskName.contains("install") -> {
                flavor = taskName.substringAfter("install")
                return flavor
            }

            taskName.contains("bundle") -> {
                flavor = taskName.substringAfter("bundle")
                return flavor
            }
        }
    }

    flavor = ""
    return flavor
}

fun Project.requireMetadata(): Properties {
    if (!::metadata.isInitialized) {
        metadata = Properties().apply {
            load(rootProject.file("dumdum.properties").inputStream())
        }
    }
    return metadata
}

fun Project.requireLocalProperties(): Properties {
    if (!::localProperties.isInitialized) {
        localProperties = Properties()

        val base64 = System.getenv("LOCAL_PROPERTIES")
        if (!base64.isNullOrBlank()) {
            localProperties.load(Base64.getDecoder().decode(base64).inputStream())
        } else if (project.rootProject.file("local.properties").exists()) {
            localProperties.load(rootProject.file("local.properties").inputStream())
        }
    }
    return localProperties
}

fun Project.setupCommon() {
    android.apply {
        buildToolsVersion = "35.0.1"
        compileSdk = 35
        defaultConfig {
            minSdk = 21
            targetSdk = 35
        }
        buildTypes {
            getByName("release") {
                isMinifyEnabled = true
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        (android as ExtensionAware).extensions.getByName<KotlinJvmOptions>("kotlinOptions").apply {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
        lint {
            showAll = true
            checkAllWarnings = true
            checkReleaseBuilds = true
            warningsAsErrors = true
            textOutput = project.file("build/lint.txt")
            htmlOutput = project.file("build/lint.html")
        }
        packaging {
            resources.excludes.addAll(
                listOf(
                    "**/*.kotlin_*",
                    "/META-INF/*.version",
                    "/META-INF/native/**",
                    "/META-INF/native-image/**",
                    "/META-INF/INDEX.LIST",
                    "DebugProbesKt.bin",
                    "com/**",
                    "org/**",
                    "**/*.java",
                    "**/*.proto",
                    "okhttp3/**"
                )
            )
        }
        (this as? AbstractAppExtension)?.apply {
            buildTypes {
                getByName("release") {
                    isShrinkResources = true
                    if (System.getenv("nkmr_minify") == "0") {
                        isShrinkResources = false
                        isMinifyEnabled = false
                    }
                }
                getByName("debug") {
                    applicationIdSuffix = "debug"
                    debuggable(true)
                    jniDebuggable(true)
                }
            }
            applicationVariants.forEach { variant ->
                variant.outputs.forEach {
                    it as BaseVariantOutputImpl
                    it.outputFileName = it.outputFileName.replace(
                        "app", "${project.name}-" + variant.versionName
                    ).replace("-release", "").replace("-github", "")
                }
            }
        }
    }
}

fun Project.setupAppCommon() {
    setupCommon()

    val lp = requireLocalProperties()
    val keystorePwd = lp.getProperty("KEYSTORE_PASS") ?: System.getenv("KEYSTORE_PASS")
    val alias = lp.getProperty("ALIAS_NAME") ?: System.getenv("ALIAS_NAME")
    val pwd = lp.getProperty("ALIAS_PASS") ?: System.getenv("ALIAS_PASS")

    android.apply {
        if (keystorePwd != null) {
            signingConfigs {
                create("release") {
                    storeFile = rootProject.file("dumdum.jks")
                    storePassword = keystorePwd
                    keyAlias = alias
                    keyPassword = pwd
                }
            }
        } else if (requireFlavor().contains("(GitHub|Expert|Play)Release".toRegex())) {
            exitProcess(0)
        }
        dependenciesInfo {
            includeInApk = false
            includeInBundle = false
        }
        buildTypes {
            val key = signingConfigs.findByName("release")
            if (key != null) {
                getByName("release").signingConfig = key
                getByName("debug").signingConfig = key
            }
        }
    }
}

fun Project.getGitCommit(): String {
    val stdout = java.io.ByteArrayOutputStream()

    project.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }

    return stdout.toString().trim()
}

fun Project.setupApp() {
    val pkgName = requireMetadata().getProperty("PACKAGE_NAME")
    val verName = requireMetadata().getProperty("VERSION_NAME")
    val verCode = requireMetadata().getProperty("VERSION_CODE").toInt()
    val gitCommit = getGitCommit()

    android.apply {
        defaultConfig {
            applicationId = pkgName
            versionCode = verCode
            versionName = verName
            buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
        }
    }
    downloadLibcore()
    setupAppCommon()

    android.apply {
        this as AbstractAppExtension

        buildTypes {
            getByName("release") {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro")
                )
            }
        }

        splits.abi {
            reset()
            isUniversalApk = true
        }

        flavorDimensions += "vendor"
        productFlavors {
            create("github")
            create("fdroid")
            create("play")
        }

        applicationVariants.all {
            outputs.all {
                this as BaseVariantOutputImpl
                outputFileName = outputFileName.replace(project.name, "DumDum-$versionName")
                    .replace("-release", "")
                    .replace("-github", "")
            }
        }

        for (abi in listOf("Arm64", "Arm", "X64", "X86")) {
            tasks.create("assemble" + abi + "FdroidRelease") {
                dependsOn("assembleFdroidRelease")
            }
        }

        sourceSets.getByName("main").apply {
            jniLibs.srcDir("executableSo")
        }
    }
}
