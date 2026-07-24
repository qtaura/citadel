description = "Citadel Internal Implementation"

dependencies {
    implementation(project(":citadel-api"))
    implementation(libs.snakeyaml)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
