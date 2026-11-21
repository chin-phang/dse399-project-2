package com.wou.logistics_assistant.repository;

import com.wou.logistics_assistant.entity.Route;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

	List<Route> findByVehicleVehicleId(Long vehicleId);

	/**
	 * Total travel distance along a route's stops, in metres.
	 *
	 * Uses {@code ST_Distance} on the {@code geography} column. {@code ST_DistanceSphere} is defined
	 * over {@code geometry} only and fails with
	 * {@code function st_distancesphere(geography, geography) does not exist}. The geography
	 * overload returns metres on the spheroid.
	 *
	 * Stops with no coordinates are filtered out before the pairing rather than failing the query,
	 * so a route that mixes geocoded and not-yet-geocoded jobs still reports the distance across
	 * the stops it does know about.
	 */
	@Query(
		value = """
			WITH ordered AS (
			    SELECT location,
			           ROW_NUMBER() OVER (ORDER BY sequence_order NULLS LAST, job_id) AS rn
			    FROM job
			    WHERE route_id = :routeId
			      AND location IS NOT NULL
			)
			SELECT COALESCE(SUM(ST_Distance(a.location, b.location)), 0)
			FROM ordered a
			JOIN ordered b ON b.rn = a.rn + 1
			""",
		nativeQuery = true
	)
	double calculateTotalDistanceMetres(@Param("routeId") Long routeId);
}
