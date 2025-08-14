plugins {
	java
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
}

fun getGitHash(): String {
	return providers.exec {
		commandLine("git", "rev-parse", "--short", "HEAD")
	}.standardOutput.asText.get().trim()
}

group = "kr.hhplus.be"
version = getGitHash()

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}


val querydslDir = "src/main/generated"

sourceSets {
	main {
		java.srcDirs("src/main/java", querydslDir)
	}
}




repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
	}
}

var tc="1.20.4";

dependencies {
    // Spring
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
// https://mvnrepository.com/artifact/org.springframework.data/spring-data-redis
	implementation("org.springframework.data:spring-data-redis:3.4.0")
// https://mvnrepository.com/artifact/org.redisson/redisson-spring-boot-starter
	implementation("org.redisson:redisson-spring-boot-starter:3.41.0")

	//래디스 캐시
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	// https://mvnrepository.com/artifact/org.mapstruct/mapstruct-processor
	implementation("org.mapstruct:mapstruct-processor:1.4.2.Final")
	implementation ("org.mapstruct:mapstruct:1.4.2.Final")

	//swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
	//lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
    // DB
	runtimeOnly("com.mysql:mysql-connector-j")



	implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
	annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")
	annotationProcessor("jakarta.annotation:jakarta.annotation-api")
	annotationProcessor("jakarta.persistence:jakarta.persistence-api:3.1.0")


    // Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
// https://mvnrepository.com/artifact/org.testcontainers/testcontainers
	testImplementation("org.testcontainers:testcontainers:1.19.8")
	testImplementation("org.testcontainers:junit-jupiter:$tc")
	testImplementation("org.testcontainers:mysql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("user.timezone", "UTC")
}

tasks.withType<JavaCompile>().configureEach {
	options.generatedSourceOutputDirectory.set(file("/generated/java"))
}

