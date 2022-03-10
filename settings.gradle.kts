@file:Suppress("UnstableApiUsage")

rootProject.name = "golang-android-plugin"

include("gradle-plugin")

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            val agp = "7.1.0"
            val lombok = "6.4.1"

            library("android-gradle", "com.android.tools.build:gradle:$agp")
            plugin("lombok", "io.freefair.lombok").version(lombok)
        }
    }
}
