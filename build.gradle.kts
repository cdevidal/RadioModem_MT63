plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
}
tasks.register("cleanAll") {
    doLast {
        delete(rootProject.buildDir)
        delete(file("app/build"))
    }
}

