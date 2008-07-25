package org.caesarj.test;

import java.util.Collection;
import java.util.LinkedList;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

public class CompileAndFail extends CompilerTest {
	private static final String ANY_ERROR = "UNDEF_MESSAGE";

	private String expectedError;
	private boolean binariesHaveErrors = false;

	public CompileAndFail(String pkgname, String expectedError) {
		super(pkgname);
		this.expectedError = expectedError;
	}

	public void runTest() throws Throwable {
		super.runTest();
		
		Collection<String> errors = new LinkedList<String>();
		prog.errorCheck(errors);
		if (!binariesHaveErrors) {
			assertFalse("Should not compile", errors.isEmpty());
			verifyErrorMessage(expectedError, errors);
		}
	}
	
	public void verifyErrorMessage(String expected, Collection<String> actual) {
		if (expected.length() == 0 || expected.equals(ANY_ERROR))
			return;

		for (String msg : actual) {
			if (ErrorMessages.instance().similarError(expected, msg))
				return;
		}

		throw new AssertionFailedError("Expected " + expected + ", but got\n"
				+ actual);
	}
}
