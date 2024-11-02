// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
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
