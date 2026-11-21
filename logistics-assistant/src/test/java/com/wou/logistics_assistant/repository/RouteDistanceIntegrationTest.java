package com.wou.logistics_assistant.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.wou.logistics_assistant.RequiresDocker;
import com.wou.logistics_assistant.TestcontainersConfiguration;
import com.wou.logistics_assistant.entity.Job;
import com.wou.logistics_assistant.entity.Route;
import com.wou.logistics_assistant.entity.Vehicle;
import com.wou.logistics_assistant.entity.VehicleStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * The parts of the data layer that only a real PostGIS database can prove: the Liquibase schema
 * matches the JPA mappings, the generated {@code location} column is computed from
 * latitude/longitude, and {@code ST_Distance} works against a {@code geography} column.
 *
 * Skipped when no Docker daemon is reachable.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@RequiresDocker
@Transactional
@DirtiesContext
class RouteDistanceIntegrationTest {

	@Autowired
	private RouteRepository routeRepository;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private EntityManager entityManager;

	private Route persistRouteWith(Job... jobs) {
		Route route = routeRepository.save(new Route("Test route"));
		for (int index = 0; index < jobs.length; index++) {
			Job job = jobs[index];
			job.assignToRoute(route);
			job.setSequenceOrder(index);
			jobRepository.save(job);
		}
		entityManager.flush();
		entityManager.clear();
		return route;
	}

	private Job job(String name, Double lat, Double lon) {
		return new Job(name, name, lat, lon);
	}

	@Test
	void computesDistanceBetweenConsecutiveStopsInRouteOrder() {
		Route route = persistRouteWith(job("KL", 3.1590, 101.7123), job("Shah Alam", 3.0738, 101.5183));

		double metres = routeRepository.calculateTotalDistanceMetres(route.getRouteId());

		assertThat(metres).isCloseTo(23_532.0, within(50.0));
	}

	@Test
	void skipsStopsWithoutCoordinatesRatherThanFailing() {
		Route route = persistRouteWith(
			job("KL", 3.1590, 101.7123),
			job("Not geocoded yet", null, null),
			job("Shah Alam", 3.0738, 101.5183)
		);

		double metres = routeRepository.calculateTotalDistanceMetres(route.getRouteId());

		assertThat(metres).isCloseTo(23_532.0, within(50.0));
	}

	@Test
	void aRouteWithNoLocatedStopsReportsZeroNotNull() {
		Route route = persistRouteWith(job("Nowhere", null, null));

		assertThat(routeRepository.calculateTotalDistanceMetres(route.getRouteId())).isZero();
	}

	@Test
	void anEmptyRouteReportsZero() {
		Route route = routeRepository.save(new Route("Empty"));
		entityManager.flush();

		assertThat(routeRepository.calculateTotalDistanceMetres(route.getRouteId())).isZero();
	}

	@Test
	void theGeneratedLocationColumnIsPopulatedFromLatitudeAndLongitude() {
		Job saved = jobRepository.save(job("KL", 3.1590, 101.7123));
		entityManager.flush();
		entityManager.clear();

		Job reloaded = jobRepository.findById(saved.getJobId()).orElseThrow();

		assertThat(reloaded.getLocation()).isNotNull();
		assertThat(reloaded.getLocation().getX()).isCloseTo(101.7123, within(0.0001));
		assertThat(reloaded.getLocation().getY()).isCloseTo(3.1590, within(0.0001));
	}

	@Test
	void aJobWithNoCoordinatesHasANullLocation() {
		Job saved = jobRepository.save(job("Not geocoded", null, null));
		entityManager.flush();
		entityManager.clear();

		assertThat(jobRepository.findById(saved.getJobId()).orElseThrow().getLocation()).isNull();
	}

	@Test
	void jobsAndRoutesPersistUnassigned() {
		Job orphan = jobRepository.save(job("Unassigned", 3.0, 101.0));
		Route routeWithoutVehicle = routeRepository.save(new Route("No vehicle yet"));
		entityManager.flush();
		entityManager.clear();

		assertThat(jobRepository.findById(orphan.getJobId()).orElseThrow().getRoute()).isNull();
		assertThat(routeRepository.findById(routeWithoutVehicle.getRouteId()).orElseThrow().getVehicle()).isNull();
		assertThat(jobRepository.findByRouteIsNull().stream().map(Job::getJobId).toList()).contains(orphan.getJobId());
	}

	@Test
	void derivedQueriesResolveAgainstTheEntityProperties() {
		vehicleRepository.save(new Vehicle("TST 001", "Van", 900.0));
		entityManager.flush();

		assertThat(vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)).isNotEmpty();
		assertThat(vehicleRepository.existsByLicensePlate("TST 001")).isTrue();
	}
}
