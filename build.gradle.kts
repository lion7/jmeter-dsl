import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.dokka") version "1.5.31"
    id("org.ajoberstar.grgit") version "4.1.0"
}

group = "com.github.lion7"
version = grgit.describe()

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.jmeter:ApacheJMeter_http:5.4.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("ch.qos.logback:logback-classic:1.2.10")
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

tasks {
    wrapper {
        gradleVersion = "7.2"
        distributionType = DistributionType.ALL
    }

    withType<JavaCompile> {
        options.compilerArgs = listOf("-parameters", "-Werror")
        options.encoding = "UTF-8"

        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-java-parameters", "-Xjsr305=strict", "-Werror")
            jvmTarget = "1.8"
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
