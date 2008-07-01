package org.caesarj.test;

public class CompileOnly extends CompilerTest {
	
	public CompileOnly(String pkgname) {
		super(pkgname);
	}

	protected void runTest() throws Throwable {
		super.runTest();
		errorCheck();
	}
}
