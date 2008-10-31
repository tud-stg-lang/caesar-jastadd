package org.caesarj.test;

import java.util.Collection;

import junit.framework.AssertionFailedError;

import org.caesarj.compiler.CaesarCompiler;
import org.caesarj.util.VerboseProgress;

public class CompileAndFail extends CompilerTest {
	private static final String ANY_ERROR = "UNDEF_MESSAGE";

	private String expectedError;
	
	public CompileAndFail(String pkgname, String expectedError) {
		super(pkgname);
		this.expectedError = expectedError;
	}

	public void runTest() throws Throwable {
		super.runTest();
		
		CaesarCompiler.initialize();
		
		if (compilerVerbose) {
			CaesarCompiler.setOption("-verbose");			
		}
		
		/* Compile */
		addSourceFiles(sources);
		typeCheck();
		
		CaesarCompiler.getErrors();
		
		assertFalse("Should not compile", CaesarCompiler.getErrors().isEmpty());
		verifyErrorMessage(expectedError, CaesarCompiler.getErrors());
				
		CaesarCompiler.cleanUp();
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
