description = "Citadel Internal Implementation"

plugins {
    application
}

application {
    mainClass = "io.citadel.core.bootstrap.Citadel"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.citadel.core.bootstrap.Citadel"
    }
}

dependencies {
    implementation(project(":citadel-api"))
    implementation(libs.snakeyaml)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.gson)
    implementation(libs.minecraftauth)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
