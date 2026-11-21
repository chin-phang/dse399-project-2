package com.wou.logistics_assistant.exception;

/** The AI provider is unreachable or refused the request. Drives the UC01/UC02 alt flow. */
public class AiUnavailableException extends RuntimeException {

	public AiUnavailableException(String message) {
		super(message);
	}

	public AiUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
