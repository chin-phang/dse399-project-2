package com.wou.logistics_assistant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests run against postgis/postgis rather than plain postgres: the schema declares
 * {@code CREATE EXTENSION postgis} and a generated {@code geography(Point,4326)} column, neither
 * of which exists on a stock postgres image.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(
			DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres")
		);
	}
}
