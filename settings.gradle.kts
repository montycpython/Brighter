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
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
  repositories {
    google()
    mavenCentral()
    // Declare the Node.js & Yarn download repositories
    exclusiveContent {
      forRepository {
        ivy("https://nodejs.org/dist/") {
          name = "Node Distributions at $url"
          patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
          metadataSources { artifact() }
          content { includeModule("org.nodejs", "node") }
        }
      }
      filter { includeGroup("org.nodejs") }
    }
    exclusiveContent {
      forRepository {
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
          name = "Yarn Distributions at $url"
          patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
          metadataSources { artifact() }
          content { includeModule("com.yarnpkg", "yarn") }
        }
      }
      filter { includeGroup("com.yarnpkg") }
    }
    exclusiveContent {
      forRepository {
        ivy("https://github.com/WebAssembly/binaryen/releases/download") {
          name = "Binaryen Distributions at $url"
          patternLayout { artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]") }
          metadataSources { artifact() }
          content { includeModule("com.github.webassembly", "binaryen") }
        }
      }
      filter { includeGroup("com.github.webassembly") }
    }
  }
}

rootProject.name = "Bwriter"

include(":app")
include(":shared")
include(":webApp")
