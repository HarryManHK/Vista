pluginManagement {
    repositories {
        // This is where Gradle looks for plugins (like the Android Gradle plugin)
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // jcenter() if needed
    }
}

rootProject.name = "VISTA"
include(":app")