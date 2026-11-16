pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info/")
        flatDir {
            dirs("app/src/main/lib")
        }
    }
}

rootProject.name = "OpenIPTV"
include(":app")