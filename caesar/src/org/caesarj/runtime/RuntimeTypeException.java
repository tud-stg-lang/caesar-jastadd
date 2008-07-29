package org.caesarj.runtime;

public class RuntimeTypeException extends Exception {

	private static final long serialVersionUID = 9122413646119624540L;
	protected String message;
	
	public RuntimeTypeException(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
}
