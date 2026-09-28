pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "HumeAndroid"
include(":app")
include(":core:ui")
include(":core:model")
include(":core:data")
include(":core:network")
include(":core:datastore")
include(":feature:home")
include(":feature:energy")
include(":feature:security")
include(":feature:me")
include(":feature:auth")
