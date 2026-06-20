plugins {
    kotlin("jvm") version "2.3.21"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0"
    application
}

ktlint {
    version.set("1.7.1")
    verbose.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

group = "com.github.alextesta00"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("it.unibo.tuprolog:solve-classic-jvm:1.1.5")
    implementation("it.unibo.tuprolog:parser-theory-jvm:1.1.5")
    implementation("io.github.jason-lang:jason:3.1.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

sourceSets {
    main {
        resources {
            srcDir("src/main/prolog")
            srcDir("src/main/agents")
        }
    }
}

application {
    mainClass.set("cli.AeroGuardCliKt")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<JavaExec>("runJasonSmoke") {
    group = "verification"
    description = "Runs a lightweight smoke check over the Jason AgentSpeak sources."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("integration.JasonSmokeKt")
    args("src/main/agents")
}
