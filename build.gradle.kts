import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
}

group = "com.github.lion7"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.jmeter:ApacheJMeter_http:5.4.1")
    implementation("org.junit.jupiter:junit-jupiter-api:5.7.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("ch.qos.logback:logback-classic:1.2.6")
}

configurations {
    implementation {
        exclude("org.apache.jmeter", "bom")
    }
    testImplementation {
        exclude("org.apache.logging.log4j", "log4j-slf4j-impl")
    }
}

tasks {
    wrapper {
        gradleVersion = "7.2"
        distributionType = DistributionType.ALL
    }

    withType<JavaCompile> {
        options.compilerArgs = listOf("-parameters", "-Werror")
        options.encoding = "UTF-8"

        sourceCompatibility = "15"
        targetCompatibility = "15"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-java-parameters", "-Xjsr305=strict", "-Werror")
            jvmTarget = "15"
        }
    }

    val copyJMeterResources = register<Sync>("copyJMeterResources") {
        from("src/main/jmeter")
        into("build/jmeter/bin")
    }

    test {
        dependsOn(copyJMeterResources)

        useJUnitPlatform()
        jvmArgs("-Djava.security.egd=file:/dev/urandom")

        testLogging {
            events(*TestLogEvent.values())
        }
    }
}
