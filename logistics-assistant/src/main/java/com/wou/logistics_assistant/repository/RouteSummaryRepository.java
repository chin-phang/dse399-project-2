package com.wou.logistics_assistant.repository;

import com.wou.logistics_assistant.entity.RouteSummary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteSummaryRepository extends JpaRepository<RouteSummary, Long> {

	Optional<RouteSummary> findByRouteRouteId(Long routeId);

	void deleteByRouteRouteId(Long routeId);
}
