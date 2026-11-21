package com.wou.logistics_assistant;

import org.testcontainers.DockerClientFactory;

/**
 * Integration tests need a real PostGIS container. Rather than fail the whole build on a machine
 * with no Docker daemon, they are skipped and run in full wherever Docker is up.
 */
public final class DockerAvailability {

	private DockerAvailability() {
	}

	public static boolean isAvailable() {
		try {
			return DockerClientFactory.instance().isDockerAvailable();
		} catch (Throwable ex) {
			return false;
		}
	}
}
