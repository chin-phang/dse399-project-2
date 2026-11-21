package com.wou.logistics_assistant.exception;

/** A request is well-formed but violates a business rule. Maps to 400. */
public class ValidationException extends RuntimeException {

	public ValidationException(String message) {
		super(message);
	}
}
