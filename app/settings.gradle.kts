pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StemTutor"

include(":app")
include(":core:designsystem", ":core:common", ":core:network", ":core:data")
include(":feature:onboarding", ":feature:student", ":feature:teacher", ":feature:parent", ":feature:notes")
