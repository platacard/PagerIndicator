import org.jreleaser.model.Active
import java.nio.file.Files

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("org.jreleaser") version "1.15.0"
    `maven-publish`
    signing
}

android {
    namespace = "dif.tech.pagerindicator"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
}

if (!release) {
    version = "$version-SNAPSHOT"
}

publishing {
    publications {
        withType<MavenPublication> {
            groupId = "mx.platacard"
            artifactId = "compose-pager-indicator"

            pom {
                name.set("Compose Pager Indicator")
                description.set("A simple pager indicator that displays dots for each page in the pager")
                url.set("https://github.com/platacard/PagerIndicator")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("Pavel Shnyakin")
                        email.set("pavel.shnyakin@dif.tech")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/platacard/PagerIndicator.git")
                    developerConnection.set("scm:git:git://github.com/platacard/PagerIndicator.git")
                    url.set("https://github.com/platacard/PagerIndicator")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = stringProperty("mavenCentralUsername")
                password = stringProperty("mavenCentralPassword")
            }
        }
    }
}

signing {
    val keyFile = project.findProperty("signing.secretKeyFile") as String?
    val keyId = project.findProperty("signing.keyId") as String?
    val password = project.findProperty("signing.password") as String?


    if (keyFile != null && keyId != null && password != null) {
        val secretKey = Files.readString(file(keyFile).toPath())
        useInMemoryPgpKeys(keyId, secretKey, password)
        sign(publishing.publications)
    }
}

val release: Boolean get() = hasProperty("release")

fun stringProperty(propertyName: String) = findProperty(propertyName) as String?
