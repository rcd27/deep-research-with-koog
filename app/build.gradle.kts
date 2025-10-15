plugins {
  kotlin("jvm")
  id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
  id("com.github.johnrengelman.shadow") version "8.1.1"
  application
}

application {
  mainClass.set("MainKt")
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

  /** Koog */
  implementation("ai.koog:koog-agents:0.5.0")

  /** OpenTelemetry */
  implementation(platform("io.opentelemetry:opentelemetry-bom:1.53.0"))
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")
  implementation("io.opentelemetry:opentelemetry-exporter-logging")

  /** Logging */
  // TODO: use io.github.oshai.kotlinlogging.KotlinLogging
  implementation("ch.qos.logback:logback-classic:1.5.13")
  implementation("org.slf4j:slf4j-api:2.0.9")

  /** Network */
  implementation("io.ktor:ktor-client-cio:2.3.4")
  implementation("io.ktor:ktor-client-core:2.3.4")
  implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
  implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

  /** Environment Variables */
  implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

  /** Tests */
  testImplementation(kotlin("test"))
  testImplementation("ai.koog:agents-test:0.4.2")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

kotlin {
  jvmToolchain(17)

  // https://kotlinlang.org/docs/context-parameters.html#how-to-enable-context-parameters
  compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
  }
}

tasks.test {
  useJUnitPlatform()
}

tasks.jar {
  manifest {
    attributes["Main-Class"] = "MainKt"
  }
  configurations["compileClasspath"].forEach { file: File ->
    from(zipTree(file.absoluteFile))
  }
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks {
  shadowJar {
    archiveBaseName.set("deep-research-with-koog")
    archiveClassifier.set("")
    archiveVersion.set("")

    // Мержим service файлы для OpenTelemetry
    mergeServiceFiles()

    manifest {
      attributes["Main-Class"] = "MainKt"
    }
  }

  build {
    dependsOn(shadowJar)
  }
}
