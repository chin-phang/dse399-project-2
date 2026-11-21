package com.wou.logistics_assistant.service;

/**
 * UC08 — export a route report.
 *
 * Both methods take the route id: the endpoint is {@code GET /api/routes/{id}/export/csv}.
 */
public interface ExportService {

	byte[] exportCSV(Long routeId);

	byte[] exportReport(Long routeId);
}
