package com.wou.logistics_assistant.exception;

/** A requested aggregate does not exist. Maps to 404. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resource, Object id) {
		super(resource + " not found: " + id);
	}
}
