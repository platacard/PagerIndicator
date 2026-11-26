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
        classpath(libs.commons.codec)
    }
    configurations.all {
        resolutionStrategy {
            force(libs.commons.compress)
            force(libs.commons.codec)
        }
    }
}
