package org.caesarj.runtime;

public class RuntimeInconsistencyError extends RuntimeException {

	private static final long serialVersionUID = -1592356319679228898L;
	protected String message;
	
	public RuntimeInconsistencyError(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}

}
