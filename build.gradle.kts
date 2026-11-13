plugins { java }

group = "dev.emerald"
version = "0.7.11-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.218-stable")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

/**
 * Always rebuild the client pack from the tracked resourcepack/ files.
 * Never depend on a stale committed binary ZIP.
 */
val packageResourcePack by tasks.registering(Zip::class) {
    from(layout.projectDirectory.dir("resourcepack"))
    archiveFileName.set("funnel-pack.zip")
    destinationDirectory.set(layout.buildDirectory.dir("generated/funnel-pack"))
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

sourceSets {
    main {
        resources.exclude("funnel-pack.zip")
    }
}

tasks.processResources {
    from(packageResourcePack)
    dependsOn(packageResourcePack)
}
