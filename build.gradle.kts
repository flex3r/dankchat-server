plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.quarkus)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

group = "com.flxrs.dankchat"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val legacyImport: SourceSet = sourceSets.create("legacyImport")

dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.rest)
    implementation(libs.quarkus.rest.kotlin.serialization)
    implementation(libs.quarkus.rest.client)
    implementation(libs.quarkus.rest.client.kotlin.serialization)
    implementation(libs.quarkus.hibernate.orm.panache.kotlin)
    implementation(libs.quarkus.jdbc.postgresql)
    implementation(libs.quarkus.cache)
    implementation(libs.quarkus.flyway)
    implementation(libs.quarkus.flyway.postgresql)
    implementation(libs.quarkus.scheduler)
    implementation(libs.quarkus.smallrye.health)
    implementation(libs.quarkus.micrometer.registry.prometheus)
    implementation(libs.quarkus.container.image.jib)
    implementation(libs.quarkus.oidc)
    implementation(libs.quarkus.rest.client.oidc.filter)
    implementation(libs.quarkus.quinoa)

    testImplementation(libs.quarkus.junit)
    testImplementation(libs.quarkus.junit.mockito)
    testImplementation(libs.quarkus.test.security.oidc)
    testImplementation(libs.rest.assured)
    testImplementation(libs.kotlin.test)
    testImplementation(legacyImport.output)
    testImplementation(libs.h2)

    "legacyImportImplementation"(libs.h2)
    "legacyImportImplementation"(libs.kotlinx.serialization.json)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        javaParameters = true
        allWarningsAsErrors = true
    }
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.register<JavaExec>("legacyExport") {
    description = "Converts a copy of the legacy dankchat-api data directory into SQL for the new schema."
    classpath = legacyImport.runtimeClasspath
    mainClass.set("com.flxrs.dankchat.server.legacy.LegacyExportKt")
    args(
        providers.gradleProperty("legacyDir").orNull ?: "",
        providers.gradleProperty("legacyOut").orNull ?: "",
        providers.gradleProperty("legacyImages").orNull ?: "",
    )
}

tasks.test {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_filename" to "disabled",
                    "ktlint_standard_multiline-expression-wrapping" to "disabled",
                    "ktlint_standard_when-entry-bracing" to "disabled",
                    "ktlint_function_signature_body_expression_wrapping" to "default",
                ),
            )
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$projectDir/config/detekt.yml")
    parallel = true
}
