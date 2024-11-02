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

// https://github.com/GoogleContainerTools/jib/issues/4235
buildscript {
    dependencies {
        classpath("commons-codec:commons-codec:1.16.1")
    }
    configurations.all {
        resolutionStrategy {
            force("org.apache.commons:commons-compress:1.26.0")
            force("commons-codec:commons-codec:1.16.1")
        }
    }
}

android {
    namespace = "mx.platacard.pagerindicator"
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

android {
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

version = properties["version"].toString()

publishing {
    publications {
        create<MavenPublication>("release") {
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
                        email.set("pavel.shnyakin@mx.platacard")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/platacard/PagerIndicator.git")
                    developerConnection.set("scm:git:git://github.com/platacard/PagerIndicator.git")
                    url.set("https://github.com/platacard/PagerIndicator")
                    connection.set("scm:git://github.com/platacard/PagerIndicator.git")
                    developerConnection.set("scm:git://github.com/platacard/PagerIndicator.git")
                }
            }

            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            setUrl(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    project {
        inceptionYear = "2024"
        author("@shpasha")
    }
    gitRootSearch = true
    signing {
        active = Active.ALWAYS
        armored = true
        verify = true
    }
    release {
        github {
            skipTag = true
            sign = true
            branch = "main"
            branchPush = "main"
            overwrite = true
        }
    }
    deploy {
        maven {
            mavenCentral.create("sonatype") {
                active = Active.ALWAYS
                url = "https://central.sonatype.com/api/v1/publisher"
                stagingRepository(layout.buildDirectory.dir("staging-deploy").get().toString())
                setAuthorization("Basic")
                applyMavenCentralRules =
                    false // Wait for fix: https://github.com/kordamp/pomchecker/issues/21
                sign = true
                checksums = true
                sourceJar = true
                javadocJar = true
                retryDelay = 60
            }
        }
    }
}