// Root build.gradle.kts
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}

// Repositories are defined in settings.gradle.kts via dependencyResolutionManagement
// Do NOT add allprojects { repositories } here - it conflicts with RepositoriesMode.FAIL_ON_PROJECT_REPOS
