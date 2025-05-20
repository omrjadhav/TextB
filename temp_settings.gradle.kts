pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // CometChat SDK repository
        maven {
            url = uri("https://dl.cloudsmith.io/public/cometchat/cometchat-android/maven/")
        }
        // JitPack repository
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "TextBook"
include(":app")
