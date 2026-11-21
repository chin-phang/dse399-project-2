package com.wou.logistics_assistant.exception;

/** A conflicting state change, e.g. a duplicate licence plate. Maps to 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
