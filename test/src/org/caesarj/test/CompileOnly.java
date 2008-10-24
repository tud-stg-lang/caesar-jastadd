package org.caesarj.test;

import org.caesarj.compiler.CaesarCompiler;

public class CompileOnly extends CompilerTest {
	
	public CompileOnly(String pkgname) {
		super(pkgname);
	}

	protected void runTest() throws Throwable {
		super.runTest();
		
		CaesarCompiler.initialize();
		
		if (compilerVerbose) {
			CaesarCompiler.setOption("-verbose");
		}
		
		addSourceFiles(sources);
		typeCheck();
				
		/* Check if compilation succeeded */
		assertTrue(CaesarCompiler.getErrors().toString(), CaesarCompiler.getErrors().isEmpty());
		
		generateClassfiles();
				
		CaesarCompiler.cleanUp();
	}
}
