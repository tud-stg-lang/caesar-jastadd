package org.caesarj.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.caesarj.ast.CompilationUnit;
import org.caesarj.ast.Program;

import parser.JavaParser;
import parser.JavaScanner;
import parser.JavaParser.SourceError;

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

	protected Program prog;

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

	// Liest den Quelltext ein und baut den AST. Das muss immer funktionieren.
	protected void parseSource() throws IOException {
		try {
			final String outdir = genSrcDir() + File.separator + pkgname.replace('.', File.separatorChar);
			clean(new File(outdir));
			
			/*
			 * TODO: Unicode und StringReader passen irgendwie nicht zusammen.
			 * Scheinbar wird EOF nicht richtig erkannt.
			 */
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
	        	prog.addSourceFile(f.getAbsolutePath());
			}
			
			// force creation of compilation units
			for (Iterator iter = prog.compilationUnitIterator(); iter.hasNext(); ) {
		        CompilationUnit unit = (CompilationUnit)iter.next();
			}
			
		} catch (SourceError e) {
			// TODO: SourceError ist von Error abgeleitet.
			// Warum nicht von Exception?
			throw new AssertionFailedError("Compilation failed: " + e);
		}
	}
	
	protected void errorCheck() {
		if (printCode) System.out.println("Compiled code : \n"+prog.toString());
		if (testcaseVerbose) System.out.println("Checking for errors ... ");
		Collection<String> errors = new LinkedList<String>();
		prog.errorCheck(errors);
		assertTrue(errors.toString(), errors.isEmpty());
		if (testcaseVerbose) System.out.println("Error check done.");
	}
	
	protected void generateClassfiles() {
		if (testcaseVerbose) System.out.println("Generating class files ...");
		prog.setValueForOption(binDir(), "-d");
		prog.java2Transformation();
		prog.generateClassfile();
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
	
	protected void prepareBinaries() throws Exception{
		if (binaries.size() > 0) {
			if (testcaseVerbose) System.out.println("Processing binary code block ...");
			Vector<Source> nonBinarySources = (Vector<Source>) sources.clone(); // clone?
			sources = new Vector<Source>();
			int i = 0;
			for (Object o : binaries) {
				String codeblock = o.toString();
				String blockname = "binary"+i+".java";
				addSource(blockname, codeblock);
				i++;
			}
			prog = new Program();
			if (compilerVerboseBinaries) prog.setOption("-verbose");
			clean(new File(binDir() + pkgname.replace(".", File.separator)));
			parseSource();
			errorCheck();
			generateClassfiles();
			sources = nonBinarySources;
			prog = new Program(); // reset program state (AST tree)
			if (testcaseVerbose) System.out.println("Processing binary code block done.");
		}
	}

	protected void setUp() throws Exception {
		super.setUp();
		prog = new Program();
		if (compilerVerbose) prog.setOption("-verbose");
	}

	protected void tearDown() throws Exception {
		prog = null;
		super.tearDown();
	}

	protected void runTest() throws Throwable {
		prepareBinaries();
//		prog.initCjSourceFiles(); // ?
		if (testcaseVerbose) System.out.println("\nFinished preparing binaries.\n");
		parseSource();
	}
}
