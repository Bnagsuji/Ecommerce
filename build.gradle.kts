plugins {
	java
	// Spring Boot 버전을 최신 안정 버전으로 변경
	id("org.springframework.boot") version "3.3.1"
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
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.2")
	}
}

var tc="1.19.8"; // testcontainers 버전을 최신 안정 버전으로 변경

dependencies {
	// Spring
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	// Spring Boot가 버전을 관리하도록 명시적 버전을 제거
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	// Spring Boot 3.3.2와 호환되는 Redisson 버전을 명시 (이 예시에서는 최신 안정 버전을 사용)
	implementation("org.redisson:redisson-spring-boot-starter:3.28.0")
	//래디스 캐시
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	// https://mvnrepository.com/artifact/org.mapstruct/mapstruct-processor
	implementation("org.mapstruct:mapstruct-processor:1.4.2.Final")
	implementation ("org.mapstruct:mapstruct:1.4.2.Final")
	// JSON 직렬화용
	implementation("com.fasterxml.jackson.core:jackson-databind")
	//카프카
	// https://mvnrepository.com/artifact/org.springframework.kafka/spring-kafka
	implementation("org.springframework.kafka:spring-kafka:3.3.4")
	// https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients
	implementation("org.apache.kafka:kafka-clients:3.9.1")
	//swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
	//lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	// DB
	runtimeOnly("com.mysql:mysql-connector-j")
	//grafana(prometheus)
	runtimeOnly ("io.micrometer:micrometer-registry-prometheus")


	implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
	annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")
	annotationProcessor("jakarta.annotation:jakarta.annotation-api")
	annotationProcessor("jakarta.persistence:jakarta.persistence-api:3.1.0")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers:1.19.8")
	testImplementation("org.testcontainers:junit-jupiter:$tc")
	testImplementation("org.testcontainers:mysql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation ("org.testcontainers:kafka:1.19.8")
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("user.timezone", "UTC")
}

tasks.withType<JavaCompile>().configureEach {
	options.generatedSourceOutputDirectory.set(file("/generated/java"))
}