description = "Citadel Internal Implementation"

dependencies {
    implementation(project(":citadel-api"))
    implementation(libs.snakeyaml)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
