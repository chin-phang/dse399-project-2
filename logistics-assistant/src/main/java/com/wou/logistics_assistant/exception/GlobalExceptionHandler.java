package com.wou.logistics_assistant.exception;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Single place that turns exceptions into RFC 7807 {@code ProblemDetail} responses, so every error
 * the API emits has the same shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
	}

	@ExceptionHandler(ValidationException.class)
	public ProblemDetail handleValidation(ValidationException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Validation Failed", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
	}

	@ExceptionHandler(AiUnavailableException.class)
	public ProblemDetail handleAiUnavailable(AiUnavailableException ex) {
		log.warn("AI provider unavailable: {}", ex.getMessage());
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "AI Assistant Unavailable", ex.getMessage());
	}

	/** Bean Validation failures, flattened to {@code field: message} lines. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
		String details = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.reduce((left, right) -> left + "; " + right)
			.orElse("");
		return problem(HttpStatus.BAD_REQUEST, "Validation Failed", details.isBlank() ? "Invalid request" : details);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ProblemDetail handleAuthentication(AuthenticationException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid username or password");
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return problem(HttpStatus.FORBIDDEN, "Forbidden", "You do not have access to this resource");
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected error");
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
		problem.setTitle(title);
		problem.setType(URI.create("https://api.logistics.assistant/errors/" + status.value()));
		return problem;
	}
}
