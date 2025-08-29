plugins {
    kotlin("jvm") version "1.9.25" // 추가
    kotlin("plugin.spring") version "1.9.25" // 추가
    kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "1.9.25"
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
}

extra["springAiVersion"] = "1.0.0"

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql:42.7.3")
    implementation("io.github.openfeign.querydsl:querydsl-jpa:7.0")
    kapt("io.github.openfeign.querydsl:querydsl-apt:7.0:jpa")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("com.h2database:h2:2.3.232")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.openkoreantext:open-korean-text:2.3.1")

    // 구버전 swagger-annotations 제거
    configurations.all {
        exclude(group = "io.swagger.core.v3", module = "swagger-annotations")
    }

    // Jakarta 버전 명시적 추가
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.30")

    implementation("org.jsoup:jsoup:1.21.1")
    implementation("org.springframework.ai:spring-ai-starter-model-openai:1.0.0")
    implementation("com.bucket4j:bucket4j_jdk17-core:8.14.0")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.mysql:mysql-connector-j")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kapt {
    arguments {
        arg("querydsl.entityAccessors", "true")
        arg("querydsl.kotlin.entityAccessors", "true")
    }
}

sourceSets {
    main {
        java {
            srcDir("$buildDir/generated/source/kapt/main")
        }
    }
}