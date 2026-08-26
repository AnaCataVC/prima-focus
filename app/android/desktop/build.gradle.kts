plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.ancata.prima_focus.desktop.MainKt")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.sqlite.jdbc)
    implementation(libs.gson)
    
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
