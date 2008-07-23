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
		prepareBinaries();
		parseSource();
		
		Collection<String> errors = new LinkedList<String>();
		prog.errorCheck(errors);
		if (!binariesHaveErrors) {
			assertFalse("Should not compile", errors.isEmpty());
			verifyErrorMessage(expectedError, errors);
		}
	}
	
	protected void errorCheck() {
		if (printCode) System.out.println("Compiled code : \n"+prog.toString());
		if (testcaseVerbose) System.out.println("Checking for errors ... ");
		Collection<String> errors = new LinkedList<String>();
		prog.errorCheck(errors);
		assertFalse("Should not compile", errors.isEmpty());
		verifyErrorMessage(expectedError, errors);
		if (!errors.isEmpty()) binariesHaveErrors = true;	// set flag, so the test is considered to have failed
		if (testcaseVerbose) System.out.println("Error check done.");
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
