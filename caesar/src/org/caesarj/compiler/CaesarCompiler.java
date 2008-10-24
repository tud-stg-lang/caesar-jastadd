package org.caesarj.compiler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.caesarj.ast.Program;

import parser.JavaParser;

public class CaesarCompiler {
	static protected Program program = null;
	static protected Collection<String> errors = null;
	
	public static void initialize() {
		program = new Program();
		errors = new LinkedList<String>();
		Program.initOptions();
		Program.addKeyValueOption("-classpath");
		Program.addKeyValueOption("-sourcepath");
		Program.addKeyValueOption("-bootclasspath");
		Program.addKeyValueOption("-extdirs");
		Program.addKeyValueOption("-d");
		Program.addKeyOption("-verbose");
		Program.addKeyOption("-logcompiler");
		Program.addKeyOption("-version");
		Program.addKeyOption("-help");
		Program.addKeyOption("-g");
		Program.addKeyOption("-checkonly");
	}
	
	public static void cleanUp() {
		program = null;
		errors = null;
	}
	
	public static void setOption(String name) {
		Program.setOption(name);
	}
	
	public static void setValueForOption(String value, String option) {
		Program.setValueForOption(value, option);
	}
	
	/**
	 * Add a new source file
	 */
	public static void addSourceFile(String filename) {
		program.addSourceFile(filename);
	}
	
	/**
	 * Get compiler errors
	 */
	public static Collection<String> getErrors() {
		return errors;
	}
	
	/**
	 * Pretty printing of parsed AST
	 */
	public static String getParsedCode() {
		try {
			return program.toString();			
		} 
		catch (JavaParser.SourceError e) {
			errors.add(e.getMessage());
			return e.getMessage();
		}	
	}
	
	/**
	 * Collects source files according to the compiler options
	 * (An alternative to manually adding source files)
	 */
	
	public static boolean collectSourceFiles() {
		Collection files = program.files();
		
		if (files.isEmpty()) {
			if (program.hasValueForOption("-sourcepath")) {
				String spath = program.getValueForOption("-sourcepath");
				if (spath.charAt(spath.length()-1) != File.separatorChar)
					spath += File.separator;
				File spathFile = new File(spath);
				if (isValidDirectory(spathFile)) {
					if (program.logCompiler()) {
						System.out.println("Source directory : "+spath);
					}
					try {
						scanDirectory(program, files, spath, spathFile);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else {
				printUsage();
				return false;
			}
		}
		
		program.initCjSourceFiles();

		for (Iterator iter = files.iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			File file = new File(name);
			if ((!file.exists()) && (program.hasValueForOption("-sourcepath"))) {
				String spath = program.getValueForOption("-sourcepath");
				if (spath.charAt(spath.length()-1) != File.separatorChar)
					spath += File.separator;
				File newFile = new File(spath+name);
				if (newFile.isFile())
					name = newFile.getAbsolutePath();
				System.out.println("check 1 file : "+name);
			}
			program.addSourceFile(name);
		}
		
		return true;
	}
	
	public static boolean typeCheck() {
		try {
			program.insertExternalizedVC();
			
			if (Program.verbose()) {
				System.out.println("Parsed source code:");
				System.out.println(program.toString());
			}
			
			program.errorCheck(errors);
			
			if (!errors.isEmpty()) {
				return false;
			}
		} 
		catch (JavaParser.SourceError e) {
			errors.add(e.getMessage());
			return false;
		} 
		return true;
	}
	
	public static void generateBytecode() {
		program.java2Transformation();
		program.generateClassfile();
	}

	// Ignore directories named "test"
	protected static boolean isValidDirectory(File spathFile) {
		return spathFile.isDirectory() && !spathFile.getName().equals("test");
	}

	protected static void scanDirectory (Program program, Collection files, String spath,
			File spathFile) throws IOException {
		String[] filelist = spathFile.list();
		for (String filename: filelist) {
			String fullname = spath+filename;
			File f = new File(fullname);
			if (f.isFile() && fullname.endsWith(".java")) {
				if (program.logCompiler()) {
					System.out.println("                   + "+filename);
				}
				files.add(fullname);
			} else if (isValidDirectory(f)) {
				if (program.logCompiler()) {
					System.out.println("adding directory : "+f.getPath());
				}
				scanDirectory(program, files, f.getPath()+File.separator, f);
			}
		}
	}

	protected static void printUsage() {
		printVersion();
		System.out
				.println("\nCaesarJ Compiler\n\n"
						+ "Usage: java CaesarCompiler <options> <source files>\n"
						+ "  -verbose                  Output messages about what the compiler is doing\n"
						+ "  -classpath <path>         Specify where to find user class files\n"
						+ "  -sourcepath <path>        Specify where to find input source files\n"
						+ "  -bootclasspath <path>     Override location of bootstrap class files\n"
						+ "  -extdirs <dirs>           Override location of installed extensions\n"
						+ "  -d <directory>            Specify where to place generated class files\n"
						+ "  -help                     Print a synopsis of standard options\n"
						+ "  -version                  Print version information\n");
	}

	protected static void printVersion() {
		System.out.println("Java1.4Frontend + Backend + Java5 extensions (http://jastadd.cs.lth.se) Version R20060915");
		System.out.println("CaesarJ: Multiple Inheritance extension Version R20080204");
	}
	
	
	public static void main(String args[]) {
		if (!compile(args))
			System.exit(1);
	}

	public static boolean compile(String args[]) {
		initialize();
		
		try {
			program.addOptions(args);
			
			if (program.hasOption("-version")) {
				printVersion();
				return false;
			}
			if (program.hasOption("-help")) {
				printUsage();
				return false;
			}
			
			if (!collectSourceFiles()) {
				return false;
			}
			
			System.out.println("Checking for errors ...");
			
			if (!typeCheck()) {
				System.out.println("Errors:");
				for (Iterator err = errors.iterator(); err.hasNext();) {
					System.out.println((String) err.next());
				}
				return false;
			}
			
			if (!program.hasOption("-checkonly")) {
				System.out.println("Generating class files...");
				generateBytecode();
				program.java2Transformation();
				program.generateClassfile();
			}
		}
		finally {
			cleanUp();
		}
		return true;
	}	
}
