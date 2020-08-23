import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
//    id("com.android.application")
    id( "com.android.library")
    id("org.jetbrains.kotlin.multiplatform")

}
android {
    compileSdkVersion(30)
    defaultConfig {
        minSdkVersion(28)
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

//dependencies {
//    // Specify Kotlin/JVM stdlib dependency.
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
//
//    testImplementation("junit:junit:4.12")
//    testImplementation("org.jetbrains.kotlin:kotlin-test")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
//
//    androidTestImplementation("junit:junit:4.12")
//    androidTestImplementation("org.jetbrains.kotlin:kotlin-test")
//    androidTestImplementation("org.jetbrains.kotlin:kotlin-test-junit")
//}
    kotlin {
        //select iOS target platform depending on the Xcode environment variables
        val iOSTarget: (String, KotlinNativeTarget.() -> Unit) -> KotlinNativeTarget =
            if (System.getenv("SDK_NAME")?.startsWith("iphoneos") == true)
                ::iosArm64
            else
                ::iosX64

        iOSTarget("ios") {
            binaries {
                framework {
                    baseName = "SharedCode"
                }
            }
        }

        android()

        sourceSets["commonMain"].dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
        }

        sourceSets["androidMain"].dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib")
        }
    }

    val packForXcode by tasks.creating(Sync::class) {
        val targetDir = File(buildDir, "xcode-frameworks")

        /// selecting the right configuration for the iOS
        /// framework depending on the environment
        /// variables set by Xcode build
        val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
        val framework = kotlin.targets
            .getByName<KotlinNativeTarget>("ios")
            .binaries.getFramework(mode)
        inputs.property("mode", mode)
        dependsOn(framework.linkTask)

        from({ framework.outputDirectory })
        into(targetDir)

        /// generate a helpful ./gradlew wrapper with embedded Java path
        doLast {
            val gradlew = File(targetDir, "gradlew")
            gradlew.writeText("#!/bin/bash\n"
                    + "export 'JAVA_HOME=${System.getProperty("java.home")}'\n"
                    + "cd '${rootProject.rootDir}'\n"
                    + "./gradlew \$@\n")
            gradlew.setExecutable(true)
        }
    }

    tasks.getByName("build").dependsOn(packForXcode)
