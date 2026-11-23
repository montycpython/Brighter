plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
  androidLibrary {
    namespace = "com.supernatural.bwriter.shared"
    compileSdk = 36
    minSdk = 24

    compilerOptions {
      jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
  }

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs {
    browser()
  }

  sourceSets {
    commonMain.dependencies {
      // The extracted core uses only the Kotlin standard library.
    }
    wasmJsMain.dependencies {
      implementation(libs.kotlinx.datetime)
    }
  }
}
