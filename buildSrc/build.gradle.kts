plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

apply(from = "../repositories.gradle.kts")

dependencies {
    // Gradle Plugins
    implementation("com.android.tools.build:gradle:8.8.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    // Apache Commons Codec for MD5 checksum
    implementation("commons-codec:commons-codec:1.16.0")
}
