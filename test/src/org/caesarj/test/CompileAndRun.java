package org.caesarj.test;

import java.io.File;
import java.lang.reflect.Method;

import junit.framework.AssertionFailedError;

import org.caesarj.runtime.mixer.MixinLoader;

public class CompileAndRun extends CompileOnly {
	final MixinLoader loader = new MixinLoader();		

	public CompileAndRun(String pkgname) {
		super(pkgname);
		// Do not reuse class files
		// *** DO reuse class files! (?) or at least don't delete .java files created from binary block
		 final String outdir = binDir() + File.separator + pkgname.replace(".", File.separator);
		 clean(new File(outdir));
		loader.setTrace(TestProperties.instance().getTraceLoader());
	}

	public void addMain(String code) {
		code = "public class Test extends junit.framework.TestCase {\n"
				+ "\tpublic static StringBuffer res = new StringBuffer();\n"
				+ "\tpublic Test() {super(\"test\");}\n"
				+ "\tpublic void runTest() {\n" + "\t\t" + code + "\n\t}\n"
				+ "}\n";
		addSource("Test.java", code);
	}

	public void runTest() throws Throwable {
		
		final String classname = pkgname + ".Test";
		if (testcaseVerbose) System.out.println("Test class name = "+classname);

		try {
			// Already loaded?
			Class.forName(classname);
			throw new AssertionError(classname
					+ " already exists. Test case IDs must be unique.");
		} catch (ClassNotFoundException e) {
		}

		// Compile code
		super.runTest();
		
		// Generate class files
		generateClassfiles();

		// Execute
		if (testcaseVerbose) System.out.println("Executing test ...");
		try {
			Class<?> c = Class.forName(classname, true, loader);			
			Method m = c.getMethod("runBare", new Class[]{});
			Object o = c.newInstance();
			m.invoke(o);
		} catch (Throwable e) {
			AssertionFailedError err = new AssertionFailedError(classname
					+ " failed to execute.");
			err.initCause(e);
			throw err;
		}
		if (testcaseVerbose) System.out.println("Executing test done.");
	}

}
