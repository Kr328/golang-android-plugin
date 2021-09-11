enableFeaturePreview("VERSION_CATALOGS")

rootProject.name = "golang-android-plugin"

include("gradle-plugin")

dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            val agpVersion = "4.2.1"

            alias("android-gradle").to("com.android.tools.build:gradle:$agpVersion")
        }
    }
}