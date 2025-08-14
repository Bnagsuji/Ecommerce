package kr.hhplus.be.server;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
class TestcontainersConfiguration {

	public static final MySQLContainer<?> MYSQL_CONTAINER;
	public static final GenericContainer<?> REDIS_CONTAINER;

	static {
		MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
			.withDatabaseName("hhplus")
			.withUsername("root")
			.withPassword("1234");
		MYSQL_CONTAINER.start();

		System.setProperty("spring.datasource.url", MYSQL_CONTAINER.getJdbcUrl() + "?characterEncoding=UTF-8&serverTimezone=UTC");
		System.setProperty("spring.datasource.username", MYSQL_CONTAINER.getUsername());
		System.setProperty("spring.datasource.password", MYSQL_CONTAINER.getPassword());

		// Redis 컨테이너 설정
		REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:alpine3.22"))
				.withExposedPorts(6379);
		REDIS_CONTAINER.start();

		// Spring Boot redis 설정
		System.setProperty("spring.data.redis.host", REDIS_CONTAINER.getHost());
		System.setProperty("spring.data.redis.port", REDIS_CONTAINER.getMappedPort(6379).toString());

	}
//TODO
//	@javax.annotation.PreDestroy
//	public void stop() {
//		if (REDIS.isRunning()) REDIS.stop();
//	}

	@PreDestroy
	public void preDestroy() {
		if (MYSQL_CONTAINER.isRunning()) {
			MYSQL_CONTAINER.stop();
		}
		if(REDIS_CONTAINER.isRunning()) {
			REDIS_CONTAINER.stop();
		}
	}
}