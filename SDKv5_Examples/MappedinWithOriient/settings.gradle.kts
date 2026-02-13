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

        maven {
            url = uri("https://oriient.jfrog.io/artifactory/libs-release-local")
            credentials {
                username = ""
                password = ""
            }
        }
    }
}

rootProject.name = "Mappedin With Oriient"
include(":app")
 