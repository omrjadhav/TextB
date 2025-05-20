pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://dl.cloudsmith.io/public/cometchat/cometchat-pro-android/maven/") }
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://dl.cloudsmith.io/public/cometchat/cometchat-pro-android/maven/") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "TextB"
include(":app")
 