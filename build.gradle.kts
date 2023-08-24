import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    `maven-publish`
    kotlin("jvm") version "1.9.10"
}

group = "me.theevilroot"
version = "1.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("junit:junit:4.13.2")
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        maven {
            val localProps = Properties()
            if (rootProject.file("local.properties").exists())
                localProps.load(rootProject.file("local.properties").reader())
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/TheEvilRoot/async-coroutines-socket")
            credentials {
                username = localProps.getOrDefault("gpr.user",
                    System.getenv("USERNAME")) as? String?
                password = localProps.getOrDefault("gpr.key",
                    System.getenv("TOKEN")) as? String?
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.theevilroot"
            artifactId = "coroutine-async-socket"
            version = version
            from(components["java"])
        }
        register<MavenPublication>("gpr") {
            groupId = "me.theevilroot"
            artifactId = "coroutine-async-socket"
            version = version
            from(components["java"])
            artifact(sourcesJar.get())
        }
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}