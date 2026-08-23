pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.chaquo.com") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.chaquo.com") }
    }
}

rootProject.name = "PY-HOST"
include(":app")
