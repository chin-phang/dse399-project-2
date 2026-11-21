package com.wou.logistics_assistant.repository;

import com.wou.logistics_assistant.entity.Vehicle;
import com.wou.logistics_assistant.entity.VehicleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

	/**
	 * Backs the {@code listAvailableVehicles} tool. Named for the entity's {@code status} property.
	 */
	List<Vehicle> findByStatus(VehicleStatus status);

	boolean existsByLicensePlate(String licensePlate);

	Optional<Vehicle> findByLicensePlate(String licensePlate);
}
