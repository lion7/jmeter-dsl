import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    id("org.ajoberstar.grgit") version "4.1.1"
}

group = "com.github.lion7"
version = grgit.describe()

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.jmeter:ApacheJMeter_http:5.6.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("ch.qos.logback:logback-classic:1.4.14")
}

configurations {
    implementation {
        exclude("org.apache.jmeter", "bom")
    }
    testImplementation {
        exclude("org.apache.logging.log4j", "log4j-slf4j-impl")
    }
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}

tasks {
    wrapper {
        gradleVersion = "8.6"
        distributionType = DistributionType.ALL
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-java-parameters", "-Xjsr305=strict", "-Werror")
        }
    }

    test {
        useJUnitPlatform()
        jvmArgs("-Djava.security.egd=file:/dev/urandom")

        testLogging {
            events(*TestLogEvent.values())
        }
    }

    register<Jar>("javadocJar") {
        dependsOn(dokkaJavadoc)
        archiveClassifier.set("javadoc")
        from(dokkaJavadoc.get().outputDirectory)
    }
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            artifact(tasks["javadocJar"])
        }
    }
}
