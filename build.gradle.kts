// Root build.gradle.kts
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.22" apply false
}

// Repositories are defined in settings.gradle.kts via dependencyResolutionManagement
// Do NOT add allprojects { repositories } here - it conflicts with RepositoriesMode.FAIL_ON_PROJECT_REPOS
