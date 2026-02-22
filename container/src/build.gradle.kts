plugins {
    kotlin("jvm") version "1.9.20"
    application
}

group = "de.codetreats"
version = "0.0.1"

repositories {
    mavenCentral()
    project.findProperty("snapshot.repo.url")?.let { snapshotUrls ->
        snapshotUrls.toString().split(",").forEach { snapshotUrl ->
            maven {
                url = uri(snapshotUrl)
                isAllowInsecureProtocol = true
            }
        }
    }
    
}

application {
    mainClass.set("de.codetreats.autocommit.MainKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

dependencies {
    val ktor = "3.0.1"
    val moshi = "1.15.1"
    val log4j = "2.24.1"
    val jackson = "2.17.1"
    val kotlin = "1.9.20"
    val restClient = "2.0.0"
    val bouncyCastle = "1.82"
    implementation("net.codetreats:kotlin-rest-client:$restClient")
    implementation("org.json:json:20240303")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jackson")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson")
    implementation("io.ktor:ktor-http:$ktor")
    implementation("io.ktor:ktor-http-jvm:$ktor")
    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-core-jvm:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktor")
    implementation("io.ktor:ktor-server-html-builder:$ktor")
    implementation("io.ktor:ktor-network-tls-certificates:$ktor")
    implementation("org.bouncycastle:bcprov-jdk18on:$bouncyCastle")
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncyCastle")
    implementation("com.squareup.moshi:moshi:$moshi")
    implementation("com.squareup.moshi:moshi-kotlin:$moshi")
    implementation("com.squareup.moshi:moshi-adapters:$moshi")
    implementation("org.apache.logging.log4j:log4j-api:$log4j")
    implementation("org.apache.logging.log4j:log4j-core:$log4j")
    implementation("org.slf4j:slf4j-nop:2.0.16")
    implementation("org.jsoup:jsoup:1.19.1")
    implementation("com.openai:openai-java:4.8.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.4.0.202509020913-r")
    annotationProcessor("com.squareup.moshi:moshi-kotlin-codegen:$moshi")
}

dependencies {
    val junit = "5.11.3"
    val mockk = "1.13.13"
    testImplementation("io.mockk:mockk:$mockk")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.named<JavaExec>("run") {
    if (project.hasProperty("extendedlog")) {
        val value = project.property("extendedlog") as String
        jvmArgs("-Dextendedlog=$value")
    }
}