description = "Citadel Dashboard Backend"

dependencies {
    implementation(project(":citadel-api"))

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
