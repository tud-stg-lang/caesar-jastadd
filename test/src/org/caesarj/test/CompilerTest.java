package org.caesarj.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.caesarj.compiler.CaesarCompiler;
import org.caesarj.util.VerboseProgress;

public abstract class CompilerTest extends TestCase {
	
	// choose test output level 
	protected boolean compilerVerbose = TestProperties.instance().getCompilerVerbose();
	protected boolean compilerVerboseBinaries = TestProperties.instance().getCompilerVerboseBinaries();
	protected boolean testcaseVerbose = TestProperties.instance().getTestcaseVerbose();
	protected boolean printCode = TestProperties.instance().getPrintCode();
	
	static public class Source {
		public Source(String name, String code) {
			this.name = name;
			this.code = code;
		}

		String name, code;
	}

	protected Vector<Source> sources = new Vector<Source>();
	protected Set binaries = new HashSet();

	protected final String pkgname;
	
	protected String binDir() {
		return TestProperties.instance().getBinDir();
	}
	
	protected String genSrcDir() {
		return TestProperties.instance().getGenSrcDir();
	}

	public CompilerTest(String pkgname) {
		this.pkgname = pkgname;
	}

	public void addSource(String name, String code) {
		sources.add(new Source(name, code));
	}
	
	public void setBinaries(Set binaries) {
		this.binaries = binaries;
	}
	
	protected String absoluteSourceFileName(String sourceName) {
		return genSrcDir() + File.separator 
		       + pkgname.replace('.', File.separatorChar) + File.separator 
		       + sourceName;
	}

	// Creates source files and adds them to compiler.
	protected void addSourceFiles(Collection<Source> sources) throws IOException {
		try {
			final String outdir = genSrcDir() + File.separator + pkgname.replace('.', File.separatorChar);
			clean(new File(outdir));
			
			for (Source code : sources) {
				
				// create source files
				String sourceCode = "package " + pkgname + ";\n" + code.code;
				String sourceFileName = absoluteSourceFileName(code.name);
				File f = new File(sourceFileName);
		        f.getParentFile().mkdirs();
		        FileOutputStream fos = new FileOutputStream(f);
		        fos.write(sourceCode.getBytes());
		        fos.close();
				
				// necessary for resolving package names in the program
	        	CaesarCompiler.addSourceFile(f.getAbsolutePath());
			}
		} 
		catch (Exception e) {
			throw new AssertionFailedError("Compilation failed: " + e);
		} 
	}
	
	protected void typeCheck() {
		if (printCode) System.out.println("Parsed code : \n"+CaesarCompiler.getParsedCode());
		if (testcaseVerbose) System.out.println("Checking for errors ... ");
		CaesarCompiler.typeCheck();
		if (testcaseVerbose) System.out.println("Error check done.");
	}
	
	protected void generateClassfiles() {
		if (testcaseVerbose) System.out.println("Generating class files ...");
		CaesarCompiler.setValueForOption(binDir(), "-d");
		CaesarCompiler.generateBytecode();		
	}
	
	protected void clean(File outdir) {
		if (testcaseVerbose) System.out.println("Looking for old class files ...");
		File[] listFiles = outdir.listFiles();
		if (listFiles == null)
			return;
		for (File f : listFiles) {
			if (f.isDirectory())
				clean(f);
			if (testcaseVerbose) 
				System.out.println("Deleting file : "+outdir+File.separator+f.getName());
			f.delete();
		}
	}
	
	protected void compileBinaries() throws Exception{
		if (binaries.size() <= 0) 
			return;
		
		if (testcaseVerbose) {
			System.out.println("Processing binary code block ...");
		}
				
		CaesarCompiler.initialize();
		
		if (compilerVerboseBinaries) {
			CaesarCompiler.setOption("-verbose");			
		}
		
		/* Extract source files for binaries */
		Vector<Source> binarySources = new Vector<Source>();
		int i = 0;
		for (Object o : binaries) {
			String codeblock = o.toString();
			String blockname = "binary"+i+".java";
			binarySources.add(new Source(blockname, codeblock));
			i++;
		}

		/* Compilation process */
		addSourceFiles(binarySources);
		typeCheck();
		/* Check if compilation succeeded */
		assertTrue(CaesarCompiler.getErrors().toString(), CaesarCompiler.getErrors().isEmpty());
		generateClassfiles();
		
		CaesarCompiler.cleanUp();

		if (testcaseVerbose) {
			System.out.println("Processing binary code block done.");
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		CaesarCompiler.cleanUp();
	}

	protected void runTest() throws Throwable {
		clean(new File(binDir() + pkgname.replace(".", File.separator)));		
		compileBinaries();
	}
}
