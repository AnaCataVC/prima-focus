plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.gson)
    
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
